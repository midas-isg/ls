package controllers;

import java.util.List;

import models.Borough;
import play.db.jpa.Transactional;
import play.mvc.Controller;
import play.mvc.Result;
import dao.NeighboorhoodPostGIS;
import dao.NeighborhoodDAO;

public class Neighborhood extends Controller {

	static String[] colors = {"goldenrod","green", "orange", "darkblue", "darkred"};
	
	//@Inject
	private static NeighborhoodDAO neighborhoodDAO = new NeighboorhoodPostGIS();

	@Transactional
    public static Result index() {
    	List<Borough> boroughs = neighborhoodDAO.boroughList();
    	
    	for (Borough borough : boroughs) {
    		int colorIndex = boroughs.indexOf(borough);
    		borough.color = colors[colorIndex];
    	}

    	return ok(views.html.neighborhood.index.render(boroughs));
    }
	
}
