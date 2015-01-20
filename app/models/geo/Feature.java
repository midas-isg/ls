package models.geo;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_NULL)
public class Feature {
	private String type;
	private FeatureGeometry geometry;
	private Map<String, Object> properties;
	
	public Feature() {
		type = Feature.class.getSimpleName();
		properties = new HashMap<>();
		
		return;
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

	@Override
	public String toString() {
		return "Feature [type=" + type + ", geometry=" + geometry
				+ ", properties=" + properties + "]";
	}
}
