package dao;

import java.math.BigInteger;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import play.Logger;
import play.db.jpa.JPA;
import dao.entities.LocationGeometry;

public class GeometryDao {

	public LocationGeometry read(long gid) {
		return read(gid, LocationGeometry.class);
	}

	public LocationGeometry read(long gid, Class<LocationGeometry> geometryClass) {
		EntityManager em = JPA.em();
		return read(em, gid, geometryClass);
	}

	public LocationGeometry read(EntityManager em, long gid,
			Class<LocationGeometry> geometryClass) {
		//Logger.debug("Find " + geometry.getSimpleName() +  " where gid=" + gid);
		return em.find(geometryClass, gid);
	}		

	public LocationGeometry read(long gid, Double tolerance) {
		EntityManager em = JPA.em();
		//@formatter:off
		String q = "select gid,ST_Simplify(multipolygon,?2) as multipolygon,"
				+ "area,update_date, 1 as clazz_ "
				+ " from location_geometry where gid=?1";
		//@formatter:on
		Query query = em.createNativeQuery(q, LocationGeometry.class);
		query.setParameter(1, gid);
		query.setParameter(2, tolerance);
		return (LocationGeometry)query.getSingleResult();
	}
	
	public Long delete(LocationGeometry lg) {
		EntityManager em = JPA.em();
		Long gid = null;
		if (lg != null){
			gid = lg.getGid();
			em.remove(lg);
			Logger.info(lg.getClass().getSimpleName() + " removed with gid=" + gid);
		}
		return gid;
	}

	public String readAsKml(long gid) {
		EntityManager em = JPA.em();
		String query = "select ST_AsKML(multipolygon) from location_geometry"
				+ " where gid = " + gid;
		Object singleResult = em.createNativeQuery(query).getSingleResult();
		return singleResult.toString();
	}
	
	public List<BigInteger> findGidsByPoint(double latitude, double longitude) {
		EntityManager em = JPA.em();
		//@formatter:off
		String point = "ST_MakePoint(" + longitude + ", " + latitude +")";
		String geometry = "ST_SetSRID("+ point+", "+ LocationDao.SRID + ")";
		String q = "SELECT gid " 
			+ "  FROM location_geometry "
			+ "  WHERE ST_Contains(multipolygon, " + geometry + ")"
			+ "  ORDER BY ST_Area(multipolygon);";
		//@formatter:on
		Query query = em.createNativeQuery(q);
		List<?> resultList = query.getResultList();
		@SuppressWarnings("unchecked")
		List<BigInteger> result = (List<BigInteger>)resultList;
		return result;
	}
}
