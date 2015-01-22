package controllers;

import interactors.AuHierarchyRule;
import interactors.LocationRule;
import play.db.jpa.Transactional;
import play.libs.Json;
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
	public static Result findLocations(String q, Integer limit, Integer offset){
		Object result = LocationRule.findByName(q, limit, offset);
		return ok(Json.toJson(result));
	}
	
	@Transactional
	public static Result findLocationNames(String q, Integer limit){
		Object result = AuHierarchyRule.findLocationNames(q, limit);
		return ok(Json.toJson(result));
	}
}
