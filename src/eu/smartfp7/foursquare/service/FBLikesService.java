package eu.smartfp7.foursquare.service;

import java.util.Collection;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.UriBuilder;

import org.glassfish.grizzly.http.server.HttpServer;

import com.sun.jersey.api.container.grizzly2.GrizzlyServerFactory;
import com.sun.jersey.api.core.PackagesResourceConfig;

import eu.smartfp7.facebook.FacebookPage;
import eu.smartfp7.facebook.FacebookUtil;


@Path("/fblikes.json")
public class FBLikesService {

	@GET
	@Produces("application/json")
	public String facebookLikes(@QueryParam("user_id") String user_id, @QueryParam("access_token") String access_token) throws Exception
	{
		Collection<FacebookPage> user_likes = FacebookUtil.getFacebookLikesByUserId(user_id, access_token);
		StringBuilder s = new StringBuilder();
		s.append("[");
		for (FacebookPage p : user_likes)
		{			
			s.append(p.toJsonString());
			s.append(",");
		}
		s.setLength(s.length() -1);
		s.append("]");
		return s.toString();
	}
	
	public static void main(String[] args) throws Exception 
	{
		PackagesResourceConfig rc = new PackagesResourceConfig(VenueRecommendationService.class.getPackage().getName());
		rc.getClasses().add(FBLikesService.class);
		HttpServer httpServer = GrizzlyServerFactory.createHttpServer(UriBuilder.fromUri("http://0.0.0.0/").port(9045).build(),  rc);
		  System.in.read();
		  httpServer.stop();
	}

}
