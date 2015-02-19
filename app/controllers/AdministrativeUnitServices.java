package controllers;

import interactors.GeoJSONParser;
import interactors.GeoJsonRule;
import interactors.KmlRule;
import interactors.LocationRule;
import models.geo.FeatureCollection;
import play.Logger;
import play.db.jpa.Transactional;
import play.libs.Json;
import play.mvc.*;
import play.mvc.Http.*;
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
			Location location = GeoJsonRule.asLocation(parsed);
			Long id = LocationRule.create(location);
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

	private static void setResponseLocation(Long id) {
		String uri = makeUri(id);
		response().setHeader(LOCATION, uri);
	}

	private static String makeUri(Long id) {
		Request request = Context.current().request();
		Logger.debug(""+ request.headers());
		String url = request.getHeader(ORIGIN) + request.path();
		String result = url;
		if (id != null)
			result += "/" + id;
		return result;
	}
	
	@Transactional
	public static Result read(String gid) {
		response().setContentType("application/vnd.geo+json");
		Location location = LocationRule.read(Long.parseLong(gid));
		return okJson(GeoJsonRule.asFeatureCollection(location));
	}
	
	@Transactional
	public static Result update(long gid) {
		try {
			FeatureCollection parsed = parseRequestAsFeatureCollection();
			Location location = GeoJsonRule.asLocation(parsed);
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

	@BodyParser.Of(BodyParser.Json.class)
	private static FeatureCollection parseRequestAsFeatureCollection() throws Exception {
		Request request = Context.current().request();
		JsonNode requestJSON = null;
		
Logger.debug("\n");
Logger.debug("=====");
		
		if(request != null) {
			RequestBody requestBody = request.body();
			
			String requestBodyText = requestBody.toString();
Logger.debug("Request [" + request.getHeader("Content-Type") + "], Length: " + requestBodyText.length() + "\n");
Logger.debug("Request Body:\n" + requestBodyText + "\n");
Logger.debug("Request.queryString():\n" + request.queryString() + "\n");
Logger.debug("Request.headers().toString():\n" + request.headers().toString() + "\n");
			
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
