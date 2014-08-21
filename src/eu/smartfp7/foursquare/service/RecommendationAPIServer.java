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
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import javax.ws.rs.core.UriBuilder;

import org.apache.commons.io.FilenameUtils;
import org.glassfish.grizzly.http.server.HttpServer;
import org.terrier.querying.Manager;
import org.terrier.structures.Index;

import com.sun.jersey.api.container.grizzly2.GrizzlyServerFactory;
import com.sun.jersey.api.core.PackagesResourceConfig;
import com.sun.jersey.api.core.ResourceConfig;

import eu.smartfp7.foursquare.RTimeSeries;
import eu.smartfp7.foursquare.Venue;
import eu.smartfp7.foursquare.VenueUtil;

public class RecommendationAPIServer {
  
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
  
  	// TODO: this is crap. Put everything in a parameter file.
  	public static final Map<String,String> geo_cities = new HashMap<String, String>();
  	public static final Collection<String> cities     = new ArrayList<String>();
  	static {
  	  geo_cities.put("u17","amsterdam");
  	  geo_cities.put("u10","london2");
  	  geo_cities.put("gcp","london2");
  	  geo_cities.put("9q8","sanfrancisco");
  	  geo_cities.put("gcu","glasgow");
  	  
  	  cities.add("amsterdam");
  	  cities.add("london2");
  	  cities.add("sanfrancisco");
  	  cities.add("glasgow");
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
		return UriBuilder.fromUri("http://130.209.247.108/").port(9998).build();
	}
	
	public static final URI BASE_URI = getBaseURI();
	
	protected static HttpServer startServer() throws IOException {
		System.out.println("Starting grizzly...");
		ResourceConfig rc = new PackagesResourceConfig("eu.smartfp7.foursquare");
		return GrizzlyServerFactory.createHttpServer(BASE_URI, rc);
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
        	   VenueForecast forecast = new VenueForecast(RecommendationAPIServer.folder+"/"+city+"_forecasts/live_arima/"+venue.getId()+".forecast");
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
	  
	 
	  timer = new Timer();
      timer.schedule(new RecommendationAPIServer.RemindTask(), getFollowing11am().getTime(), 1000*60*60*24);
      System.out.println("Scheduled job: recomputing background probabilities every day at 11am.");
      
	  
	  for(String city: cities) {
		Index index = Index.createIndex(folder+"/"+city+".index", "data");
		managers.put(city,new Manager(index));
		
		BufferedReader buffer = new BufferedReader(new FileReader(folder+"/"+city+".geohash."+precision));
		String tmp;
		
		Map<String,Collection<Venue>> geo_venues = new HashMap<String, Collection<Venue>>();
		
		
		// This part is all about computing the background statistics to apply
		// Dirichlet smoothing on the forecasts.
		// Since iterating over RTimeSeries is slow we do this only once.
		Double cmu = 0.0;
		Double cbackground_sum = 0.0;
		
		ArrayList<File>        files    = VenueUtil.getAllVenueFilesEndingWith(folder, city, ".ts");
		
		for(File file: files) {
		  RTimeSeries ts = new RTimeSeries(file.getAbsolutePath());

			rtss.put(FilenameUtils.removeExtension(file.getName()),ts.getTotalCheckins());
			cmu += ts.getTotalCheckins()/ts.getDates().size();
			cbackground_sum += ts.getTotalCheckins();

		  
		}
		
		cmu /= files.size();

		VenueForecast sum_forecast = null;
		
		while ((tmp = buffer.readLine()) != null) {
		  String[] line = tmp.split("\t");
		  Collection<Venue> geohash_venues = geo_venues.get(line[0]);
		  if(geohash_venues == null) {
			geohash_venues = new ArrayList<Venue>();
			geo_venues.put(line[0], geohash_venues);
		  }

		  BufferedReader infofile_buffer = new BufferedReader(new FileReader(folder+"/"+city+"_specific_crawl/"+line[1]+".info"));
		  
		  String prefix = "live_arima";
		  
		  VenueForecast forecast = new VenueForecast(RecommendationAPIServer.folder+"/"+city+"_forecasts/"+prefix+"/"+line[1]+".forecast");
		  forecast.firstStepProbs(cmu,cmu*rtss.get(line[1])/cbackground_sum);
		  if(sum_forecast == null)
			sum_forecast = forecast;
		  else
			sum_forecast.add(forecast);
		  
		  geohash_venues.add(new Venue(infofile_buffer.readLine()));
		  infofile_buffer.close();
		}
		buffer.close();
		
		mu.put(city, cmu);
		background_sum.put(city, cbackground_sum);
		background_forecast.put(city, sum_forecast);
		city_geohashes_venues.put(city, geo_venues);
	  }
	  
	  for(String city: cities)
		for(VenueForecast f: venue_forecasts.values())
		  f.secondStepProbs(mu.get(city), background_forecast.get(city));


	  HttpServer httpServer = startServer();
	  System.out.println(String.format("Jersey app started with WADL available at "
		  + "%sapplication.wadl\nTry out %sgeohash.json?lng=-0.112478&lat=51.4969866\nHit enter to stop it...",
		  BASE_URI, BASE_URI));
	  System.in.read();
	  httpServer.stop();
	}    

}
