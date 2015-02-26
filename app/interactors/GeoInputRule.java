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
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;

public class GeoInputRule {
	static Geometry toMultiPolygon(FeatureCollection fc) {
		GeometryFactory fact = new GeometryFactory();
		List<Feature> features = fc.getFeatures();
		List<Polygon> polygons = new ArrayList<Polygon>();
		
		for(Feature feature :features) {
			FeatureGeometry geometry = feature.getGeometry();
			
			if(geometry.getType().equals("MultiPolygon")) {
				models.geo.MultiPolygon multipolygon = (models.geo.MultiPolygon)geometry;
				List<List<List<double[]>>> polygonCoordinatesCollection = multipolygon.getCoordinates();
				int polygonCount = polygonCoordinatesCollection.size();
				
				for(int j = 0; j < polygonCount; j++) {
					Feature polygonFeature = new Feature();
					models.geo.Polygon polygonBody = new models.geo.Polygon();
					polygonBody.setCoordinates(polygonCoordinatesCollection.get(j));
					polygonBody.setType("Polygon");
					polygonFeature.setType("Polygon");
					polygonFeature.setGeometry(polygonBody);
					
					Polygon polygon = toPolygon(fact, polygonFeature);
					polygons.add(polygon);
				}
			}
			else if(geometry.getType().equals("Polygon")) {
				Feature polygonFeature = new Feature();
				polygonFeature.setType("Polygon");
				polygonFeature.setGeometry(geometry);
				
				Polygon polygon = toPolygon(fact, polygonFeature);
				polygons.add(polygon);
			}
		}
		
		Polygon [] polygonArray = polygons.toArray(new Polygon[polygons.size()]);
		MultiPolygon mpg = new MultiPolygon(polygonArray, fact);
		
		return mpg;
	}

	private static Polygon toPolygon(GeometryFactory fact, Feature feature) {
		Coordinate[] coordinates = toCoordinates(feature);
		Polygon poly = fact.createPolygon(coordinates);
		
		return poly;
	}

	private static Coordinate[] toCoordinates(Feature feature) {
		FeatureGeometry fg = feature.getGeometry();
		String type = fg.getType();
		List<Coordinate> coordinates = null;
		
		if(type.equals("MultiPolygon")) {
			models.geo.MultiPolygon mp = (models.geo.MultiPolygon)fg;
			List<List<double[]>> p = mp.getCoordinates().get(0);
			coordinates = new ArrayList<Coordinate>();
			
			for(List<double[]> line : p) {
				for(int i = 0; i < line.size(); i++){
					double[] point = line.get(i);
					
					coordinates.add(new Coordinate());
					Coordinate coordinateToSet = coordinates.get(i);
					for(int j = 0; j < point.length; j++) {
						coordinateToSet.setOrdinate(j, point[j]);
					}
				}
			}
		}
		else if(type.equals("Polygon")) {
			models.geo.Polygon polygon = (models.geo.Polygon)fg;
			List<List<double[]>> pointCollection = polygon.getCoordinates();
			int polygonComponentCount = pointCollection.size();
			
			coordinates = new ArrayList<Coordinate>();
			for(int i = 0; i < polygonComponentCount; i++) {
				for(int j = 0; j < pointCollection.get(i).size(); j++) {
					double [] point = pointCollection.get(i).get(j);
					
					coordinates.add(new Coordinate());
					Coordinate coordinateToSet = coordinates.get(j);
					for(int k = 0; k < 2/*point.length*/; k++) {
						coordinateToSet.setOrdinate(k, point[k]);
					}
				}
			}
		}
		else {
			throw new RuntimeException(type + " Geometry is not supported.");
		}
		
		Coordinate [] output = null;
		
		if(coordinates != null) {
			output = coordinates.toArray(new Coordinate[coordinates.size()]);
		}
		
		return output;
	}
}
