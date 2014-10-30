package controllers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import models.Borough;
import models.geo.Feature;
import models.geo.FeatureCollection;
import models.geo.FeatureGeometry;
import models.geo.MultiPolygon;
import play.db.jpa.Transactional;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;

import dao.CountyDAO;
import dao.NeighboorhoodPostGIS;
import dao.NeighborhoodDAO;
import dao.entities.County;

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
	
	@Transactional
    public static Result leaflet() {
		List<County> all = new CountyDAO().findAllCounties();
		Object result = toFeatureCollection(all);
    	return ok(Json.toJson(result));
    }

	private static FeatureCollection toFeatureCollection(List<County> countys) {
		FeatureCollection fc = new FeatureCollection();
		fc.features = toFeatures(countys);
		return fc;
	}

	private static List<Feature> toFeatures(List<County> countys) {
		List<Feature> features = new ArrayList<>();
    	for (County county : countys) {
    		features.add(toFeature(county));
    	}
    	return features;
	}

	private static Feature toFeature(County county) {
		Feature feature = new Feature();
		feature.properties = toProperties(county);
		feature.geometry = toFeatureGeometry(county.geom);
		return feature;
	}


	private static Map<String, String> toProperties(County county) {
		Map<String, String> properties = new HashMap<>();
		String name = county.namelsad;
		properties.put("description", county.name + " in statefp="+ county.statefp);
		properties.put("id", county.geoid);
		properties.put("title", name);
		return properties;
	}

	private static FeatureGeometry toFeatureGeometry(Geometry geom) {
		List<List<List<double[]>>> list = new ArrayList<>();
		list.add(toCoordinates(geom));
		MultiPolygon g = new MultiPolygon();
		g.coordinates = list;
		return g; 
	}

	private static List<List<double[]>> toCoordinates(Geometry geom) {
		List<List<double[]>> coordinates = new ArrayList<>();
   		List<double[]> coordinate = toCondinates(geom.getCoordinates());
   		coordinates.add(coordinate);
    	return coordinates;
	}

	private static List<double[]> toCondinates(Coordinate[] coordinates) {
		List<double[]> result = new ArrayList<>();
		for (Coordinate coordinate1: coordinates){
			result.add(toCoordinate(coordinate1));
		}
		return result;
	}

	private static double[] toCoordinate(Coordinate coordinate1) {
		double[] coordinate = new double[2];
		coordinate[0] = coordinate1.x;
		coordinate[1] = coordinate1.y;
		return coordinate;
	}
}
