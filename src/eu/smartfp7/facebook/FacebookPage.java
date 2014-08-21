package eu.smartfp7.facebook;

import java.util.ArrayList;
import java.util.Collection;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Class representing a Facebook page which contains information provided
 * by the Graph API.
 * see https://developers.facebook.com/docs/getting-started/graphapi/
 * 
 * @author Romain Deveaud <romain.deveaud at glasgow.ac.uk>
 *
 */
public class FacebookPage {
  
  /**
   * An example of a page in the Facebook JSON format:
   * 
   * {
       "about": "Fond\u00e9e en 2004, La Volte est une maison d'\u00e9dition ind\u00e9pendante qui publie chaque ann\u00e9e trois \u00e0 cinq romans ou recueils de nouvelles d'auteurs fran\u00e7ais ou \u00e9trangers.",
       "category": "Publisher",
       "is_published": true,
       "talking_about_count": 51,
       "username": "editions.lavolte",
       "website": "http://www.lavolte.net/",
       "were_here_count": 0,
       "id": "281222991993110",
       "name": "\u00c9ditions La Volte",
       "link": "http://www.facebook.com/editions.lavolte",
       "likes": 1245,
       "cover": {
          "cover_id": 281227681992641,
          "source": "http://scontent-a.xx.fbcdn.net/hphotos-ash2/s720x720/264071_281227681992641_1110589475_n.jpg",
          "offset_y": 0,
          "offset_x": 0
       }
     }
   */
  
  private String id;
  private String name;
  private String link;
  private String about;
  private String category;
  private String username;
  private String website;
  private String company_overview;
  private String description;
  
  private Collection<String> category_list;
  
  private boolean is_published;
  
  private int talking_about_count;
  private int were_here_count;
  private int likes;
  
  
  public FacebookPage(String jsonString) {
	JsonObject jsonObj= new JsonParser().parse(jsonString).getAsJsonObject();
	
	if(jsonObj.has("id"))
	  setId(jsonObj.get("id").getAsString());
	if(jsonObj.has("name"))
	  setName(jsonObj.get("name").getAsString());
	if(jsonObj.has("link"))
	  setLink(jsonObj.get("link").getAsString());
	if(jsonObj.has("about"))
	  setAbout(jsonObj.get("about").getAsString());
	if(jsonObj.has("category"))
	  setCategory(jsonObj.get("category").getAsString());
	if(jsonObj.has("username"))
	  setUsername(jsonObj.get("username").getAsString());
	if(jsonObj.has("website"))
	  setWebsite(jsonObj.get("website").getAsString());
	if(jsonObj.has("company_overview"))
	  setCompany_overview(jsonObj.get("company_overview").getAsString());
	if(jsonObj.has("description"))
	  setDescription(jsonObj.get("description").getAsString());

	if(jsonObj.has("is_published"))
	  setIs_published(jsonObj.get("is_published").getAsBoolean());

	if(jsonObj.has("talking_about_count"))
	  setTalking_about_count(jsonObj.get("talking_about_count").getAsInt());
	if(jsonObj.has("were_here_count"))
	  setWere_here_count(jsonObj.get("were_here_count").getAsInt());
	if(jsonObj.has("likes"))
	  setLikes(jsonObj.get("likes").getAsInt());
	
	if(jsonObj.has("category_list")) {
	  Collection<String> tmp_list = new ArrayList<String>();
	  for(JsonElement e: jsonObj.get("category_list").getAsJsonArray())
		tmp_list.add(e.getAsJsonObject().get("name").getAsString());
	  
	  setCategory_list(tmp_list);
	}
  }


  public String getId() {
    return id;
  }


  public void setId(String id) {
    this.id = id;
  }


  public String getName() {
    return name;
  }


  public void setName(String name) {
    this.name = name;
  }


  public String getLink() {
    return link;
  }


  public void setLink(String link) {
    this.link = link;
  }


  public String getAbout() {
    return about;
  }


  public void setAbout(String about) {
    this.about = about;
  }


  public String getCategory() {
    return category;
  }


  public void setCategory(String category) {
    this.category = category;
  }


  public String getUsername() {
    return username;
  }


  public void setUsername(String username) {
    this.username = username;
  }


  public String getWebsite() {
    return website;
  }


  public void setWebsite(String website) {
    this.website = website;
  }


  public String getCompany_overview() {
    return company_overview;
  }


  public void setCompany_overview(String company_overview) {
    this.company_overview = company_overview;
  }


  public String getDescription() {
    return description;
  }


  public void setDescription(String description) {
    this.description = description;
  }


  public Collection<String> getCategory_list() {
    return category_list;
  }


  public void setCategory_list(Collection<String> category_list) {
    this.category_list = category_list;
  }


  public boolean isIs_published() {
    return is_published;
  }


  public void setIs_published(boolean is_published) {
    this.is_published = is_published;
  }


  public int getTalking_about_count() {
    return talking_about_count;
  }


  public void setTalking_about_count(int talking_about_count) {
    this.talking_about_count = talking_about_count;
  }


  public int getWere_here_count() {
    return were_here_count;
  }


  public void setWere_here_count(int were_here_count) {
    this.were_here_count = were_here_count;
  }


  public int getLikes() {
    return likes;
  }


  public void setLikes(int likes) {
    this.likes = likes;
  }

}
