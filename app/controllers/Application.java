package controllers;

import java.sql.Date;
import javax.persistence.EntityManager;
import dao.entities.AdministrativeUnit;
import dao.entities.Data;
import play.db.jpa.JPA;

//import interactors.CountyRule;

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
	public static Result test() {
		EntityManager em = JPA.em();
		AdministrativeUnit au = new AdministrativeUnit();
		Data data = new Data();
		data.setName("au1");
		data.setStartDate(new Date(112,2,1));
		data.setProtect(true);
		data.setUpdateDate(new Date(114,1,1));
		au.setData(data);
		try {
			em.persist(au);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return ok();
	}

	@Transactional
	public static Result leaflet() {
		//List<County> all = new CountyDAO().findAllCounties();
		Object result = null;//CountyRule.toFeatureCollection(all);
		return ok(Json.toJson(result));
	}
}
