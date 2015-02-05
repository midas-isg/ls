package dao;

import javax.persistence.EntityManager;

import play.db.jpa.JPA;
import dao.entities.LocationGeometry;

public class GeometryDao {

	public LocationGeometry read(long gid) {
		return read(gid, LocationGeometry.class);
	}

	public LocationGeometry read(long gid, Class<LocationGeometry> geometry) {
		EntityManager em = JPA.em();
		return read(em, gid, geometry);
	}

	public LocationGeometry read(EntityManager em, long gid, Class<LocationGeometry> geometry) {
		//Logger.debug("Find " + geometry.getSimpleName() +  " where gid=" + gid);
		return em.find(geometry, gid);
	}

}
