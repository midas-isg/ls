package controllers;

import interactors.ApolloLocationRule;
import interactors.LocationRule;
import interactors.XmlRule;
import play.db.jpa.Transactional;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import dao.entities.Location;

public class EpidemicZoneServices  extends Controller {
	
	@Transactional
	public static Result locationsInJson(String gid){
		Location au = LocationRule.findByGid(Long.parseLong(gid));
		return okJson(ApolloLocationRule.toApolloLocation(au));
	}
	
	@Transactional
	public static Result locationsInXml(String gid){
		Location au = LocationRule.findByGid(Long.parseLong(gid));
		return okAsXml(ApolloLocationRule.toApolloLocation(au));
	}
	
	private static Result okAsXml(Object result) {
		String xml = toXml(result);
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

