package models.geo;

import java.util.Arrays;

public class Point extends FeatureGeometry {
	private double[] coordinates;
	
	public Point() {
		setType(Point.class.getSimpleName());
		
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

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(coordinates);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Point other = (Point) obj;
		if (!Arrays.equals(coordinates, other.coordinates))
			return false;
		return true;
	}
}
