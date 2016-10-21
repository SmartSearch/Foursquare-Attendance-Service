package eu.smartfp7.foursquare.service;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import org.apache.commons.lang.StringUtils;
import org.terrier.matching.ResultSet;
import org.terrier.querying.Manager;
import org.terrier.querying.SearchRequest;
import org.terrier.structures.MetaIndex;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import eu.smartfp7.facebook.FacebookPage;
import eu.smartfp7.facebook.FacebookUtil;
import eu.smartfp7.foursquare.Venue;
import eu.smartfp7.geo.GeoHash;
import eu.smartfp7.geo.GeoUtil;

@Path("/geohash.json")
public class VenueRecommendationService {

	private static final String FACEBOOK_SAMPLES_DIR = "/local/tr.smart/foursquare/facebook_samples/";

	int queryCounter = 0;
	
	@GET
	@Produces("application/json")
	public String retrieveVenuesByLngLat(@QueryParam("lng") Double longitude, @QueryParam("lat") Double latitude,
			@QueryParam("user_id") String user_id, @QueryParam("access_token") String access_token,
			@QueryParam("callback") String callback, @QueryParam("limit") Integer limit)
			throws IOException, ParseException {
		// First, get the geohash corresponding to the user location
		if (longitude == null && latitude == null)
			return callback == null ? "{}" : callback + "( {} );";

		String venue_json = retrieveVenues(GeoUtil.geoHash(longitude, latitude, RecommendationAPIServer.precision),
				user_id, access_token, limit);
		return callback == null ? venue_json : callback + "( " + venue_json + " );";
	}

	@GET
	@Path("/attendance")
	@Produces("application/json")
	public String getVideosPool(@QueryParam("foursquare_id") String foursquare_id,
			@QueryParam("callback") String callback) {

		String json = new Gson().toJson(RecommendationAPIServer.venue_forecasts.get(foursquare_id));
		return callback == null ? json : callback + "( " + json + " );";
	}

	protected String retrieveVenues(String geohash, String user_id, String access_token, Integer limit)
			throws IOException, ParseException {
		// Then, get all the venue IDs that are located within the same geohash
		// area
		// (stored in the geohashes_files folder)
		Collection<VenueJSON> return_json = new ArrayList<VenueJSON>();

		String sub_geohash = geohash.substring(0, 3);

		String city = RecommendationAPIServer.geo_cities.get(sub_geohash);
		if (city == null || RecommendationAPIServer.city_geohashes_venues.get(city) == null) {
			return new Gson().toJson("No venues for this city.");
		}
		assert RecommendationAPIServer.city_geohashes_venues.get(city) != null;

		Collection<Venue> venues = RecommendationAPIServer.city_geohashes_venues.get(city).get(geohash);
		if (venues == null && !RecommendationAPIServer.city_geohashes_venues.get(city).containsKey(geohash))
			venues = new ArrayList<Venue>();

		for (GeoHash neighbour_geohash : GeoHash.fromGeohashString(geohash).getAdjacent()) {
			String sub = neighbour_geohash.toBase32().substring(0, 3);

			if (RecommendationAPIServer.city_geohashes_venues.get(RecommendationAPIServer.geo_cities.get(sub))
					.containsKey(neighbour_geohash.toBase32()))
				venues.addAll(RecommendationAPIServer.city_geohashes_venues
						.get(RecommendationAPIServer.geo_cities.get(sub)).get(neighbour_geohash.toBase32()));
		}

		HashSet<Venue> hs = new HashSet<Venue>();
		hs.addAll(venues);
		venues.clear();
		venues.addAll(hs);

		Collection<FacebookPage> user_likes = null;
		Map<String, Double> venue_scores = new HashMap<String, Double>();

		if (user_id == null || access_token == null) {
			System.err.println("WARN: No FB credentials passed");
			user_likes = new ArrayList<FacebookPage>();
		} else {
			try {
				user_likes = FacebookUtil.getFacebookLikesByUserId(user_id, access_token);
			} catch (Exception e) {
				System.err.println(
						"ERROR: Bad call/request to the Facebook API. Check the syntax of the URL and the validity of the access token.");
				e.printStackTrace();
				return new Gson().toJson(
						"Bad call/request to the Facebook API. Check the syntax of the URL and the validity of the access token.");
			}
		}

		if (venues.isEmpty() || venues == null) {
			return new Gson().toJson("No venues near this location.");
		}
		if (user_likes.size() > 0) {

			for (FacebookPage like : user_likes) {
				Manager manager = RecommendationAPIServer.managers.get(city);

				SearchRequest srq = manager.newSearchRequest(String.valueOf(queryCounter++), like.getCategory());

				srq.addMatchingModel("Matching", "DPH");
				srq.setControl("start", "0");
				srq.setControl("end", "19");

				manager.runPreProcessing(srq);
				manager.runMatching(srq);
				manager.runPostProcessing(srq);
				manager.runPostFilters(srq);

				ResultSet set = srq.getResultSet();
				int[] docids = set.getDocids();
				double[] scores = set.getScores();

				MetaIndex metaIndex = manager.getIndex().getMetaIndex();
				for (int i = 0; i < docids.length; ++i) {
					String venue_id = metaIndex.getItem("docno", docids[i]);
					venue_scores.put(venue_id, venue_scores.get(venue_id) + scores[i]);
				}
			}
			
			
		} else {
			System.err.println("WARN: No FB likes found u=" + user_id + " a=" + access_token
					+ ", resorting to ranking all venues equally");
			for (Venue v : venues) {
				venue_scores.put(v.getId(), 1d);
			}
		}

		for (Venue venue : venues) {
			Double facebook_score = venue_scores.get(venue.getId());

			if (facebook_score == null || facebook_score == 0.0)
				continue;

			facebook_score /= 110;

			VenueForecast forecast = RecommendationAPIServer.venue_forecasts.get(venue.getId());

			VenueJSON json = new VenueJSON();
			json.setFoursquareId(venue.getId());
			json.setTitle(venue.getName());
			json.setUrl(venue.getUrl());
			json.setForecast(forecast);
			json.setFacebook_score(facebook_score == null ? 0.0 : facebook_score);
			json.setLat(venue.getLat());
			json.setLng(venue.getLon());
			json.setRating(venue.getRating());
			json.setLike(venue.getLikes());
			json.setCheckincount(venue.getCheckincount());
			json.setCategoryIcons(venue.getCategoryIcons());
			json.setIcons(venue.getIcons());
			json.setHtml(
					"<div class='over_image'><strong>" + venue.getName() + "</strong>"
							+ "<p><a href='" + venue
									.getUrl()
							+ "' target='_blank'>" + venue.getUrl() + "</a></p>" + "<p>Categories: "
							+ StringUtils.join(venue.getCategories().values(), ", ") + ".</p></div>"
							+ (venue.getPhotos().size() > 0 ? "<img class='venue_photo' src='"
									+ ((ArrayList<String>) venue.getPhotos())
											.get(new Random().nextInt(venue.getPhotos().size()))
									+ "' alt='" + venue.getName() + "'/>" : ""));

			return_json.add(json);
		}

		System.out.println(return_json.size());

		Gson gson = new GsonBuilder().serializeSpecialFloatingPointValues().create();

		// return a list of URLs, name, address, whatever... to be defined.
		return gson.toJson(return_json);
	}

	protected static Collection<FacebookPage> getLikesFromSample(String user_id) throws IOException {
		BufferedReader infofile_buffer = new BufferedReader(new FileReader(FACEBOOK_SAMPLES_DIR + user_id));
		JsonObject obj = new JsonParser().parse(infofile_buffer.readLine()).getAsJsonObject();
		infofile_buffer.close();

		return FacebookUtil.getFacebookPagesFromJSON(obj);
	}
}
