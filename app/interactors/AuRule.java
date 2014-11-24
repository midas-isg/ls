package interactors;


import java.sql.Date;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import models.geo.Feature;
import models.geo.FeatureCollection;
import dao.AuDao;
import dao.entities.AdministrativeUnit;
import dao.entities.Data;

public class AuRule {
	public static FeatureCollection toFeatureCollection(List<AdministrativeUnit> aus) {
		FeatureCollection fc = new FeatureCollection();
		fc.setFeatures(toFeatures(aus));
		fc.setType("FeatureCollection");
		
		return fc;
	}

	private static List<Feature> toFeatures(List<AdministrativeUnit> aus) {
		List<Feature> features = new ArrayList<>();
		for (AdministrativeUnit au : aus) {
			features.add(toFeature(au));
		}
		
		return features;
	}

	private static Feature toFeature(AdministrativeUnit au) {
		Feature feature = new Feature();
		feature.setProperties(toProperties(au));
		feature.setGeometry(GeoOutputRule.toFeatureGeometry(au.getData().getMultiPolygonGeom()));
		
		return feature;
	}

	private static Map<String, String> toProperties(AdministrativeUnit au) {
		Map<String, String> properties = new HashMap<>();
		properties.put("title",  au.getData().getCode());
		String gid = String.valueOf(au.getGid());
		properties.put("id", gid);
		String name = au.getData().getName();
		String start = String.valueOf(au.getData().getStartDate());
		String end = String.valueOf(au.getData().getEndDate());
		String des = gid + ": " + name + " " + start + " to " + end;
		properties.put("description", des);
		
		return properties;
	}
	

	public static Long save(FeatureCollection fc){
		AdministrativeUnit au = toAu(fc);
		AuDao dao = new AuDao();
		return dao.save(au);
	}
	
	private static AdministrativeUnit toAu(FeatureCollection fc){
		AdministrativeUnit c = new AdministrativeUnit();
		Data data = new Data();
		data.setMultiPolygonGeom(GeoInputRule.toMultiPolygon(fc));
		String name = extractName(fc, "name");
		data.setName(name);
		String date = extractName(fc, "startDate");
		Date startDate = newDate(date);
		data.setStartDate(startDate);
		data.setUpdateDate(startDate);
		data.setCode(extractName(fc, "code"));
		c.setData(data);
		return c;
	}

	private static Date newDate(String date) {
		return java.sql.Date.valueOf(date);
	}
	
	private static String extractName(FeatureCollection fc, String key) {
		return fc.getFeatures().get(0).getProperties().get(key);
	}

	public static FeatureCollection findByGid(long gid) {
		AdministrativeUnit au = new AuDao().findByGid(gid);
		List<AdministrativeUnit> list = new ArrayList<>();
		list.add(au);
		return toFeatureCollection(list);
	}

}
