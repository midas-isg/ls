package controllers;

import java.util.ArrayList;
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
    	/*List<Borough> boroughs = neighborhoodDAO.boroughList();
    	Map<String, Object> result = toMap(boroughs);
    	return ok(Json.toJson(result));*/
		List<County> all = new CountyDAO().findAllCounties();
		/*for (County c : all){
			System.out.println(c);
			c.geomText = (c.geom == null) ? null : c.geom.toText();
			//c.geom = null;
		}
		return ok();//Json.toJson(all));*/
		Map<String, Object> result = toMap(all);
    	return ok(Json.toJson(result));
    }

	private static Map<String, Object> toMap(List<County> boroughs) {
		Map<String, Object> result = new HashMap<>();
    	result.put("id", "nyc.Neighborhood");
    	result.put("type", "FeatureCollection");
    	result.put("features", toFeatures(boroughs));
		return result;
	}

	private static List<Object> toFeatures(List<County> boroughs) {
		List<Object> features = new LinkedList<>();
    	for (County borough : boroughs) {
    		features.add(toFeature(borough));
    	}
    	return features;
	}

	private static Map<String, Object> toFeature(County borough) {
		Map<String, Object> feature = new HashMap<>();
		feature.put("geometry", toGeometry(borough));
		feature.put("properties", toProperties(borough));
		feature.put("type", "Feature");
		return feature;
	}

	private static Map<String, Object> toGeometry(County borough) {
		Map<String, Object> geometry = new HashMap<>();
		Object[] list = new Object[1];
		list[0] = toCoordinates(borough.geom);
		geometry.put("coordinates", list);
		geometry.put("type", "MultiPolygon");
		return geometry;
	}

	private static List<Object> toCoordinates(Geometry geom) {
		List<Object> coordinates = new LinkedList<>();
    	//for (models.Neighborhood neighborhood : neighborhoods) {
    		List<Object> coordinate = toCondinates(geom.getCoordinates());
    		coordinates.add(coordinate);
    	//}
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
		coordinate.add(coordinate1.x);// / 100000f);
		coordinate.add(coordinate1.y);// / 100000f);
		return coordinate;
	}

	private static Map<String, Object> toProperties(County borough) {
		Map<String, Object> properties = new HashMap<>();
		String name = borough.name;
		properties.put("description", name);
		properties.put("id", borough.gid + "");
		properties.put("title", name);
		return properties;
	}
}
