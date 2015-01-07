package controllers;

import play.db.jpa.Transactional;
import play.mvc.Controller;
import play.mvc.Result;


public class Application extends Controller {
	
	@Transactional
	public static Result index() {
		return ok(views.html.index.render("Apollo Location Services"));
	}
	
	@Transactional
	public static Result browser() {
		return ok(views.html.browser.render("Apollo Location Services"));
	}
	
	@Transactional
	public static Result getDataSource(String gid, String format) {
		Result result = notFound("<h1>Could not find ID:" + gid + " in " + format + " format</h1>").as("text/html");
		
		switch(format) {
			case "ApolloJSON":
			case "apollojson":
				result = controllers.EpidemicZoneServices.locations(gid);
			break;
			
			case "geojson":
			case "GeoJSON":
				result = controllers.AdministrativeUnitServices.read(gid);
			break;
			
			case "kml":
			case "KML":
			break;
			
			default:
			break;
		}
		
		return result;
	}
}
