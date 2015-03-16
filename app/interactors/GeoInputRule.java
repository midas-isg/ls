package interactors;

import java.util.ArrayList;
import java.util.List;

import play.Logger;
import models.geo.Feature;
import models.geo.FeatureCollection;
import models.geo.FeatureGeometry;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;

import dao.entities.LocationGeometry;

public class GeoInputRule {
	/** 
	 * Converts every features into geometries and union them all into 
	 * a single multipolygon to be persisted into the database. The geometries 
	 * could be in form of geometry coordinates or referring to a GID of 
	 * an existing location in the database. 
	 * 
	 * This can be used to create an EZ from an existing AU but the client 
	 * (e.g. front-end) has to make sure that only one feature with a GID 
	 * in the FeatureCollection.  
	 */
	static Geometry toMultiPolygon(FeatureCollection fc) {
		GeometryFactory fact = new GeometryFactory();
		List<Feature> features = fc.getFeatures();
		List<Polygon> polygons = new ArrayList<Polygon>();
		List<MultiPolygon> multipolygons = new ArrayList<>();
		
		for(Feature feature :features) {
			FeatureGeometry geometry = feature.getGeometry();
			if (geometry == null){
				String id = feature.getId();
				if (id != null){
					LocationGeometry lg = GeometryRule.read(Long.parseLong(id));
					Geometry geo = lg.getMultiPolygonGeom();
					if ("MultiPolygon".equals(geo.getGeometryType())){
						MultiPolygon mp = (MultiPolygon)geo;
						multipolygons.add(mp);
					}
				}
			} else if(geometry.getType().equals("GeometryCollection")) {
				List<FeatureGeometry> subGeometries = ((models.geo.GeometryCollection)geometry).getGeometries();
				
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
						
						default:
						break;
					}
					geometryFeature.setType(type);
					
					processGeometryTypes(fact, polygons, geometryFeature, subGeometry);
				}
			}
			else{
				processGeometryTypes(fact, polygons, feature, geometry);
			}
		}
		
		Polygon [] polygonArray = polygons.toArray(new Polygon[polygons.size()]);
//Logger.debug("p0=" + polygonArray[0].getDimension());
//Logger.debug("\tx=" + polygonArray[0].getExteriorRing().getCoordinateN(0).x);
//Logger.debug("\ty=" + polygonArray[0].getExteriorRing().getCoordinateN(0).y);
		MultiPolygon mpg = new MultiPolygon(polygonArray, fact);
		
		for (MultiPolygon mp : multipolygons){
			mpg = (MultiPolygon)mpg.union(mp);
		}
		return mpg;
	}

	public static void processGeometryTypes(GeometryFactory fact,
			List<Polygon> polygons, Feature feature, FeatureGeometry geometry) {
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
