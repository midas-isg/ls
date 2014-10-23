package controllers;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import models.Borough;
import play.db.jpa.Transactional;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;

import com.vividsolutions.jts.geom.Coordinate;

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
	
	@Transactional
    public static Result leaflet() {
    	List<Borough> boroughs = neighborhoodDAO.boroughList();
    	Map<String, Object> result = toMap(boroughs);
    	return ok(Json.toJson(result));
    }

	private static Map<String, Object> toMap(List<Borough> boroughs) {
		Map<String, Object> result = new HashMap<>();
    	result.put("id", "nyc.Neighborhood");
    	result.put("type", "FeatureCollection");
    	result.put("features", toFeatures(boroughs));
		return result;
	}

	private static List<Object> toFeatures(List<Borough> boroughs) {
		List<Object> features = new LinkedList<>();
    	for (Borough borough : boroughs) {
    		features.add(toFeature(borough));
    	}
    	return features;
	}

	private static Map<String, Object> toFeature(Borough borough) {
		Map<String, Object> feature = new HashMap<>();
		feature.put("geometry", toGeometry(borough));
		feature.put("properties", toProperties(borough));
		return feature;
	}

	private static Map<String, Object> toGeometry(Borough borough) {
		Map<String, Object> geometry = new HashMap<>();
		geometry.put("coordinates", toCoordinates(borough.neighborhoods));
		geometry.put("type", "MultiPolygon");
		return geometry;
	}

	private static List<Object> toCoordinates(List<models.Neighborhood> neighborhoods) {
		List<Object> coordinates = new LinkedList<>();
    	for (models.Neighborhood neighborhood : neighborhoods) {
    		List<Object> coordinate = toCondinates(neighborhood.geom.getCoordinates());
    		coordinates.add(coordinate);
    	}
    	return coordinates;
	}

	private static List<Object> toCondinates(Coordinate[] coordinates) {
		List<Object> result = new LinkedList<>();
		for (Coordinate coordinate1: coordinates){
			result.add(toCoordinate(coordinate1));
		}
		return result;
	}

	private static List<Object> toCoordinate(Coordinate coordinate1) {
		List<Object> coordinate = new LinkedList<>();
		coordinate.add(coordinate1.x);
		coordinate.add(coordinate1.y);
		return coordinate;
	}

	private static Map<String, Object> toProperties(Borough borough) {
		Map<String, Object> properties = new HashMap<>();
		String name = borough.name;
		properties.put("description", name);
		properties.put("id", name);
		properties.put("title", name);
		return properties;
	}
}
