package eu.smartfp7.foursquare.service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import javax.ws.rs.core.UriBuilder;

import org.apache.commons.io.FilenameUtils;
import org.glassfish.grizzly.http.server.HttpServer;
import org.terrier.querying.Manager;
import org.terrier.structures.Index;
import org.terrier.utility.TerrierTimer;

import com.sun.jersey.api.container.grizzly2.GrizzlyServerFactory;
import com.sun.jersey.api.core.PackagesResourceConfig;

import eu.smartfp7.foursquare.RTimeSeries;
import eu.smartfp7.foursquare.Venue;
import eu.smartfp7.foursquare.VenueUtil;

public class RecommendationAPIServer {
  
	// TODO: this is bad. We should put everything in a parameter file.
	
	static final String FORECAST_TYPE = "live_arima";
	static final String HTTP_BIND_HOST = "0.0.0.0";
	static final int HTTP_BIND_PORT = 9998;
	
  	public static final Map<String,String> geo_cities = new HashMap<String, String>();
  	public static final Collection<String> cities     = new ArrayList<String>();
	static {
	  	  //geo_cities.put("u17","amsterdam");
	  	  //geo_cities.put("u10","london2");
	  	  //geo_cities.put("gcp","london2");
	  	  //geo_cities.put("9q8","sanfrancisco");
	  	  geo_cities.put("gcu","glasgow");
	  	  
	  	  //cities.add("amsterdam");
	  	  //cities.add("london2");
	  	  //cities.add("sanfrancisco");
	  	  cities.add("glasgow");
	  	}
	
	public static Calendar getFollowing11am() {
		Calendar date = new GregorianCalendar();
		// reset hour, minutes, seconds and millis
	
		if(date.get(Calendar.HOUR_OF_DAY) >= 11)
		  date.add(Calendar.DAY_OF_MONTH, 1);
	
		date.set(Calendar.HOUR_OF_DAY, 11);
		date.set(Calendar.MINUTE, 0);
		date.set(Calendar.SECOND, 0);
		date.set(Calendar.MILLISECOND, 0);
	
		return date;
	}
  
  	
  	
  	public static Map<String,Manager> managers  = new HashMap<String, Manager>();
  	public static Map<String, Double> rtss = new HashMap<String,Double>();
  	public static Map<String, Double> mu   = new HashMap<String, Double>();
  	public static Map<String, Double> background_sum = new HashMap<String, Double>();
  	public static Map<String,VenueForecast> venue_forecasts = new HashMap<String, VenueForecast>();
  	public static Map<String,VenueForecast> background_forecast = new HashMap<String, VenueForecast>();
  	
  	public static Map<String,Map<String,Collection<Venue>>> city_geohashes_venues = new HashMap<String,Map<String, Collection<Venue>>>();
  
  	public static int    precision;
  	public static String folder;
  	
  	private static Timer timer;

	private static URI getBaseURI() {
		return UriBuilder.fromUri("http://"+HTTP_BIND_HOST+"/").port(HTTP_BIND_PORT).build();
	}
	
	public static final URI BASE_URI = getBaseURI();
	
	protected static HttpServer startServer() throws IOException {
		System.out.println("Starting grizzly...");
		
		PackagesResourceConfig rc = new PackagesResourceConfig(VenueRecommendationService.class.getPackage().getName());
		rc.getClasses().add(VenueRecommendationService.class);
		rc.getClasses().add(FBLikesService.class);
		assert rc.getClasses().size() > 0;
		return GrizzlyServerFactory.createHttpServer(BASE_URI,  rc);
	}

	public static class RemindTask extends TimerTask {
      public void run() {
    	long currentTime = System.currentTimeMillis();
    	DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
         System.out.format(df.format(currentTime)+": computing background probabilities...%n");
         
         for(String city: cities)
		  try {
			RecommendationAPIServer.background_forecast.put(city,VenueForecast.computeBackgroundForecast(RecommendationAPIServer.folder,city));
		  } catch (IOException e) {
			System.err.println("Error when computing background probability for "+city+" : "+e.getMessage());
		  }
         
         for(String city: cities) {
           Collection<Venue> venues = null;
           
           try {
        	 venues = VenueUtil.listAllVenues(folder, city);
           } catch (IOException e) {
        	 System.err.println("Error when listing venues for "+city+" : "+e.getMessage());
           }
           
           for(Venue venue: venues) {
        	 //VenueForecast forecast = new VenueForecast(RecommendationAPIServer.folder+"/"+RecommendationAPIServer.geo_cities.get(sub_geohash)+"_forecasts/live/"+venue.getId()+".forecast");
        	 try {
        	   VenueForecast forecast = new VenueForecast(RecommendationAPIServer.folder+"/"+city+"_forecasts/"+FORECAST_TYPE+"/"+venue.getId()+".forecast");
        	   forecast.firstStepProbs(RecommendationAPIServer.mu.get(city),RecommendationAPIServer.mu.get(city)*RecommendationAPIServer.rtss.get(venue.getId())/RecommendationAPIServer.background_sum.get(city));
        	   forecast.secondStepProbs(RecommendationAPIServer.mu.get(city), RecommendationAPIServer.background_forecast.get(city));

        	   RecommendationAPIServer.venue_forecasts.put(venue.getId(),forecast);
        	 } catch (IOException e) {
        	   System.err.println("Error when reading forecast for venue ("+venue.getId()+") : "+e.getMessage());
        	 }

           }
         }
      }
	}
	
	public static void main(String[] args) throws IOException, ParseException {  
	  
	  folder    = args[0];
	  precision = Integer.parseInt(args[1]);
	  
	  System.setProperty("terrier.home", folder+"/terrier.web_service");
	  
	 
	  Thread backgroundStartup = new Thread() {
		  public void run() {
			  
			  for(String city: cities)
			  {
				  try {
				 	loadCity(city);
				 	
				  } catch (Exception e) {
					  System.err.println("Problem processing city " + city + " : " + e.toString());
					  e.printStackTrace();
				  }
			  }
			  
			  for(String city: cities)
				for(VenueForecast f: venue_forecasts.values())
				  f.secondStepProbs(mu.get(city), background_forecast.get(city));
			  
			  System.out.println("Background startup thread complete");
			  
			  timer = new Timer();
		      timer.schedule(new RecommendationAPIServer.RemindTask(), getFollowing11am().getTime(), 1000*60*60*24);
		      System.out.println("Scheduled job: recomputing background probabilities every day at 11am.");
		      
		      
		  }
		  
	  };
      
	  backgroundStartup.start();
	  

	  HttpServer httpServer = startServer();
	  System.out.println(String.format("Jersey app started with WADL available at "
		  + "%sapplication.wadl\nTry out %sgeohash.json?lng=-0.112478&lat=51.4969866\nHit enter to stop it...",
		  BASE_URI, BASE_URI));
	  System.in.read();
	  httpServer.stop();
	  timer.cancel();
	}

	protected static void loadCity(String cityName)
			throws IOException, ParseException {
		Index index = Index.createIndex(folder+"/"+cityName+".index", "data");
		managers.put(cityName,new Manager(index));
		
		
		
		
		Map<String,Collection<Venue>> geo_venues = new HashMap<String, Collection<Venue>>();
		
		
		// This part is all about computing the background statistics to apply
		// Dirichlet smoothing on the forecasts.
		// Since iterating over RTimeSeries is slow we do this only once.
		Double cmu = 0.0;
		Double cbackground_sum = 0.0;
		
		List<File> files = VenueUtil.getAllVenueFilesEndingWith(folder, cityName, ".ts");
		TerrierTimer progress = new TerrierTimer("Loading venues from " + cityName, files.size());
		progress.start();
		try{
			for(File file: files) {
				try{
					final String venueId = FilenameUtils.removeExtension(file.getName());
					RTimeSeries ts = new RTimeSeries(file.getAbsolutePath());
					if (ts.getDates().size() == 0)
					{
						System.err.println("WARN: Skipping venue with no current dates in " + file);
						continue;
					}
					rtss.put(venueId,ts.getTotalCheckins());
					cmu += ts.getTotalCheckins()/ts.getDates().size();
					cbackground_sum += ts.getTotalCheckins();
				} catch (Exception e) {
					System.err.println("WARN: problem with venue recorded in " + file + " : " + e.toString());
					e.printStackTrace();
				}
				progress.increment();
			}
		} finally {
			progress.finished();
		}
		
		
		cmu /= files.size();

		VenueForecast sum_forecast = null;
		String tmp;
		
		final String fileName_geohash2venue = folder+"/"+cityName+".geohash."+precision;
		BufferedReader geohash2venue = new BufferedReader(new FileReader(fileName_geohash2venue));
		progress = new TerrierTimer("Loading venues from " + geohash2venue, files.size());
		progress.start();
		try{
			
			while ((tmp = geohash2venue.readLine()) != null) {
			  String[] line = tmp.split("\t");
			  final String geoHash = line[0];
			  final String venueId = line[1];
			  
			  Collection<Venue> geohash_venues = geo_venues.get(geoHash);
			  if(geohash_venues == null) {
				geohash_venues = new ArrayList<Venue>();
				geo_venues.put(geoHash, geohash_venues);
			  }
	
			  
			  //check that we have a timeseries for this venue
			  if (! rtss.containsKey(venueId))
			  {
				  System.err.println("WARN: ignoring venue " + venueId + " in " + fileName_geohash2venue + ": no rtss value for that venue");
				  continue;
			  }
			  assert rtss.get(venueId) != null;
			  
			  //open the specific information about the venue
			  File venueInfoFile = new File(folder+"/"+cityName+"_specific_crawl/"+venueId+".info");
			  if (! venueInfoFile.exists())
			  {
				  System.err.println("WARN: ignoring venue " + venueId + " in " + fileName_geohash2venue + ": no venue info file for that venue");
				  continue;
			  }
			  BufferedReader infofile_buffer = new BufferedReader(new FileReader(venueInfoFile));
			  
			  String filename_forecast = RecommendationAPIServer.folder+"/"+cityName+"_forecasts/"+FORECAST_TYPE+"/"+venueId+".forecast";
			  if (! new File(filename_forecast).exists())
			  {
				  System.err.println("WARN: ignoring venue " + venueId + " in " + fileName_geohash2venue + ": no venue forecase file for that venue");
				  continue;
			  }
			  	  
			  VenueForecast forecast = new VenueForecast(filename_forecast);
			  forecast.firstStepProbs(cmu,cmu*rtss.get(venueId)/cbackground_sum);
			  if(sum_forecast == null)
				sum_forecast = forecast;
			  else
				sum_forecast.add(forecast);
			  
			  geohash_venues.add(new Venue(infofile_buffer.readLine()));
			  infofile_buffer.close();
			}
			geohash2venue.close();		
		} finally {
			progress.finished();
		}
		
		mu.put(cityName, cmu);
		background_sum.put(cityName, cbackground_sum);
		background_forecast.put(cityName, sum_forecast);
		city_geohashes_venues.put(cityName, geo_venues);
		System.err.println("Finished loading " + cityName);
	}    

}
