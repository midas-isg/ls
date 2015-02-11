package controllers;

import interactors.AuHierarchyRule;
import interactors.GeoJSONParser;
import interactors.LocationRule;

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

import dao.AuDao;

public class AdministrativeUnitServices extends Controller {
	static Status okJson(Object resultObject) {
		return ok(Json.toJson(resultObject));
	}
	
	@Transactional
	public static Result create() {
		try {
			FeatureCollection parsed = parseRequestAsFeatureCollection();
			Long id = LocationRule.create(parsed);
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
		return url + "/" + id;
	}
	
	@Transactional
	public static Result read(String gid) {
		response().setContentType("application/vnd.geo+json");
		return okJson(LocationRule.getFeatureCollection(Long.parseLong(gid)));
	}
	
	@Transactional
	public static Result update(long gid) {
		try {
			FeatureCollection parsed = parseRequestAsFeatureCollection();
			LocationRule.update(gid, parsed);
			setResponseLocation(gid);
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

	private static FeatureCollection parseRequestAsFeatureCollection() throws Exception {
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
		LocationRule.delete(gid);
		setResponseLocation(gid);
		return noContent();
	}
	
	private static Status auTree = null;
	@Transactional
	public synchronized static Result tree() {
		if (auTree == null){
			List<FancyTreeNode> tree = TreeViewAdapter.toFancyTree(AuHierarchyRule.getHierarchy());
			auTree = okJson(TreeViewAdapter.removeEpidemicZone(tree));
		}
		return auTree;
	}
	
	@Transactional
	public static Result tree2() {
		List<FancyTreeNode> tree = TreeViewAdapter.toFancyTree(new AuDao().findRoots());
		return okJson(tree);
	}

	@Transactional
	public static Result asKml(long gid) {
		String result = LocationRule.asKml(gid);
		response().setContentType("application/vnd.google-earth.kml+xml");
		return ok(result);
	}

}
