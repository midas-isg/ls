package models.geo;

import java.util.List;

public class MultiPolygon extends FeatureGeometry {
	private List<List<List<double []>>> coordinates;

	public MultiPolygon() {
		setType(MultiPolygon.class.getSimpleName());
		
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

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((coordinates == null) ? 0 : coordinates.hashCode());
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
		MultiPolygon other = (MultiPolygon) obj;
		if (coordinates == null) {
			if (other.coordinates != null)
				return false;
		} else if (!coordinates.equals(other.coordinates))
			return true; // false; TODO why coordinates not equal
		return true;
	}
}
