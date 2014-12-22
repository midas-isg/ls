package controllers;

import interactors.AuRule;
import interactors.GeoJSONParser;

import java.util.List;

import models.FancyTreeNode;
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
					return badRequest("Expecting JSON data");
				}
				else {
					String type = requestJSON.findPath("type").textValue();
					
					if(type == null) {
						return badRequest("Missing parameter [type]");
					}
				}
				FeatureCollection parsed = GeoJSONParser.parse(requestJSON);
				Long id = AuRule.create(parsed);
				String uri = getUri(request, id);
				response().setHeader(LOCATION, uri);
				//response().setHeader(CONTENT_LOCATION, uri);
				return created();
			}
			else {
				String message = "Request is null";
Logger.debug("\n" + message + "\n");
				return  badRequest(message);
			}
		}
		catch (Exception e) {
			return forbiddenJson(e);
		}
	}

	private static String getUri(Request request, Long id) {
		Logger.debug(""+ request.headers());
		String url = request.getHeader(ORIGIN) + request.path();
		return url + "/" + id;
	}
	
	@Transactional
	public static Result read(String gid) {
		response().setContentType("application/vnd.geo+json");
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
	
	@Transactional
	public static Result tree() {
		List<FancyTreeNode> tree = TreeViewAdapter.toFancyTree(AuRule.getHierarchy());
		return okJson(tree);
	}
}
