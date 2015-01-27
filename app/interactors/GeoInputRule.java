package interactors;

import java.util.List;

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
		Polygon[] polygons = new Polygon[features.size()];
		int i = 0;
		for (Feature feature :features){
			Polygon poly = toPolygon(fact, feature);
			polygons[i] = poly;
			i++;
		}
		MultiPolygon mpg = new MultiPolygon(polygons, fact);
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
		if ( ! type.equals("MultiPolygon"))
			throw new RuntimeException(type + " Geometry is not supported.");
		models.geo.MultiPolygon mp = (models.geo.MultiPolygon)fg;
		List<List<double[]>> p = mp.getCoordinates().get(0);
		for (List<double[]> line : p){
			Coordinate[] coordinates = new Coordinate[line.size()];
			for (int i = 0; i < line.size(); i++){
				double[] point = line.get(i);
				coordinates[i] = new Coordinate(point[0], point[1]);
			}
			return coordinates;
		}
		return null;
	}
}
