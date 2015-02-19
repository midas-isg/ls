package controllers;

import interactors.GeoJSONParser;
import interactors.GeoJsonRule;
import interactors.KmlRule;
import interactors.LocationRule;
import models.geo.FeatureCollection;
import play.Logger;
import play.db.jpa.Transactional;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Http.Context;
import play.mvc.Http.Request;
import play.mvc.Http.RequestBody;
import play.mvc.Result;

import com.fasterxml.jackson.databind.JsonNode;

import dao.entities.Location;

public class AdministrativeUnitServices extends Controller {
	static Status okJson(Object resultObject) {
		return ok(Json.toJson(resultObject));
	}
	
	@Transactional
	public static Result create() {
		try {
			FeatureCollection parsed = parseRequestAsFeatureCollection();
			Long id = Wire.create(parsed);
			setResponseLocation(id);
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
			Location location = GeoJsonRule.asDeprecatedLocation(fc);
			Long id = LocationRule.create(location);
			return id;
		}

		public static FeatureCollection read(long gid) {
			Location location = LocationRule.read(gid);
			FeatureCollection fc = GeoJsonRule.asFeatureCollection(location);
			return fc;
		}
		

	}


	static void setResponseLocation(Long id) {
		String uri = makeUri(id);
		response().setHeader(LOCATION, uri);
	}

	static String makeUri(Long id) {
		Request request = Context.current().request();
		Logger.debug(""+ request.headers());
		String url = request.getHeader(ORIGIN) + request.path();
		String result = url;
		if (id != null)
			result += "/" + id;
		return result;
	}
	
	@Transactional
	public static Result read(String gidText) {
		response().setContentType("application/vnd.geo+json");
		long gid = Long.parseLong(gidText);
		return okJson(Wire.read(gid));
	}

	@Transactional
	public static Result update(long gid) {
		try {
			FeatureCollection parsed = parseRequestAsFeatureCollection();
			Location location = GeoJsonRule.asDeprecatedLocation(parsed);
			LocationRule.update(gid, location);
			setResponseLocation(null);
			return noContent();
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

	static FeatureCollection parseRequestAsFeatureCollection() throws Exception {
		Request request = Context.current().request();
		JsonNode requestJSON = null;
			
Logger.debug("\n");
Logger.debug("=====");
			
		if(request != null) {
			RequestBody requestBody = request.body();
			
			String requestBodyText = requestBody.toString();
Logger.debug("Request [" + request.getHeader("Content-Type") + "], Length: " + requestBodyText.length());
Logger.debug("Request Body:\n" + requestBodyText);
				
			requestJSON = requestBody.asJson();
			if(requestJSON == null) {
				throw new RuntimeException("Expecting JSON data");
			}
			else {
				String type = requestJSON.findPath("type").textValue();
				
				if(type == null) {
					throw new RuntimeException("Missing parameter [type]");
				}
			}
			return GeoJSONParser.parse(requestJSON);
		} else {
			String message = "Request is null";
Logger.debug("\n" + message + "\n");
			throw new RuntimeException(message);
		}
	}
	
	@Transactional
	public static Result delete(long gid) {
		Long id = LocationRule.deleteTogetherWithAllGeometries(gid);
		setResponseLocation(null);
		if (id == null){
			return notFound();
		}
		return noContent();
	}
	
	@Transactional
	public static Result asKml(long gid) {
		Location location = LocationRule.read(gid);
		String result = KmlRule.asKml(location);
		response().setContentType("application/vnd.google-earth.kml+xml");
		return ok(result);
	}
}
