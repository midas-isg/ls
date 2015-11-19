package controllers;

import interactors.ApolloLocationRule;
import interactors.XmlRule;
import play.db.jpa.Transactional;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import dao.entities.Location;

public class ApolloLocationServices  extends Controller {
	
	@Transactional
	static Result asJson(Location location){
		return okJson(ApolloLocationRule.asApolloLocation(location));
	}
	
	@Transactional
	static Result asXml(Location location){
		return okAsXml(Wire.asXml(location));
	}

	public static class Wire { 
		public static String asXml(Location location) {
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

