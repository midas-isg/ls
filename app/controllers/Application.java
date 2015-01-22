package controllers;

import play.db.jpa.Transactional;
import play.mvc.Controller;
import play.mvc.Result;


public class Application extends Controller {
	
	@Transactional
	public static Result index() {
		return ok(views.html.search.render("apollo location search"));
	}
	
	@Transactional
	public static Result concept() {
		return ok(views.html.index.render("apollo location services"));
	}
	
	@Transactional
	public static Result browser() {
		return ok(views.html.browser.render("apollo location browser"));
	}
}
