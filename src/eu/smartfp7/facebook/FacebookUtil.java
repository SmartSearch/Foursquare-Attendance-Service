package eu.smartfp7.facebook;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import eu.smartfp7.foursquare.utils.Utils;

/**
 * [Copyright and License]
 */

/**
 * A class containing utility functions related to the Facebook Graph API.
 * 
 * @author Romain Deveaud <romain.deveaud at glasgow.ac.uk>
 *
 */
public class FacebookUtil {
  
  /**
   * Given the ID of a page, returns the JSON containing the complete information.
   */
  public static String getFacebookPageById(String id) throws IOException {
	return Utils.makeAPICall("http://graph.facebook.com/"+id);
  }
  
  /**
   * Given a JsonObject coming from a Facebook API call, returns a Collection of
   * FacebookPage objects.
   */
  public static Collection<FacebookPage> getFacebookPagesFromJSON(JsonObject jsonObj) {
	Collection<FacebookPage> return_coll = new ArrayList<FacebookPage>();
	
	for(JsonElement e: jsonObj.get("data").getAsJsonArray())
	  return_coll.add(new FacebookPage(e.toString()));

	return return_coll;
  }
  
  /**
   * Given a user ID and an access token allowing listing his/her likes, returns
   * a list of FacebookPage objects (which only contain IDs, categories and names).
   */
  public static Collection<FacebookPage> getFacebookLikesByUserId(String user_id, String access_token) throws IOException {
	final String apiUrl = "https://graph.facebook.com/v2.8/"+user_id+"/likes?"
		+"fields=category,category_list,website,about,were_here_count,name,cover,talking_about_count,username,company_overview,likes,location,new_like_count"
		+"&limit=100&access_token="+access_token;
	System.err.println("Accessing fb graph api at " + apiUrl);
	JsonObject jsonObj= new JsonParser().parse(Utils.makeAPICall(apiUrl)).getAsJsonObject();
	try{
		return getFacebookPagesFromJSON(jsonObj);
	} catch (Exception e) {
		System.err.println(e + " Could not find desired items likes->data->[array] from JSON:");
		e.printStackTrace();
		System.err.println(jsonObj.toString());
		return new ArrayList<>();
	}
  }
}
