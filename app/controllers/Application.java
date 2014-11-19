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
}
