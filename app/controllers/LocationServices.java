package controllers;

import interactors.GeoJsonRule;
import interactors.LocationProxyRule;
import interactors.LocationRule;
import models.geo.FeatureCollection;
import play.Logger;
import play.db.jpa.Transactional;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import dao.entities.Location;

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
	
	@Transactional
	public static Result create() {
		try {
			FeatureCollection parsed = AdministrativeUnitServices.parseRequestAsFeatureCollection();
			Long id = Wire.create(parsed);
			AdministrativeUnitServices.setResponseLocation(id);
			return created();
		} catch (RuntimeException e){
			String message = e.getMessage();
			Logger.error(message, e);
			return badRequest(message);
		} catch (Exception e) {
			String message = e.getMessage();
			Logger.error(message, e);
			return forbidden(message);
		}
	}
	
	public static class Wire{
		public static Long create(FeatureCollection fc) {
			Location location = GeoJsonRule.asLocation(fc);
			Long id = LocationRule.create(location);
			return id;
		}

		public static FeatureCollection read(long gid) {
			Location location = LocationRule.read(gid);
			FeatureCollection fc = GeoJsonRule.asFeatureCollection(location);
			return fc;
		}
		

	}

}
