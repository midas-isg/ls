package interactors;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import play.db.jpa.JPA;
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

	public static List<BigInteger> findGidsByGeometry(String geojsonGeometry, Long superTypeId, Long typeId) {
		String q = toQuery(geojsonGeometry, superTypeId, typeId);

		EntityManager em = JPA.em();
		Query query = em.createNativeQuery(q);
		@SuppressWarnings("unchecked")
		List<BigInteger> list = (List<BigInteger>)query.getResultList();

		return list;
	}
	
	private static String toQuery(String geojsonGeometry, Long superTypeId, Long typeId) {
		String q = 
		"select a.gid "
		+ "from "
		+ " location_geometry a, location l, location_type t, "
		+ " ST_SetSRID(ST_GeomFromGeoJSON('" + geojsonGeometry + "'), 4326) as b "
		+ "where "
		+ " l.gid = a.gid and l.location_type_id = t.id and "
		+ (superTypeId == null ? "" : " t.super_type_id = " + superTypeId + " and ")
		+ " st_intersects(a.multipolygon,b) = true and "
		+ " a.gid !=1216 and "
		+ " a.gid !=1216000 "
		+ "order by "
		+ " st_area(st_intersection(a.multipolygon,b),true) desc, "
		+ " st_area(a.multipolygon,true)";
		return q;
	}
}
