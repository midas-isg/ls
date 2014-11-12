package interactors;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import models.geo.Feature;
import models.geo.FeatureCollection;
import dao.entities.County;

public class CountyRule {
	public static FeatureCollection toFeatureCollection(List<County> counties) {
		FeatureCollection fc = new FeatureCollection();
		fc.features = toFeatures(counties);
		return fc;
	}

	private static List<Feature> toFeatures(List<County> counties) {
		List<Feature> features = new ArrayList<>();
    	for (County county : counties) {
    		features.add(toFeature(county));
    	}
    	return features;
	}

	private static Feature toFeature(County county) {
		Feature feature = new Feature();
		feature.properties = toProperties(county);
		feature.geometry = GeoRule.toFeatureGeometry(county.geom);
		return feature;
	}


	private static Map<String, String> toProperties(County county) {
		Map<String, String> properties = new HashMap<>();
		String name = county.namelsad;
		properties.put("description", county.name + " in statefp="+ county.statefp);
		properties.put("id", county.geoid);
		properties.put("title", name);
		return properties;
	}


}
