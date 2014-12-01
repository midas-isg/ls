package controllers;

import play.db.jpa.Transactional;
import play.mvc.Controller;
import play.mvc.Result;


public class Application extends Controller {
	
	@Transactional
	public static Result index() {
		return ok(views.html.index.render("Location Services"));
	}
}
