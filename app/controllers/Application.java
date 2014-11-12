package controllers;

import java.sql.Date;

import javax.persistence.EntityManager;

import dao.entities.AdministrativeUnit;
import dao.entities.Data;
import play.db.jpa.JPA;
import play.db.jpa.Transactional;
import play.mvc.Controller;
import play.mvc.Result;
import views.html.index;

public class Application extends Controller {

	@Transactional
	public static Result index() {
		return ok(index.render("PlayGIS Index"));
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

}
