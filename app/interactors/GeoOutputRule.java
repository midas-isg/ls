package interactors;

import java.util.ArrayList;
import java.util.List;

import models.geo.FeatureGeometry;
import models.geo.MultiPolygon;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;

public class GeoOutputRule {
	static FeatureGeometry toFeatureGeometry(Geometry geom) {
		List<List<List<double[]>>> list = new ArrayList<>();
		list.add(toMultipolygonCoordinates(geom));
		MultiPolygon g = new MultiPolygon();
		g.setCoordinates(list);
		
		return g; 
	}

	private static List<List<double[]>> toMultipolygonCoordinates(Geometry geom) {
		List<List<double[]>> multipolygon = new ArrayList<>();
		if (geom == null)
			return multipolygon;
		int n = geom.getNumGeometries();
		for (int i = 0; i < n; i++){
			Geometry polygonGeo = geom.getGeometryN(i);
			List<double[]> polygon = toPolygonCoordinates(polygonGeo.getCoordinates());
			multipolygon.add(polygon);
		}
		
		return multipolygon;
	}

	private static List<double[]> toPolygonCoordinates(Coordinate[] coordinates) {
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