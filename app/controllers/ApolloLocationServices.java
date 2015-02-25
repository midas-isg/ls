package controllers;

import interactors.ApolloLocationRule;
import interactors.LocationRule;
import interactors.XmlRule;
import play.db.jpa.Transactional;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import dao.entities.Location;

public class ApolloLocationServices  extends Controller {
	
	@Transactional
	public static Result locationsInJson(String gid){
		Location location = LocationRule.read(Long.parseLong(gid));
		return okJson(ApolloLocationRule.asApolloLocation(location));
	}
	
	@Transactional
	public static Result locationsInXml(String gidText){
		long gid = Long.parseLong(gidText);
		return okAsXml(Wire.readAsXml(gid));
	}

	public static class Wire { 
		public static String readAsXml(long gid) {
			Location location = LocationRule.read(gid);
			Object al = ApolloLocationRule.asApolloLocation(location);
			return toXml(al);
		}
	}
	
	private static Result okAsXml(String xml) {
		response().setContentType("application/xml");
		return ok(xml);
	}

	private static String toXml(Object result) {
		try {
			return XmlRule.toXml(result);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	private static Status okJson(Object result) {
		return ok(Json.toJson(result));
	}
}

