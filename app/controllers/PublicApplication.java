package controllers;

import java.util.Map;

import controllers.LocationServices.Wire;
import dao.entities.UserAccount;
import models.exceptions.PostgreSQLException;
import models.geo.FeatureCollection;
import play.Configuration;
import play.Logger;
import play.Play;
import play.db.jpa.Transactional;
import play.libs.Scala;
import play.mvc.Result;
import security.auth0.Auth0Aid;
import play.mvc.Http.Context;
import play.mvc.Http.Request;
import play.mvc.Http.RequestBody;
import scala.collection.concurrent.Debug;
import play.mvc.Controller;


public class PublicApplication extends Controller {
	
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
		return ok(views.html.login.render("search", info(), Application.hasCredentials()));
	}
	
	public Result createAccount() {
		Request request = Context.current().request();
		
		if (request != null) {
			RequestBody requestBody = request.body();
			Map<String, String[]> requestMap = requestBody.asFormUrlEncoded();
			String userEmail = requestMap.get("user-email")[0];
			String userPassword = requestMap.get("user-password")[0];
			
			Logger.debug("Attempt to login with " + userEmail);
		}
		
		try {
			UserAccount userAccount = null;
		}
		/*
		try {
			FeatureCollection parsed = parseRequestAsFeatureCollection();
			Long id = Wire.create(parsed);
			setResponseLocation(id);

			return created();
		}
		catch(PostgreSQLException e){
			String message = e.getMessage();
			if(e.getSQLState()!= null && e.getSQLState().equals(UNIQUE_VIOLATION))
				return forbidden(message);
			
			return badRequest(message);
		}
		*/
		catch (RuntimeException e) {
			String message = e.getMessage();
			Logger.error(message, e);

			return badRequest(message);
		}
		catch (Exception e) {
			String message = e.getMessage();
			Logger.error(message, e);

			return forbidden(message);
		}
		
		return redirect("/ls/login-test");
	}
	
	public Result mapSearch() {
		return ok(views.html.map_search.render("map search", info(), Application.hasCredentials()));
	}
	
	public Result results() {
		return ok(views.html.results.render("refine search", info(), Application.hasCredentials()));
	}
}
