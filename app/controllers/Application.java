package controllers;

import java.sql.Date;
import javax.persistence.EntityManager;
import dao.entities.AdministrativeUnit;
import dao.entities.Data;
import play.db.jpa.JPA;
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
	public static Result test() {
		EntityManager em = JPA.em();
		AdministrativeUnit au = new AdministrativeUnit();
		Data data = new Data();
		data.setName("au1");
		data.setStartDate(new Date(112,2,1));
		data.setLocked(true);
		data.setUpdateDate(new Date(114,1,1));
		au.setData(data);
		//em.getTransaction().begin();
		try {
			em.persist(au);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		//em.getTransaction().commit();
		//em.close();
		return ok();
	}

	@Transactional
	public static Result leaflet() {
		List<County> all = new CountyDAO().findAllCounties();
		Object result = CountyRule.toFeatureCollection(all);
		return ok(Json.toJson(result));
	}
	
	@Transactional
	public static Result createMap() {
		return ok(views.html.index.render("TODO: Replace w/ createJSON service"));
	}
	
	@Transactional
	public static Result retrieveMap() {
		return ok(views.html.index.render("TODO: Replace w/ retrieveJSON service"));
	}
	
	@Transactional
	public static Result updateMap() {
		return ok(views.html.index.render("TODO: Replace w/ updateJSON service"));
	}
	
	@Transactional
	public static Result deleteMap() {
		return ok(views.html.index.render("TODO: Replace w/ deleteJSON service"));
	}
}
