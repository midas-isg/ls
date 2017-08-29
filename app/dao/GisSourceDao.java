package dao;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import dao.entities.GisSource;
import gateways.database.jpa.JpaAdaptor;
import play.db.jpa.JPA;

public class GisSourceDao extends DataAccessObject<GisSource> {
	public GisSourceDao(EntityManager em) {
		this(new JpaAdaptor(em));
	}

	private GisSourceDao(JpaAdaptor jpaAdaptor) {
		super(GisSource.class, jpaAdaptor);
	}

	public GisSource findByUrl(String url) {
		EntityManager em = JPA.em();
		String q = "from GisSource where url='" + url + "'";
		Query query = em.createQuery(q);
		return (GisSource) query.getSingleResult();
	}
}
