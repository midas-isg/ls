package controllers;

import play.Configuration;
import play.Play;
import play.db.jpa.Transactional;
import play.mvc.Controller;
import play.mvc.Result;


public class ApiDocument extends Controller {
	public Result swagger() {
        return ok(views.html.swagger.render());
    }
}
