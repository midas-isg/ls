package interactors;

import java.util.ArrayList;
import java.util.List;

import play.Logger;
import models.geo.Feature;
import models.geo.FeatureCollection;
import models.geo.FeatureGeometry;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateSequence;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.impl.CoordinateArraySequence;

public class GeoInputRule {
	static Geometry toGeometry(FeatureCollection fc) {
		GeometryFactory fact = new GeometryFactory();
		List<Feature> features = fc.getFeatures();
		List<Geometry> polygons = new ArrayList<Geometry>();
		
		for(Feature feature :features) {
			FeatureGeometry featureGeometry = feature.getGeometry();
			String geometryType = featureGeometry.getType();
			
			if(geometryType.equals("GeometryCollection")) {
				List<FeatureGeometry> subGeometries = ((models.geo.GeometryCollection)featureGeometry).getGeometries();
				
				for(FeatureGeometry subGeometry : subGeometries) {
					Feature geometryFeature = new Feature();
					String type = subGeometry.getType();
					
					switch(type) {
						case "MultiPolygon":
							models.geo.MultiPolygon multipolygonBody = (models.geo.MultiPolygon)subGeometry;
							geometryFeature.setGeometry(multipolygonBody);
						break;
						
						case "Polygon":
							models.geo.Polygon polygonBody = (models.geo.Polygon)subGeometry;
							geometryFeature.setGeometry(polygonBody);
						break;
						
						case "Point":
							//TODO: uncomment this once processGeometryTypes() supports Point processing
							//TODO: update processGeometryTypes() to support Points
							//models.geo.Point pointBody = (models.geo.Point) featureGeometry;
							//geometryFeature.setGeometry(pointBody);
						//break;
						
						default:
						break;
					}
					geometryFeature.setType(type);
					
					processGeometryTypes(fact, polygons, geometryFeature, subGeometry);
				}
			}
			else if(geometryType.equals("Point")) {
				if(features.size() == 1) {
					models.geo.Point pointGeometry = (models.geo.Point) featureGeometry;
					Coordinate [] coordinates = new Coordinate[1];
					coordinates[0] = new Coordinate(pointGeometry.getLatitude(), pointGeometry.getLongitude());
					CoordinateSequence coordinate = new CoordinateArraySequence(coordinates);
					Point outputPoint = new Point((CoordinateSequence) coordinate, fact);
					
					return outputPoint;
				}
				
				throw new RuntimeException("A Point cannot be saved with other points or geometry types");
			}
			else {
				processGeometryTypes(fact, polygons, feature, featureGeometry);
			}
		}
		
		//TODO: return GeometryCollection instead of MultiPolygon so that system supports points
		
		Polygon [] polygonArray = polygons.toArray(new Polygon[polygons.size()]);
//Logger.debug("p0=" + polygonArray[0].getDimension());
//Logger.debug("\tx=" + polygonArray[0].getExteriorRing().getCoordinateN(0).x);
//Logger.debug("\ty=" + polygonArray[0].getExteriorRing().getCoordinateN(0).y);
		MultiPolygon mpg = new MultiPolygon(polygonArray, fact);
		
		return mpg;
	}

	public static void processGeometryTypes(GeometryFactory fact,
			List<Geometry> polygons, Feature feature, FeatureGeometry geometry) {
		if(geometry.getType().equals("MultiPolygon")) {
//Logger.debug("============");
			models.geo.MultiPolygon multipolygon = (models.geo.MultiPolygon)geometry;
			List<List<List<double[]>>> multipolygonsCoordinatesList = multipolygon.getCoordinates();
			int polygonCount = multipolygonsCoordinatesList.size();
//Logger.debug("There exist/s " + polygonCount + " Polygon/s");
			
			for(int j = 0; j < polygonCount; j++) {
				List<List<double []>> polygonsCoordinatesList = multipolygonsCoordinatesList.get(j);
//Logger.debug("There exist/s " + polygonsCoordinatesList.size() + " linear ring/s");
					Feature polygonFeature = new Feature();
					models.geo.Polygon polygonBody = new models.geo.Polygon();
					
					polygonBody.setCoordinates(polygonsCoordinatesList);
					polygonBody.setType("Polygon");
					polygonFeature.setType("Polygon");
					polygonFeature.setGeometry(polygonBody);
//Logger.debug("Polygon[" + j + "]");
					Polygon polygon = toPolygon(fact, polygonFeature);
					polygons.add(polygon);
			}
//Logger.debug("============");
		}
		else if(geometry.getType().equals("Polygon")) {
			Polygon polygon = toPolygon(fact, feature);
			polygons.add(polygon);
		}
		else /*if(geometry.getType() is not supported)*/ {
			Logger.error("ERROR: Malformed GeoJSON");
			Logger.error(geometry.getType() + " is not supported at Geometry level.");
		}
		
		return;
	}

	private static Polygon toPolygon(GeometryFactory fact, Feature feature) {
		List<Coordinate[]> coordinates = toCoordinates(feature);
		Polygon poly = null;
		
		try {
				LinearRing shell = fact.createLinearRing(coordinates.get(0));
				List<LinearRing> holes = new ArrayList<LinearRing>();
				
				for(int i = 1; i < coordinates.size(); i++) {
					holes.add(fact.createLinearRing(coordinates.get(i)));
//Logger.debug("Inner: " + holes.get(i - 1));
				}
				
				poly = fact.createPolygon(shell, holes.toArray(new LinearRing[coordinates.size() - 1]));
//Logger.debug("Stored outer: " + poly.getExteriorRing());
//Logger.debug(poly.getNumInteriorRing() + " inner ring/s");
		}
		catch(IllegalArgumentException e) {
			Logger.error("IllegalArgumentException: \n\t" + e.getLocalizedMessage() + "\n\t" + e.getMessage());
		}
		
		return poly;
	}

	private static List<Coordinate[]> toCoordinates(Feature feature) {
		FeatureGeometry fg = feature.getGeometry();
		String type = fg.getType();
		List<List<Coordinate>> coordinates = null;
		
//Logger.debug("type is: " + type);
		if(type.equals("Polygon")) {
			models.geo.Polygon polygon = (models.geo.Polygon)fg;
			List<List<double[]>> pointCollection = polygon.getCoordinates();
			int polygonRingsCount = pointCollection.size();
			
			coordinates = new ArrayList<List<Coordinate>>();
			for(int i = 0; i < polygonRingsCount; i++) {
				List<double []> polygonComponent = pointCollection.get(i);
				int pointsCount = polygonComponent.size();
				
				coordinates.add(new ArrayList<Coordinate>());
				List<Coordinate> ringList = coordinates.get(i);
				
				for(int j = 0; j < pointsCount; j++) {
					double point[] = polygonComponent.get(j);
					
					Coordinate coordinateToAdd = new Coordinate();
					for(int k = 0; k < 2/*point.length*/; k++) {
						coordinateToAdd.setOrdinate(k, point[k]);
					}
					ringList.add(coordinateToAdd);
				}
			}
		}
		else {
			throw new RuntimeException(type + " Geometry is not supported.");
		}
		
		List<Coordinate []> output = null;
		
		if(coordinates != null) {
			output = new ArrayList<Coordinate[]>();
			
			for(int i = 0; i < coordinates.size(); i++) {
				output.add(new Coordinate[coordinates.get(i).size()]);
				
				for(int j = 0; j < coordinates.get(i).size(); j++) {
					output.get(i)[j] = coordinates.get(i).get(j);
//Logger.debug("[" + i + "][" + j + "]: " + output.get(i)[j]);
				}
			}
		}
		
		return output;
	}
}
