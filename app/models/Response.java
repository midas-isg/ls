package models;

import java.util.Map;

import models.geo.FeatureCollection;

public class Response {
	private FeatureCollection geoJSON;
	private Map<String, Object> properties;
	
	public FeatureCollection getGeoJSON() {
		return geoJSON;
	}
	
	public void setGeoJSON(FeatureCollection geoJSON) {
		this.geoJSON = geoJSON;
	}
	
	public Map<String, Object> getProperties() {
		return properties;
	}
	
	public void setProperties(Map<String, Object> properties) {
		this.properties = properties;
	}

	@Override
	public String toString() {
		return "Response [geoJSON=" + geoJSON + ", properties=" + properties
				+ "]";
	}
}
