package models.geo;

import java.util.ArrayList;
import java.util.List;

public class MultiPolygon extends FeatureGeometry {
	private List<List<List<double []>>> coordinates;
	//public List<Polygon> coordinates = new ArrayList<Polygon>();

	public MultiPolygon() {
		setType(MultiPolygon.class.getSimpleName());
		coordinates = new ArrayList<>();
		
		return;
	}
	
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
