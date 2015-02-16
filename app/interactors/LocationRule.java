package interactors;


import java.util.List;

import models.geo.FeatureCollection;
import dao.LocationDao;
import dao.entities.CodeType;
import dao.entities.GisSource;
import dao.entities.Location;
import dao.entities.LocationType;

public class LocationRule {
	public static final long PUMA_TYPE_ID = 102L;
	public static final long COMPOSITE_LOCATION_ID = 8L;

	
	public static final long EPIDEMIC_ZONE_ID = 7L;
	public static final long ISG_CODE_TYPE_ID = 2L;
	
	private static LocationType epidemicZoneLocationType = null;
	private static CodeType isgCodeType = null;
	private static GisSource alsGisSource = null;

	static LocationType getEpidemicZoneLocationType(){
		if (epidemicZoneLocationType == null){
			epidemicZoneLocationType = new LocationType();
			epidemicZoneLocationType.setId(EPIDEMIC_ZONE_ID);
			epidemicZoneLocationType.setName("Epidemic Zone");
		}
		return epidemicZoneLocationType;
	}

	static CodeType getIsgCodeType(){
		if (isgCodeType == null){
			isgCodeType = new CodeType();
			isgCodeType.setId(ISG_CODE_TYPE_ID);
			isgCodeType.setName("ISG");
		}
		return isgCodeType;
	}

	static GisSource getAlsGisSource(){
		if (alsGisSource == null){
			alsGisSource = new GisSource();
			alsGisSource.setId(8L);
			alsGisSource.setUrl("ALS");
		}
		return alsGisSource;
	}

	public static Long create(FeatureCollection fc){
		Location location = GeoJsonRule.toLocation(fc);
		LocationDao dao = new LocationDao();
		return dao.create(location);
	}
	
	public static Object getFeatureCollection(long gid) {
		return GeoJsonRule.getFeatureCollection(gid);
	}
	
	public static String asKml(long gid) {
		return KmlRule.asKml(gid);
	}
	
	public static Location findByGid(long gid) {
		Location location = new LocationDao().read(gid);
		return location;
	}
	
	public static Long update(long gid, FeatureCollection fc){
		if (gid <= 0)
			throw new RuntimeException("id shall be more than 0 but got " + gid);
		Location location = GeoJsonRule.toLocation(fc);
		LocationDao dao = new LocationDao();
		location.setGid(gid);
		return dao.update(location);
	}
	
	public static Long delete(long gid){
		LocationDao dao = new LocationDao();
		return dao.delete(gid);
	}

	static List<Location> findByName(String q, Integer limit,
			Integer offset) {
		List<Location> result = new LocationDao().findByName(q, limit, offset);
		return result;
	}
	
}
