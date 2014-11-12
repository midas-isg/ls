package models.geo;

import java.util.ArrayList;
import java.util.List;

public class MultiPolygon extends FeatureGeometry {
	public List<List<List<double []>>> coordinates = new ArrayList<>();
	//public List<Polygon> coordinates = new ArrayList<Polygon>();

	public List<List<List<double []>>> getCoordinates() {
		return coordinates;
	}

	public void setCoordinates(List<List<List<double []>>> coordinates) {
		this.coordinates = coordinates;
	}
	
	@Override
	public String toString() {
		return "MultiPolygon (" + coordinates.size() + " Polygon/s)";
	}
}
