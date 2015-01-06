package interactors;


import java.sql.Date;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.vividsolutions.jts.geom.Geometry;

import models.geo.Feature;
import models.geo.FeatureCollection;
import dao.AuDao;
import dao.entities.AdministrativeUnit;
import dao.entities.Data;

public class AuRule {
	public static final long EPIDEMIC_ZONE_ID = 7L;
	public static final long ISG_CODE_TYPE_ID = 2L;

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
		//TODO Geometry multiPolygonGeom = au.getData().getGeometry();
		//TODO feature.setGeometry(GeoOutputRule.toFeatureGeometry(multiPolygonGeom));
		
		return feature;
	}

	private static Map<String, String> toProperties(AdministrativeUnit au) {
		Map<String, String> properties = new HashMap<>();
		putAsStringIfNotNull(properties, "gid", getGid(au));
		Data data = au.getData();
		putAsStringIfNotNull(properties, "name", data.getName());
		putAsStringIfNotNull(properties, "code", data.getCode());
		putAsStringIfNotNull(properties, "codePath", data.getCodePath());
		putAsStringIfNotNull(properties, "startDate", data.getStartDate());
		putAsStringIfNotNull(properties, "endDate", data.getEndDate());
		AdministrativeUnit parent = au.getParent();
		putAsStringIfNotNull(properties, "parentGid", getGid(parent));
		return properties;
	}

	private static String toString(Object object) {
		if (object == null)
			return null;
		return String.valueOf(object);
	}

	private static String getGid(AdministrativeUnit parent) {
		if (parent == null)
			return null;
		return String.valueOf(parent.getGid());
	}

	private static void putAsStringIfNotNull(Map<String, String> properties,
			String key, Object value) {
		if (value == null)
			return;
		properties.put(key, toString(value));
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
		AdministrativeUnit au = new AdministrativeUnit();
		Data data = new Data();
		//TODO data.setAuTypeId(EPIDEMIC_ZONE_ID);
		//TODO data.setCodeTypeId(ISG_CODE_TYPE_ID);
		//TODO data.setGisSource("LS");
		//TODO data.setGeometry(GeoInputRule.toMultiPolygon(fc));
		String name = getString(fc, "name");
		data.setName(name);
		String date = getString(fc, "startDate");
		Date startDate = newDate(date);
		data.setStartDate(startDate);
		data.setUpdateDate(getNowDate());
		String code = getString(fc, "code");
		data.setCode(code);
		au.setData(data);
		String parentGid = getString(fc, "parent");
		AdministrativeUnit parent = findByGid(Long.parseLong(parentGid));
		if (parent == null){
			throw new RuntimeException("Cannot find parent gid=" + parentGid);
		}
		au.setParent(parent);
		data.setCodePath(parent.getData().getCodePath() + "." + code);
		return au;
	}

	private static Date getNowDate() {
		java.util.Date now = new java.util.Date();
		return new Date(now.getTime());
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

	public static AdministrativeUnit findByGid(long gid) {
		AdministrativeUnit au = new AuDao().read(gid);
		return au;
	}

	public static List<AdministrativeUnit> getHierarchy() {
		return new AuDao().findRoots();
	}

}
