package models.geo;

import java.util.HashMap;
import java.util.Map;


public class Feature {
	public String type = Feature.class.getSimpleName();
	public FeatureGeometry geometry;
	public Map<String, String> properties = new HashMap<>();
}
