package controllers;

import interactors.GeoJsonRule;
import interactors.LocationProxyRule;
import play.db.jpa.Transactional;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;

public class LocationServices extends Controller {
	private static final String FORMAT_GEOJSON = "geojson";
	private static final String FORMAT_APOLLOJSON = "json";
	private static final String FORMAT_APOLLOXML = "xml";
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
			return ApolloLocationServices.locationsInJson(gid +"");
		case FORMAT_APOLLOXML:
			return ApolloLocationServices.locationsInXml(gid +"");
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
		Object result = GeoJsonRule.findByName(q, limit, offset);
		return ok(Json.toJson(result));
	}
	
	@Transactional
	public static Result findLocationNames(String q, Integer limit){
		Object result = LocationProxyRule.findLocationNames(q, limit);
		return ok(Json.toJson(result));
	}
	
	@Transactional
	public static Result findLocationsByPoint(double lat, double lon){
		Object result = GeoJsonRule.findByNameByPoint(lat, lon);
		return ok(Json.toJson(result));
	}
}
