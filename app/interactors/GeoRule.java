package interactors;

import java.util.ArrayList;
import java.util.List;

import models.geo.FeatureGeometry;
import models.geo.MultiPolygon;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;

public class GeoRule {

	static FeatureGeometry toFeatureGeometry(Geometry geom) {
		//List<List<List<double[]>>> list = new ArrayList<>();
		List<List<List<Double>>> list = new ArrayList<>();
		list.add(toCoordinates(geom));
		MultiPolygon g = new MultiPolygon();
		g.coordinates = list;
		return g;
	}

	//private static List<List<double[]>> toCoordinates(Geometry geom)
	private static List<List<Double>> toCoordinates(Geometry geom) {
		//List<List<double[]>> coordinates = new ArrayList<>();
		List<List<Double>> coordinates = new ArrayList<>();
		//List<double[]> coordinate = toCondinates(geom.getCoordinates());
		List<Double> coordinate = toCondinates(geom.getCoordinates());
		coordinates.add(coordinate);
		
		return coordinates;
	}

	//private static List<double[]> toCondinates(Coordinate[] coordinates)
	private static List<Double> toCondinates(Coordinate[] coordinates) {
		//List<double[]> result = new ArrayList<>();
		List<Double> result = new ArrayList<Double>();
		for (Coordinate coordinate1: coordinates){
			//result.add(toCoordinate(coordinate1));
			result.set(0, toCoordinate(coordinate1)[0]);
			result.set(1, toCoordinate(coordinate1)[1]);
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
