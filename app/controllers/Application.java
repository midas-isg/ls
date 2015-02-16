package controllers;

import play.Play;
import play.db.jpa.Transactional;
import play.mvc.Controller;
import play.mvc.Result;


public class Application extends Controller {

    private static String APP_VERSION = "Version: "	+ Play.application().configuration().getString("app.version");
   	private static String DB_NAME = "Database: " + Play.application().configuration().getString("db.default.url");
   	private static String VERSION = "Copyright 2014 - University of Pittsburgh, " + APP_VERSION + ", " + DB_NAME;

	@Transactional
	public static Result index() {
		return ok(views.html.search.render("apollo location search", VERSION));
	}
	
	@Transactional
	public static Result concept() {
		return ok(views.html.concept.render("apollo location services", VERSION));
	}
	
	@Transactional
	public static Result browser() {
		return ok(views.html.browser.render("apollo location browser", VERSION));
	}
	
	@Transactional
	public static Result create() {
		return ok(views.html.create.render("apollo location creator", VERSION));
	}
}
