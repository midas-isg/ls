package interactors;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import play.Logger;
import dao.GeometryDao;
import dao.entities.LocationGeometry;
import dao.entities.LocationLowResolutionGeometry;

public class GeometryRule {
	private static Collection<Class<? extends LocationGeometry>> 
	allGeometryClasses = null;

	private static Collection<Class<? extends LocationGeometry>> 
	getAllGeometryClasses() {
		if (allGeometryClasses == null) {
			allGeometryClasses = new ArrayList<>();
			allGeometryClasses.add(LocationGeometry.class);
			allGeometryClasses.add(LocationLowResolutionGeometry.class);
		}
		return allGeometryClasses;
	}

	public static LocationGeometry read(long gid){
		return read(gid, LocationGeometry.class);
	}

	public static LocationGeometry read(long gid, Class<LocationGeometry> geometryClass){
		return new GeometryDao().read(gid, geometryClass);
	}
	
	public static LocationGeometry simplify(long gid, Double tolerance){
		if (tolerance == null)
			return read(gid);
		return new GeometryDao().simplify(gid, tolerance);
	}

	public static String readAsKml(Long gid) {
		return new GeometryDao().readAsKml(gid);
	}

	public static Long delete(long gid){
		return delete(gid, LocationGeometry.class);
	}

	public static Long delete(long gid, Class<LocationGeometry> c) {
		GeometryDao geoDao = new GeometryDao();
		LocationGeometry lg = read(gid, c);
		Long result = null;
		if (lg != null){
			result = geoDao.delete(lg);
		}
		return result;
	}
	public static List<BigInteger> findByPoint(double latitude, double longitude) {
		return new GeometryDao().findGidsByPoint(latitude, longitude);
	}
	
	static Long deleteAllGeometries(long gid) {
		Long result = null;
		for (Class<? extends LocationGeometry> cel : getAllGeometryClasses()){
			@SuppressWarnings("unchecked")
			Class<LocationGeometry> c = (Class<LocationGeometry>)cel;
			Long deletedGid = GeometryRule.delete(gid, c);
			if (result == null)
				result = deletedGid;
		}
		return result;
	}

	public static Double searchForTolerance(long gid, int maxExteriorRings) {
		LocationGeometry geo = read(gid);
		int n = getNumExteriorRings(geo);
		if (n <= maxExteriorRings)
			return null;
		double left = 0.0;
		double right = 20.0;
		double mid = left;
		for (int i = 0; i < 20; i++){
			mid = (left + right) / 2;
			n = findNumExteriorRings(gid, mid);
			Logger.debug(n + ": tolerance=" + mid);
			/*if (n == maxExteriorRings)
				return mid;
			else*/ if (n > maxExteriorRings){
				Logger.debug("\tleft=" + left + "<=" + mid);
				left =  mid;
			} else {
				Logger.debug("\tright=" + right + "<=" + mid);
				right = mid;
			}
		}
		return right;
	}

	private static int findNumExteriorRings(long gid, double mid) {
		int n = new GeometryDao().numGeometriesAfterSimplified(gid, mid);
		return n;
	}

	private static int getNumExteriorRings(LocationGeometry geo) {
		int n = geo.getShapeGeom().getNumGeometries();
		return n;
	}
}
