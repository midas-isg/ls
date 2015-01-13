package controllers;

import play.db.jpa.Transactional;
import play.mvc.Controller;
import play.mvc.Result;


public class Application extends Controller {
	
	@Transactional
	public static Result index() {
		return ok(views.html.index.render("Apollo Location Services"));
	}
	
	@Transactional
	public static Result browser() {
		return ok(views.html.browser.render("Apollo Location Browser"));
	}
}
