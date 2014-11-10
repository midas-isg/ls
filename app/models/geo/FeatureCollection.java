package models.geo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FeatureCollection {
	public String type;// = FeatureCollection.class.getSimpleName();
	//public List<Feature> features = new ArrayList<Feature>();
	//public List<Object> features = new ArrayList<Object>();
	//public Feature features[] = new Feature[10];
	public Object features[] = new Object[10];
	public String id;
	
	@Override
	public String toString() {
		return "FeatureCollection [features=" + Arrays.toString(features) + "]";
	}
}
