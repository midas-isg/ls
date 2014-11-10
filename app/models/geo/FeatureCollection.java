package models.geo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

public class FeatureCollection {
	public String type;// = FeatureCollection.class.getSimpleName();
	//public List<Feature> features = new ArrayList<Feature>();
	public JsonNode features;
	public String id;
	
	@Override
	public String toString() {
		return "FeatureCollection [id=" + id + ", type=" + type + ", features=" + features.toString() + "]";
	}
}
