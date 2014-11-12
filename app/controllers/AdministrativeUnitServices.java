package controllers;

import interactors.CountyRule;

import java.util.List;

import play.db.jpa.Transactional;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import dao.CountyDAO;
import dao.entities.County;

public class AdministrativeUnitServices extends Controller {	
	@Transactional
	public static Result create() {
		return ok(views.html.index.render("TODO: Replace w/ create service"));
	}
	
	@Transactional
	public static Result read() {
		return ok(views.html.index.render("TODO: Replace w/ read service"));
	}
	
	@Transactional
	public static Result update() {
		return ok(views.html.index.render("TODO: Replace w/ update service"));
	}
	
	@Transactional
	public static Result delete() {
		return ok(views.html.index.render("TODO: Replace w/ delete service"));
	}
}
