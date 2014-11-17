package models.geo;

public class Point extends FeatureGeometry {
	private double[] coordinates;
	
	public Point() {
		setType(Point.class.getSimpleName());
		coordinates = new double[2];
		
		return;
	}
	
	public double[] getCoordinates() {
		return coordinates;
	}
	
	public double getLatitude() {
		return coordinates[1];
	}
	
	public double getLongitude() {
		return coordinates[0];
	}

	public void setCoordinates(double[] coordinates) {
		this.coordinates = coordinates;
	}
	
	public void setLatitude(double coordinate) {
		this.coordinates[1] = coordinate;
	}
	
	public void setLongitude(double coordinate) {
		this.coordinates[0] = coordinate;
	}
}
