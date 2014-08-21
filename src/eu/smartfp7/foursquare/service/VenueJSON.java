package eu.smartfp7.foursquare.service;

import java.util.Collection;
import java.util.Map;

public class VenueJSON {
  
  private String title;
  private String url;
  private String foursquare_id;
  private String html;
  
  public String getHtml() {
    return html;
  }

  public void setHtml(String html) {
    this.html = html;
  }

  private Double lat;
  private Double lon;
  private Double facebook_score;
  private Double facebook_prob;
  private VenueForecast forecast;
  private Double rating;
  private int likes;
  private int checkincount;
  private Map<String,String> category_icons;
  private Collection<String> icons;
  

  public VenueJSON(String line) {
	String l[] = line.split("\t");
	
	this.foursquare_id = l[0];
	this.title    	   = l[1];
	this.url           = l[2];
  }
  
  public Double getLat() {
    return lat;
  }

  public void setLat(Double lat) {
    this.lat = lat;
  }

  public Double getLng() {
    return lon;
  }

  public void setLng(Double lng) {
    this.lon = lng;
  }

  public VenueJSON() {  }
  
  public Double getRating() {
	return this.rating;
  }
  
  public int getLikes() {
	return this.likes;
  }
  
  public int getCheckinCount() {
	return this.checkincount;
  }
  
  public Map<String,String> getCategoryIcons(){
	return this.category_icons;
  }
  
  public Collection<String> getIcons(){
	return this.icons;
  }
  
  public String getTitle() {
    return title;
  }
  public void setTitle(String name) {
    this.title = name;
  }
  public String getUrl() {
    return url;
  }
  public void setUrl(String url) {
    this.url = url;
  }
  public String getFoursquareId() {
    return foursquare_id;
  }
  public void setFoursquareId(String id) {
    this.foursquare_id = id;
  }
  public VenueForecast getForecast() {
    return forecast;
  }
  public void setForecast(VenueForecast forecast) {
    this.forecast = forecast;
  }
  
  public Double getFacebook_score() {
    return facebook_score;
  }

  public void setFacebook_score(Double facebook_score) {
    this.facebook_score = facebook_score;
  }

  public Double getFacebookProb() {
	return facebook_prob;
  }

  public void setFacebookProb(Double facebook_prob) {
	this.facebook_prob = facebook_prob;
  }

  public void setRating(Double rating) {
	this.rating = rating;
  }

  public void setLike(int likes) {
	this.likes = likes;
  }

  public void setCheckincount(int checkincount) {
	this.checkincount = checkincount;
  }

  public void setCategoryIcons(Map<String,String> categoryIcons) {
	this.category_icons = categoryIcons;
  }

  public void setIcons(Collection<String> icons) {
	this.icons = icons;
  }
}
