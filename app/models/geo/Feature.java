package models.geo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class Feature {
	public String type;// = Feature.class.getSimpleName();
	public FeatureGeometry geometry;
	public Map<String, String> properties = new HashMap<>();
}
