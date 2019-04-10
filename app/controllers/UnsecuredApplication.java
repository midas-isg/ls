package controllers;

import play.Configuration;
import play.Logger;
import play.Play;
import play.db.jpa.Transactional;
import play.libs.Scala;
import play.mvc.Result;
import security.auth0.Auth0Aid;
import play.mvc.Http.Context;
import play.mvc.Controller;


public class UnsecuredApplication extends Controller {
	
	private static String INFO = null;
	
	public static String info() {
		init();
		return INFO;
	}
	
	private static void init() {
		if (INFO == null) {
			final Configuration cfg = Play.application().configuration();
			String version = "Version: " + cfg.getString("app.version");
			String dbName = "Database: " + cfg.getString("db.default.url");
			
			INFO = "Copyright 2014-2019 - University of Pittsburgh, " 
				+ version;
			
			if(Play.isDev()) {
				INFO += ", " + dbName;
			}
		}
	}
	
	public Result about() {
		return ok(views.html.about.render("about", info(), Application.hasCredentials()));
	}
	
	public Result browser() {
		return ok(views.html.browser.render("browser", info(), Application.hasCredentials()));
	}
	
	public Result error() {
		return ok(views.html.error.render("error", info(), Application.hasCredentials()));
	}
	
	public Result index() {
		return ok(views.html.index.render("search", info(), Application.hasCredentials()));
	}
	
	public Result login() {
		return ok(views.html.login.render("search", info(), "SPOOF!", "null", "null"));
	}
	
	public Result mapSearch() {
		return ok(views.html.map_search.render("map search", info(), Application.hasCredentials()));
	}
	
	public Result results() {
		return ok(views.html.results.render("refine search", info(), Application.hasCredentials()));
	}
}
