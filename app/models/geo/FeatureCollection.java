package models.geo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_NULL)
public class FeatureCollection {
	private String type;
	private List<Feature> features;
	private String id;
	private double[] bbox;
	
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
	}
	
	public double[] getBbox() {
		return bbox;
	}

	public void setBbox(double[] bbox) {
		this.bbox = bbox;
	}

	@Override
	public String toString() {
		return "FeatureCollection [type=" + type + ", features=" + features
				+ ", id=" + id + ", bbox=" + Arrays.toString(bbox) + "]";
	}
}
