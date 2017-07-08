package controllers;

import play.mvc.Controller;
import play.mvc.Result;


public class ApiDocument extends Controller {
	public Result swagger() {
        return ok(views.html.swagger.render());
    }
}
