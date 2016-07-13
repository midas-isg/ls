package controllers;

import play.Configuration;
import play.Play;
import play.db.jpa.Transactional;
import play.mvc.Controller;
import play.mvc.Result;


public class Application extends Controller {

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
   	
	@Transactional
	public Result index() {
		return ok(views.html.search.render("apollo location search", info()));
	}

	@Transactional
	public Result concept() {
		return ok(views.html.concept.render("apollo location services", info()));
	}
	
	@Transactional
	public Result browser() {
		return ok(views.html.browser.render("apollo location browser", info()));
	}
	
	@Transactional
	public Result create() {
		return ok(views.html.create.render("apollo location creator", info()));
	}

	public Result swagger() {
        return ok(views.html.swagger.render());
    }
}
