package models.geo;

import java.util.ArrayList;
import java.util.List;

public class FeatureCollection {
	private String type;
	private List<Feature> features;
	private String id;
	
	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public List<Feature> getFeatures() {
		return features;
	}

	public void setFeatures(List<Feature> features) {
		this.features = features;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}
	
	public FeatureCollection() {
		type = FeatureCollection.class.getSimpleName();
		features = new ArrayList<Feature>();
		
		return;
	}
	
	@Override
	public String toString() {
		return "FeatureCollection [id=" + id + ", type=" + type + ", " +
				features.size() + " features=" + features.toString() + "]";
	}
}
