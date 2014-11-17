package models.geo;

import java.util.ArrayList;
import java.util.List;

public class Polygon extends FeatureGeometry {
	/* list of lists because polygons can have 'holes' */
	private List<List<double[]>> coordinates;
	//private List<List<Point>> coordinates = new ArrayList<>();
	
	public Polygon() {
		setType(Polygon.class.getSimpleName());
		
		return;
	}
	
	public List<List<double[]>> getCoordinates() {
		return coordinates;
	}

	public void setCoordinates(List<List<double[]>> coordinates) {
		this.coordinates = coordinates;
	}
}
