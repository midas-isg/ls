package models.geo;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

//@JsonInclude(Include.NON_NULL)
public class Feature {
	private String type;
	private FeatureGeometry geometry;
	private Map<String, Object> properties;
	private String id;
	private double[] bbox;
	private double[] repPoint;

	public Feature() {
		type = Feature.class.getSimpleName();
		properties = new HashMap<>();
	}
	
	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public FeatureGeometry getGeometry() {
		return geometry;
	}

	public void setGeometry(FeatureGeometry geometry) {
		this.geometry = geometry;
	}

	public Map<String, Object> getProperties() {
		return properties;
	}

	public void setProperties(Map<String, Object> properties) {
		this.properties = properties;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public double[] getBbox() {
		return bbox;
	}

	public void setBbox(double[] bbox) {
		this.bbox = bbox;
	}

	public double[] getRepPoint() {
		return repPoint;
	}

	public void setRepPoint(double[] repPoint) {
		this.repPoint = repPoint;
	}

	@Override
	public String toString() {
		return "Feature [type=" + type + ", geometry=" + geometry
				+ ", properties=" + properties + ", id=" + id + ", bbox="
				+ Arrays.toString(bbox) + ", rep_point="
				+ Arrays.toString(repPoint) + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((geometry == null) ? 0 : geometry.hashCode());
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result
				+ ((properties == null) ? 0 : properties.hashCode());
		result = prime * result + ((type == null) ? 0 : type.hashCode());
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
		Feature other = (Feature) obj;
		if (geometry == null) {
			if (other.geometry != null)
				return false;
		} else if (!geometry.equals(other.geometry))
			return false;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (properties == null) {
			if (other.properties != null)
				return false;
		} else if (!properties.equals(other.properties))
			return false;
		if (type == null) {
			if (other.type != null)
				return false;
		} else if (!type.equals(other.type))
			return false;
		return true;
	}
}
