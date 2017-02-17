package v1.interactors;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Polygon;

import play.Logger;
import play.Play;
import dao.LocationDao;
import dao.LocationTypeDao;
import dao.entities.CodeType;
import dao.entities.GisSource;
import dao.entities.Location;
import dao.entities.LocationGeometry;
import dao.entities.LocationType;
import models.Request;

public class LocationRule {
	public static final long PUMA_TYPE_ID = 102L;
	public static final long COMPOSITE_LOCATION_ID = 8L;

	public static final long EPIDEMIC_ZONE_ID = 7L;
	public static final long ISG_CODE_TYPE_ID = 2L;

	private static LocationType epidemicZoneLocationType = null;
	private static CodeType isgCodeType = null;
	private static GisSource alsGisSource = null;

	static LocationType getEpidemicZoneLocationType() {
		if (epidemicZoneLocationType == null) {
			epidemicZoneLocationType = new LocationType();
			epidemicZoneLocationType.setId(EPIDEMIC_ZONE_ID);
			epidemicZoneLocationType.setName("Epidemic Zone");
		}
		return epidemicZoneLocationType;
	}

	static CodeType getIsgCodeType() {
		if (isgCodeType == null) {
			isgCodeType = new CodeType();
			isgCodeType.setId(ISG_CODE_TYPE_ID);
			isgCodeType.setName("ISG");
		}
		return isgCodeType;
	}

	public static GisSource getAlsGisSource() {
		if (alsGisSource == null) {
			alsGisSource = new GisSource();
			alsGisSource.setId(8L);
			alsGisSource.setUrl("ALS");
		}
		return alsGisSource;
	}

	public static Long create(Location location) {
		LocationDao dao = new LocationDao();
		return dao.create(location);
	}

	public static Location read(long gid) {
		return simplify(gid, null);
	}

	public static Location simplifyToMaxExteriorRings(long gid, Integer maxExteriorRings) {
		if (maxExteriorRings == null)
			return read(gid);
		if (maxExteriorRings.equals(0))
			return readWithoutGeometry(gid);

		Double t = GeometryRule.searchForTolerance(gid, maxExteriorRings);
		Logger.info("Found tolerance=" + t + " for GID=" + gid + " and maxExteriorRings=" + maxExteriorRings);
		return simplify(gid, t);
	}

	private static Location readWithoutGeometry(long gid) {
		LocationDao locationDao = new LocationDao();
		Location location = locationDao.read(gid);
		if (location != null)
			location.setGeometry(new LocationGeometry());
		return location;
	}

	public static Location simplify(long gid, Double tolerance) {
		Location result = new LocationDao().read(gid);
		if (result != null) {
			LocationGeometry geo = GeometryRule.simplify(gid, tolerance);
			result.setGeometry(geo);
			List<Location> locations = result.getLocationsIncluded();
			if (locations != null) {
				for (Location l : locations) {
					l.setGeometry(GeometryRule.simplify(l.getGid(), tolerance));
				}
			}
		}
		return result;
	}

	public static Long update(long gid, Location location) {
		if (gid <= 0)
			throw new RuntimeException("id shall be more than 0 but got " + gid);
		LocationDao dao = new LocationDao();
		location.setGid(gid);
		return dao.update(location);
	}

	public static Long deleteTogetherWithAllGeometries(long gid) {
		Long result = delete(gid);
		Long deletedGid = GeometryRule.deleteAllGeometries(gid);
		return result != null ? result : deletedGid;
	}

	private static Long delete(long gid) {
		LocationDao locationDao = new LocationDao();
		Location location = locationDao.read(gid);
		Long deletedGid = null;
		if (location != null) {
			deletedGid = locationDao.delete(location);
		}
		return deletedGid;
	}

	public static List<Location> findByTerm(Request req) {
		LocationDao locationDao = new LocationDao();
		List<?> resultList = locationDao.findByTerm(req);
		List<Location> result = locationDao.queryResult2LocationList(resultList);
		return result;
	}

	public static List<BigInteger> findGids(Request req) {
		LocationDao locationDao = new LocationDao();
		List<?> resultList = locationDao.findByTerm(req);
		return locationDao.getGids(resultList);
	}

	static public List<Location> findByPoint(double latitude, double longitude) {
		List<BigInteger> result = GeometryRule.findByPoint(latitude, longitude);
		List<Location> locations = getLocations(result);
		return locations;
	}

	public static List<Location> getLocations(List<BigInteger> result) {
		return LocationDao.getLocations(result);
	}

	public static List<Location> findByTypeId(long typeId) {
		List<Location> locations = LocationTypeDao.findByType(typeId, 0, 0);
		return locations;
	}

	public static Object getSimplifiedGeometryMetadata(long gid, Double tolerance) {
		Location location = simplify(gid, tolerance);
		Geometry shapeGeometry = location.getGeometry().getShapeGeom();
		Map<String, Object> map = new HashMap<>();
		String DB_NAME = "Database: " + Play.application().configuration().getString("db.default.url");
		map.put("DB_NAME", DB_NAME);
		int numGeometries = shapeGeometry.getNumGeometries();
		map.put("numGeometries", numGeometries);
		int numCoordinates = shapeGeometry.getCoordinates().length;
		map.put("numCoordinates", numCoordinates);
		int maxCoordinates = 0;
		int sumHoles = 0;
		int sumHolePoints = 0;
		int sumShells = 0;
		int sumShellPoints = 0;
		int sumPoints = 0;
		for (int i = 0; i < numGeometries; i++) {
			Geometry p = shapeGeometry.getGeometryN(i);
			int points = p.getCoordinates().length;
			sumPoints += points;
			if (maxCoordinates < points)
				maxCoordinates = points;
			if (p instanceof Polygon) {
				sumShells += 1;
				Polygon polgon = (Polygon) p;
				sumShellPoints += polgon.getExteriorRing().getNumPoints();
				int numHoles = polgon.getNumInteriorRing();
				sumHoles += numHoles;
				for (int j = 0; j < numHoles; j++) {
					LineString hole = polgon.getInteriorRingN(j);
					sumHolePoints += hole.getNumPoints();
				}
			}
		}
		map.put("numExteriorRings", sumShells);
		map.put("maxCoordinatesIn1Geometry", maxCoordinates);
		if (sumPoints != numCoordinates)
			map.put("MISMATCH_sumCoordinates", sumPoints);
		map.put("coordinatePerGeometry", sumPoints / numGeometries);
		map.put("sumInteriorRings", sumHoles);
		map.put("sumInteriorRingPoints", sumHolePoints);
		map.put("tolerance", tolerance);
		map.put("sumExteriorRingPoints", sumShellPoints);
		int nPoints = sumHolePoints + sumShellPoints;
		if (nPoints != numCoordinates)
			map.put("MISMATCH_numHolePoints+numShellPoints", nPoints);
		return map;
	}
}