package models.geo;

import java.util.ArrayList;
import java.util.List;

public class FeatureCollection {
	public String type = FeatureCollection.class.getSimpleName();
	public List<Feature> features = new ArrayList<>(); 
}
