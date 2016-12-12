package v1.dao;

import v1.dao.entities.AltName;
import v1.gateways.database.jpa.JpaAdaptor;

import javax.persistence.EntityManager;

public class AltNameDao extends DataAccessObject<AltName> {

	public AltNameDao(EntityManager em) {
		this(new JpaAdaptor(em));
	}

	private AltNameDao(JpaAdaptor jpaAdaptor) {
		super(AltName.class, jpaAdaptor);
	}
}
