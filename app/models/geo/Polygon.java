package models.geo;

import java.util.List;

public class Polygon extends FeatureGeometry {
	/** list of lists because polygons can have 'holes' **/
	private List<List<double[]>> coordinates;
	
	public Polygon() {
		setType(Polygon.class.getSimpleName());
	}
	
	public List<List<double[]>> getCoordinates() {
		return coordinates;
	}

	public void setCoordinates(List<List<double[]>> coordinates) {
		this.coordinates = coordinates;
	}
}
