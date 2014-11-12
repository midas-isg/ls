package controllers;

//import interactors.CountyRule;

import interactors.GeoJSONParser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import models.geo.*;
import play.Logger;
import play.db.jpa.Transactional;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Http.Context;
import play.mvc.Http.MultipartFormData;
import play.mvc.Http.Request;
import play.mvc.Http.RequestBody;
import play.mvc.Result;
import play.data.DynamicForm;
import play.data.Form;
import play.mvc.BodyParser;

import com.fasterxml.jackson.databind.JsonNode;

//import dao.CountyDAO;
//import dao.entities.County;

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
		//Map<String, String> resultMap = new HashMap<String, String>();
		//resultMap.put("id", String.valueOf(result));
		
		//return okJson(resultMap);
		return okJson(result);
	}
	
	@Transactional
	//@BodyParser.Of(BodyParser.Json.class)
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
			Logger.debug(parsed.toString());
			Logger.debug("=====");
			
			return okCRUD(result);
		}
		catch (Exception e) {
			return forbiddenJson(e);
		}
	}
	
	@Transactional
	public static Result read() {
		return ok(views.html.index.render("TODO: Replace w/ read service"));
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
