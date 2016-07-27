package controllers;

import play.mvc.Controller;
import play.mvc.Result;
import play.twirl.api.Html;

public class Test extends Controller {
    public Result acceptance() {
        return ok(views.html.tests.acceptance.render());
    }
    
    public Result qunitTest(String title, Html imports, Html content) {
    	return ok(views.html.tests.qunit_test.render(title, imports, content));
    }
    
    public Result searchResultsTest() {
        return ok(views.html.tests.search_results.render("Search Results Test"));
    }
}
