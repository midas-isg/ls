package interactors;


import java.sql.Date;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import models.geo.Feature;
import models.geo.FeatureCollection;
import play.Logger;

import com.vividsolutions.jts.geom.Geometry;

import dao.AuDao;
import dao.entities.Code;
import dao.entities.CodeType;
import dao.entities.Data;
import dao.entities.GisSource;
import dao.entities.Location;
import dao.entities.LocationGeometry;
import dao.entities.LocationType;

public class LocationRule {
	public static final long EPIDEMIC_ZONE_ID = 7L;
	public static final long ISG_CODE_TYPE_ID = 2L;
	
	private static LocationType epidemicZoneLocationType = null;
	private static CodeType isgCodeType = null;
	private static GisSource alsGisSource = null;
	
	private static LocationType getEpidemicZoneLocationType(){
		if (epidemicZoneLocationType == null){
			epidemicZoneLocationType = new LocationType();
			epidemicZoneLocationType.setId(EPIDEMIC_ZONE_ID);
			epidemicZoneLocationType.setName("Epidemic Zone");
		}
		return epidemicZoneLocationType;
	}

	private static CodeType getIsgCodeType(){
		if (isgCodeType == null){
			isgCodeType = new CodeType();
			isgCodeType.setId(ISG_CODE_TYPE_ID);
			isgCodeType.setName("ISG");
		}
		return isgCodeType;
	}

	private static GisSource getAlsGisSource(){
		if (alsGisSource == null){
			alsGisSource = new GisSource();
			alsGisSource.setId(8L);
			alsGisSource.setUrl("ALS");
		}
		return alsGisSource;
	}

	public static FeatureCollection toFeatureCollection(List<Location> aus) {
		FeatureCollection fc = new FeatureCollection();
		fc.setFeatures(toFeatures(aus));
		fc.setType("FeatureCollection");
		
		return fc;
	}

	@SuppressWarnings("unused")
	private static FeatureCollection toFeatureCollection(Location compositeAu) {
		FeatureCollection fc = new FeatureCollection();
		fc.setFeatures(toFeatures(compositeAu.getLocationsIncluded()));
		fc.setType("FeatureCollection");
		
		return fc;
	}

	private static List<Feature> toFeatures(List<Location> aus) {
		List<Feature> features = new ArrayList<>();
		for (Location au : aus) {
			if (au == null){
				Logger.warn("toFeatures got an element in the list as null.");
				continue;
			}
			features.add(toFeature(au));
		}
		
		return features;
	}

	private static Feature toFeature(Location au) {
		Feature feature = new Feature();
		Map<String, Object> properties = toProperties(au);
		Collections.sort(au.getChildren());
		putAsLocationObjectsIfNotNull(properties, "children", au.getChildren());
		putAsLocationObjectsIfNotNull(properties, "lineage", AuHierarchyRule.getLineage(au.getGid()));
		putAsLocationObjectsIfNotNull(properties, "related", au.getRelatedLocations());
		putAsCodeObjectsIfNotNull(properties, "codes", au);
		feature.setProperties(properties);
		LocationGeometry geometry = au.getGeometry();
		if (geometry != null){
			Geometry multiPolygonGeom = geometry.getMultiPolygonGeom();
			feature.setGeometry(GeoOutputRule.toFeatureGeometry(multiPolygonGeom));
		}
		
		return feature;
	}

	private static void putAsCodeObjectsIfNotNull(Map<String, Object> properties,
			String string, Location location) {
		if (location == null)
			return;
		
		List<Map<String, String>> codes = new ArrayList<>();
		Map<String, String> code = new HashMap<>();
		final String KEY_CODE = "code";
		final String KEY_TYPE = "codeTypeName";
		code.put(KEY_CODE, location.getData().getCode());
		code.put(KEY_TYPE, location.getData().getCodeType().getName());
		codes.add(code);
		properties.put(string, codes);
		
		List<Code> otherCodes = location.getOtherCodes();
		if (otherCodes == null)
			return;
		for (Code c : otherCodes){
			Map<String, String> anotherCode = new HashMap<>();
			anotherCode.put(KEY_CODE, c.getCode());
			anotherCode.put(KEY_TYPE, c.getCodeType().getName());
			codes.add(anotherCode);
		}
	}

	private static void putAsLocationObjectsIfNotNull(Map<String, Object> properties, String key, List<Location> locations) {
		if (locations == null)
			return;
		List<Map<String, Object>> list = new ArrayList<>();
		for (Location l : locations){
			list.add(toProperties(l));
		}
		properties.put(key, list);
	}

	private static Map<String, Object> toProperties(Location au) {
		Map<String, Object> properties = new HashMap<>();
		String gid = getGid(au);
		putAsStringIfNotNull(properties, "gid", gid);
		Data data = au.getData();
		if (data == null){
			Logger.warn(gid + " has null data!");
			return properties;
		}
		putAsStringIfNotNull(properties, "name", data.getName());
		putAsStringIfNotNull(properties, "description", data.getDescription());
		putAsStringIfNotNull(properties, "headline", au.getHeadline());
		putAsStringIfNotNull(properties, "rank", au.getRank());
		/*putAsStringIfNotNull(properties, "code", data.getCode());
		String codeTypeName = getName(data.getCodeType(), data.getCodeType().getName());
		putAsStringIfNotNull(properties, "codeTypeName", codeTypeName);*/
		putAsStringIfNotNull(properties, "startDate", data.getStartDate());
		putAsStringIfNotNull(properties, "endDate", data.getEndDate());
		String locationTypeName = getLocationTypeName(data.getLocationType());
		putAsStringIfNotNull(properties, "locationTypeName", locationTypeName);
		Location parent = au.getParent();
		putAsStringIfNotNull(properties, "parentGid", getGid(parent));
		return properties;
	}

	private static String getLocationTypeName(LocationType locationType) {
		return (locationType == null) ? null : locationType.getName();
	}

	private static String toString(Object object) {
		if (object == null)
			return null;
		return String.valueOf(object);
	}

	private static String getGid(Location parent) {
		if (parent == null)
			return null;
		return String.valueOf(parent.getGid());
	}

	private static void putAsStringIfNotNull(Map<String, Object> properties,
			String key, Object value) {
		if (value == null)
			return;
		properties.put(key, toString(value));
	}

	public static Long create(FeatureCollection fc){
		Location au = toAu(fc);
		AuDao dao = new AuDao();
		return dao.create(au);
	}
	
	public static void delete(long gid){
		AuDao dao = new AuDao();
		dao.delete(dao.read(gid));
	}

	private static Location toAu(FeatureCollection fc){
		Location au = new Location();
		Data data = new Data();
		data.setLocationType(getEpidemicZoneLocationType());
		data.setCodeType(getIsgCodeType());
		data.setGisSource(getAlsGisSource());
		au.setGeometry(createLocationGeometry(fc, au));
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
		Location parent = findByGid(Long.parseLong(parentGid));
		if (parent == null){
			throw new RuntimeException("Cannot find parent gid=" + parentGid);
		}
		au.setParent(parent);
		return au;
	}

	private static LocationGeometry createLocationGeometry(FeatureCollection fc, Location l) {
		LocationGeometry lg = new LocationGeometry();
		lg.setMultiPolygonGeom(GeoInputRule.toMultiPolygon(fc));
		lg.setLocation(l);
		return lg;
	}

	private static Date getNowDate() {
		java.util.Date now = new java.util.Date();
		return new Date(now.getTime());
	}

	private static Date newDate(String date) {
		return java.sql.Date.valueOf(date);
	}
	
	private static String getString(FeatureCollection fc, String key) {
		Object object = fc.getFeatures().get(0).getProperties().get(key);
		if (object == null)
			return null;
		return object.toString();
	}

	public static FeatureCollection getFeatureCollection(long gid) {
		Location au = findByGid(gid);
		List<Location> list = new ArrayList<>();
		list.add(au);
		return toFeatureCollection(list);
	}

	public static Location findByGid(long gid) {
		Location au = new AuDao().read(gid);
		return au;
	}
	
	public static Map<String, Object> findByName(String q, Integer limit, Integer offset){
		List<Location> result = new AuDao().findByName(q, limit, offset);
		Map<String, Object> map = new HashMap<>();
		map.put("geoJSON", toFeatureCollection(result));
		Map<String, Object> properties = new HashMap<>();
		map.put("properties", properties);
		properties.put("q", q);
		putAsStringIfNotNull(properties, "limit", limit);
		putAsStringIfNotNull(properties, "offset", offset);
		putAsStringIfNotNull(properties, "resultSize", "" + result.size());
		properties.put("locationTypeName", "Result from a query");
		String descritpion = "Result from the query for '" + q + "' limit=" + limit + " offset=" + offset;
		properties.put("description", descritpion);
		return map;
	}
}
