package dao;

import gateways.database.jpa.JpaAdaptor;

import javax.persistence.EntityManager;

import dao.entities.AltName;

public class AltNameDao extends DataAccessObject<AltName> {

	public AltNameDao(EntityManager em) {
		this(new JpaAdaptor(em));
	}

	private AltNameDao(JpaAdaptor jpaAdaptor) {
		super(AltName.class, jpaAdaptor);
	}
}
