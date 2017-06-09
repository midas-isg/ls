package dao;

import javax.persistence.EntityManager;

import dao.entities.GisSource;
import gateways.database.jpa.JpaAdaptor;

public class GisSourceDao extends DataAccessObject<GisSource> {
	public GisSourceDao(EntityManager em) {
		this(new JpaAdaptor(em));
	}

	private GisSourceDao(JpaAdaptor jpaAdaptor) {
		super(GisSource.class, jpaAdaptor);
	}
}
