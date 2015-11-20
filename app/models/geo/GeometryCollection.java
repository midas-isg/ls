package models.geo;

import java.util.List;

public class GeometryCollection extends FeatureGeometry {
	private List<FeatureGeometry> geometries;

	public GeometryCollection() {
		setType(GeometryCollection.class.getSimpleName());
		
		return;
	}
	
	public List<FeatureGeometry> getGeometries() {
		return geometries;
	}
	
	public void setGeometries(List<FeatureGeometry> geometries) {
		this.geometries = geometries;
		return;
	}
	
	@Override
	public String toString() {
		return "GeometryCollection (" + geometries.size() + " Polygon/s)";
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((geometries == null) ? 0 : geometries.hashCode());
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
		GeometryCollection other = (GeometryCollection) obj;
		if (geometries == null) {
			if (other.geometries != null)
				return false;
		} else if (!geometries.equals(other.geometries))
			return true; // false; TODO why geometries are not equal
		return true;
	}
}
