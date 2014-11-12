package interactors;

import java.util.ArrayList;
import java.util.List;

import models.geo.FeatureGeometry;
import models.geo.MultiPolygon;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;

public class GeoRule {
	static FeatureGeometry toFeatureGeometry(Geometry geom) {
		List<List<List<double[]>>> list = new ArrayList<>();
		list.add(toBodyArray(geom));
		MultiPolygon g = new MultiPolygon();
		g.coordinates = list;
		
		return g; 
	}

	private static List<List<double[]>> toBodyArray(Geometry geom) {
		List<List<double[]>> coordinates = new ArrayList<>();
		List<double[]> coordinate = toBody(geom.getCoordinates());
		coordinates.add(coordinate);
		
		return coordinates;
	}

	private static List<double[]> toBody(Coordinate[] coordinates) {
		List<double[]> result = new ArrayList<>();
		for (Coordinate coordinate1: coordinates){
			result.add(toPoint(coordinate1));
		}
		
		return result;
	}

	private static double[] toPoint(Coordinate coordinate1) {
		double[] coordinate = new double[2];//[3];
		coordinate[0] = coordinate1.x;
		coordinate[1] = coordinate1.y;
		//coordinate[2] = coordinate1.z;
		
		return coordinate;
	}
}
