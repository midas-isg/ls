package models.geo;

import java.util.ArrayList;
import java.util.List;

public class Polygon extends FeatureGeometry {
	private List<double[]> coordinates;
	//private List<Point> coordinates = new ArrayList<Point>();
	
	public Polygon() {
		setType(Polygon.class.getSimpleName());
		coordinates = new ArrayList<double[]>();
		
		return;
	}
	
	public List<double[]> getCoordinates() {
		return coordinates;
	}

	public void setCoordinates(List<double[]> coordinates) {
		this.coordinates = coordinates;
	}
}
