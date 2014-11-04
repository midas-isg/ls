package controllers;

import interactors.GeoRule;

import java.util.List;

import dao.CountyDAO;
import dao.entities.County;
import play.db.jpa.Transactional;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import views.html.index;

public class Application extends Controller {

	@Transactional
    public static Result index() {
        return ok(index.render("PlayGIS Index"));
    }
	
	@Transactional
    public static Result leaflet() {
		List<County> all = new CountyDAO().findAllCounties();
		Object result = GeoRule.toFeatureCollection(all);
    	return ok(Json.toJson(result));
    }


}
