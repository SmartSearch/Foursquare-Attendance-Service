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
	
	for(JsonElement e: jsonObj.get("likes").getAsJsonObject().get("data").getAsJsonArray())
	  return_coll.add(new FacebookPage(e.toString()));

	return return_coll;
  }
  
  /**
   * Given a user ID and an access token allowing listing his/her likes, returns
   * a list of FacebookPage objects (which only contain IDs, categories and names).
   */
  public static Collection<FacebookPage> getFacebookLikesByUserId(String user_id, String access_token) throws IOException {
	JsonObject jsonObj= new JsonParser().parse(Utils.makeAPICall("https://graph.facebook.com/"+user_id+"?fields=likes&limit=100&access_token="+access_token)).getAsJsonObject();
	
	return getFacebookPagesFromJSON(jsonObj);
  }
}
