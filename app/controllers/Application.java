package controllers;

import interactors.CountyRule;

import java.util.List;

import play.db.jpa.Transactional;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import dao.CountyDAO;
import dao.entities.County;

public class Application extends Controller {

	@Transactional
	public static Result index() {
		return ok(views.html.index.render("PlayGIS Index"));
	}

	@Transactional
	public static Result leaflet() {
		List<County> all = new CountyDAO().findAllCounties();
		Object result = CountyRule.toFeatureCollection(all);
		return ok(Json.toJson(result));
	}
	
	@Transactional
	public static Result createJSON() {
		return ok(views.html.index.render("TODO: Replace w/ createJSON service"));
	}
	
	@Transactional
	public static Result retrieveJSON() {
		return ok(views.html.index.render("TODO: Replace w/ retrieveJSON service"));
	}
	
	@Transactional
	public static Result updateJSON() {
		return ok(views.html.index.render("TODO: Replace w/ updateJSON service"));
	}
	
	@Transactional
	public static Result deleteJSON() {
		return ok(views.html.index.render("TODO: Replace w/ deleteJSON service"));
	}
}
