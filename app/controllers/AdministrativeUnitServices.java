package controllers;

//import interactors.CountyRule;

import interactors.AuRule;
import interactors.GeoJSONParser;
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

public class AdministrativeUnitServices extends Controller {
	static Status okJson(Object resultObject) {
		return ok(Json.toJson(resultObject));
	}
	
	private static Status forbiddenJson(Exception e) {
		Logger.error(e.toString());
		e.printStackTrace();
		
		return forbidden(Json.toJson(e.toString()));
	}
	
	private static Result okCRUD(Object result) {
		return okJson(result);
	}
	
	@Transactional
	public static Result create() {
		try {
			Object result = null;
			Request request = Context.current().request();
			RequestBody requestBody = null;
			JsonNode requestJSON = null;
			
			Logger.debug("\n");
			Logger.debug("=====");
			
			if(request != null) {
				requestBody = request.body();
				
				String requestBodyText = requestBody.toString();
				Logger.debug("Request [" + request.getHeader("Content-Type") + "], Length: " + requestBodyText.length());
				//Logger.debug("Request Body:\n" + requestBodyText);
				
				requestJSON = requestBody.asJson();
				if(requestJSON == null) {
					return badRequest("Expecting JSON data");
				}
				else {
					String type = requestJSON.findPath("type").textValue();
					
					if(type == null) {
						return badRequest("Missing parameter [type]");
					}
				}
				
				result = requestJSON;
			}
			else {
				Logger.debug("\nRequest is null\n");
			}
			
			FeatureCollection parsed = GeoJSONParser.parse(requestJSON);
			Long id = AuRule.create(parsed);
			Logger.debug("CountyRule save =" + id);
			Logger.debug("=====");
			
			return okCRUD(result);
		}
		catch (Exception e) {
			return forbiddenJson(e);
		}
	}
	
	@Transactional
	public static Result read(String gid) {
		return okCRUD(AuRule.getFeatureCollection(Long.parseLong(gid)));
	}
	
	@Transactional
	public static Result update() {
		return ok(views.html.index.render("TODO: Replace w/ update service"));
	}
	
	@Transactional
	public static Result delete() {
		return ok(views.html.index.render("TODO: Replace w/ delete service"));
	}
}
