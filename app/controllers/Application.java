package controllers;

import play.db.jpa.Transactional;
import play.mvc.Controller;
import play.mvc.Result;
import views.html.index;

public class Application extends Controller {

	@Transactional
    public static Result index() {
        return ok(index.render("PlayGIS Index"));
    }

}
