package interactors;


import java.math.BigInteger;
import java.util.List;

import dao.LocationDao;
import dao.entities.CodeType;
import dao.entities.GisSource;
import dao.entities.Location;
import dao.entities.LocationGeometry;
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

	public static GisSource getAlsGisSource(){
		if (alsGisSource == null){
			alsGisSource = new GisSource();
			alsGisSource.setId(8L);
			alsGisSource.setUrl("ALS");
		}
		return alsGisSource;
	}
	
	public static Long create(Location location){
		LocationDao dao = new LocationDao();
		return dao.create(location);
	}
	
	public static Location read(long gid) {
		return read(gid, LocationGeometry.class);
	}
	
	public static Location read(long gid, Class<LocationGeometry> geometry) {
		Location result = new LocationDao().read(gid);
		if (result != null){
			LocationGeometry geo = GeometryRule.read(gid, geometry);
			result.setGeometry(geo);
			List<Location> locations = result.getLocationsIncluded();
			if (locations != null){
				for (Location l : locations){
					l.setGeometry(GeometryRule.read(l.getGid(), geometry));
				}
			}
		}

		return result;
	}
	
	public static Long update(long gid, Location location){
		if (gid <= 0)
			throw new RuntimeException("id shall be more than 0 but got " + gid);
		LocationDao dao = new LocationDao();
		location.setGid(gid);
		return dao.update(location);
	}
	
	public static Long deleteTogetherWithAllGeometries(long gid){
		Long result = delete(gid);
		Long deletedGid = GeometryRule.deleteAllGeometries(gid);
		return result != null ? result : deletedGid;
	}

	private static Long delete(long gid) {
		LocationDao locationDao = new LocationDao();
		Location location = locationDao.read(gid);
		Long deletedGid = null;
		if (location != null){
			deletedGid = locationDao.delete(location);
		}
		return deletedGid;
	}

	static List<Location> findByName(String q, Integer limit,
			Integer offset) {
		List<Location> result = new LocationDao().findByName(q, limit, offset);
		return result;
	}
	
	static public List<Location> findByPoint(double latitude, double longitude) {
		List<BigInteger> result = GeometryRule.findByPoint(latitude, longitude);
		List<Location> locations = LocationProxyRule.getLocations(result);
		return locations;
	}
}
