package dao;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import play.Logger;
import play.db.jpa.JPA;
import dao.entities.County;


public class CountyDAO {
	public List<County> findAllCounties() {
		EntityManager em = JPA.em();
		String qs = "FROM County "
				+ "where gid=12345"
				;
		Query q = em.createQuery (qs)
				//.setFirstResult(1)
				.setMaxResults(3);

		@SuppressWarnings("unchecked")
		List<County> result = q.getResultList();
		Logger.debug("" + result.get(0).geom.getGeometryType());
		return result;
	}

	public Long save(County c) {
		EntityManager em = JPA.em();
		c.gid = 12345L;
		//em.persist(c);
		em.merge(c);
		Logger.debug("new County id=" + c.gid);
		return c.gid;
	}

	public County findById(Long id){
		EntityManager em = JPA.em();
		County result = em.find(County.class, id);
		return result;
	}
}
