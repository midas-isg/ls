package interactors;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import models.geo.Feature;
import models.geo.FeatureCollection;
import dao.CountyDAO;
import dao.entities.County;

public class CountyRule {
	public static FeatureCollection toFeatureCollection(List<County> counties) {
		FeatureCollection fc = new FeatureCollection();
		fc.setFeatures(toFeatures(counties));
		//fc.id = ;
		fc.setType("FeatureCollection");
		
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
		feature.setProperties(toProperties(county));
		feature.setGeometry(GeoOutputRule.toFeatureGeometry(county.geom));
		
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
	
	public static Long save(FeatureCollection fc){
		County c = toCounty(fc);
		CountyDAO dao = new CountyDAO();
		return dao.save(c);
	}

	private static County toCounty(FeatureCollection fc){
		County c = new County();
		c.statefp = "1";
		c.geoid = "1";
		c.name = extractName(fc);
		c.geom = GeoInputRule.toMultiPolygon(fc);
		return c;
	}
	
	private static String extractName(FeatureCollection fc) {
		return fc.getFeatures().get(0).getProperties().get("name");
	}
}
