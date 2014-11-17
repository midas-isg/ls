package models.geo;

import java.util.HashMap;
import java.util.Map;

public class Feature {
	public String type = Feature.class.getSimpleName();
	public FeatureGeometry geometry;
	public Map<String, String> properties = new HashMap<>();
	
	@Override
	public String toString() {
		return "Feature [type=" + type + ", geometry=" + geometry
				+ ", properties=" + properties + "]";
	}
}
