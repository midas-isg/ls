package controllers;

import play.Configuration;
import play.Play;
import play.db.jpa.Transactional;
import play.mvc.Controller;
import play.mvc.Result;
import security.controllers.UserController;


public class Application extends UserController {

	private static String INFO = null;

	public static String info() {
		init();
		return INFO;
	}

	private static void init(){
		if (INFO == null){
			final Configuration cfg = Play.application().configuration();
			String version = "Version: " + cfg.getString("app.version");
			String dbName = "Database: " + cfg.getString("db.default.url");
			INFO = "Copyright 2014-2016 - University of Pittsburgh, " 
				+ version + ", " + dbName;
		}
	}
	
	public Result index() {
		return basicSearch();
	}
	
	public Result basicSearch() {
		return ok(views.html.index.render("location search", info()));
	}
	
	public Result mapSearch() {
		return ok(views.html.map_search.render("location search", info()));
	}
	
	public Result advancedSearch() {
		return ok(views.html.advanced_search.render("location search", info()));
	}
	
	@Transactional
	public Result concept() {
		return ok(views.html.concept.render("location services", info()));
	}
	
	@Transactional
	public Result browser() {
		return ok(views.html.browser.render("location browser", info()));
	}
	
	@Transactional
	public Result resolver() {
		return ok(views.html.resolver.render("location resolver", info()));
	}
	
	@Transactional
	public Result create() {
		return ok(views.html.create.render("location creator", info()));
	}
}
