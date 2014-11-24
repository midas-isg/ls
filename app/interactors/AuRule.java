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
		String parentName = getParentName(au);
		String des = gid + ": " + name + " " + start + " to " + end
				+ " parent=" + parentName;
		
		properties.put("description", des);
		
		return properties;
	}

	private static String getParentName(AdministrativeUnit au) {
		AdministrativeUnit parent = au.getParent();
		String parentName = (parent == null) 
				? "null" 
				: String.valueOf(parent.getData().getName());
		return parentName;
	}
	

	public static Long create(FeatureCollection fc){
		AdministrativeUnit au = toAu(fc);
		AuDao dao = new AuDao();
		return dao.create(au);
	}
	
	public static void delete(long gid){
		AuDao dao = new AuDao();
		dao.delete(dao.read(gid));
	}

	private static AdministrativeUnit toAu(FeatureCollection fc){
		AdministrativeUnit c = new AdministrativeUnit();
		Data data = new Data();
		data.setMultiPolygonGeom(GeoInputRule.toMultiPolygon(fc));
		String name = getString(fc, "name");
		data.setName(name);
		String date = getString(fc, "startDate");
		Date startDate = newDate(date);
		data.setStartDate(startDate);
		data.setUpdateDate(startDate);
		data.setCode(getString(fc, "code"));
		c.setData(data);
		String parentGid = getString(fc, "parent");
		AdministrativeUnit parent = findByGid(Long.parseLong(parentGid));
		if (parent == null){
			throw new RuntimeException("Cannot find parent gid=" + parentGid);
		}
		c.setParent(parent);
		return c;
	}

	private static Date newDate(String date) {
		return java.sql.Date.valueOf(date);
	}
	
	private static String getString(FeatureCollection fc, String key) {
		return fc.getFeatures().get(0).getProperties().get(key);
	}

	public static FeatureCollection getFeatureCollection(long gid) {
		AdministrativeUnit au = findByGid(gid);
		List<AdministrativeUnit> list = new ArrayList<>();
		list.add(au);
		return toFeatureCollection(list);
	}

	private static AdministrativeUnit findByGid(long gid) {
		AdministrativeUnit au = new AuDao().read(gid);
		return au;
	}

}
