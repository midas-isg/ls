package controllers;

import interactors.LocationRule;
import play.Logger;
import play.db.jpa.Transactional;
import play.mvc.Controller;
import play.mvc.Result;

public class LocationServices extends Controller {
	private static final String FORMAT_GEOJSON = "geojson";
	private static final String FORMAT_APOLLOJSON = "apollojson";
	private static final String FORMAT_KML = "kml";
	private static final String FORMAT_DEFAULT = "geojson";
	
	@Transactional
	public static Result locations(Long gid, String format){
		if (gid == null)
			return notFound("gid is required but got " + gid);
		
		if (IsFalsified(format))
			format = FORMAT_DEFAULT;
		
		switch (format.toLowerCase()){
		case FORMAT_GEOJSON:
			return AdministrativeUnitServices.read(gid +"");
		case FORMAT_APOLLOJSON:
			return EpidemicZoneServices.locations(gid +"");
		case FORMAT_KML:
			return AdministrativeUnitServices.asKml(gid); 
		default:
			return badRequest(format + " is not supported.");
		}
	}
	
	private static boolean IsFalsified(String text) {
		return text == null || text.isEmpty();
	}
	
	@Transactional
	public static Result findLocations(String input){
		Object result = LocationRule.findByName(input);
		Logger.debug(result.toString());
		//return ok(Json.toJson(result));
		return ok();
	}
}
