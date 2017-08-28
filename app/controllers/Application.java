package controllers;

import play.Configuration;
import play.Logger;
import play.Play;
import play.db.jpa.Transactional;
import play.mvc.Controller;
import play.mvc.Http.Context;
import play.mvc.Result;
import security.controllers.UserController;


public class Application extends UserController {

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
			
			INFO = "Copyright 2014-2017 - University of Pittsburgh, " 
				+ version;
			
			if(Play.isDev()) {
				INFO += ", " + dbName;
			}
		}
	}
	
	public Result index() {
		return basicSearch();
	}
	
	public Result basicSearch() {
		return ok(views.html.index.render("search", info()));
	}
	
	public Result mapSearch() {
		return ok(views.html.map_search.render("map search", info()));
	}
	
	public Result translate() {
		return ok(views.html.translate.render("translator", info()));
	}
	
	public Result results() {
		return ok(views.html.results.render("refine search", info()));
	}
	
	public Result browser() {
		return ok(views.html.browser.render("browser", info()));
	}
	
	@Transactional
	public Result resolver() {
		return ok(views.html.resolver.render("resolver", info()));
	}
	
	@Transactional
	public Result create() {
		return ok(views.html.create.render("creator", info()));
	}
	
	@Transactional
	public Result concept() {
		Logger.warn("\nWARNING! " + Context.current().request().uri() + " is deprecated and may stop being available in the future!\n");
		return ok(views.html.concept.render("concept", info()));
	}
}
