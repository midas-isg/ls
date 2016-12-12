package v1.dao;

import v1.dao.entities.Code;
import v1.gateways.database.jpa.JpaAdaptor;

import javax.persistence.EntityManager;

public class CodeDao extends DataAccessObject<Code> {

	public CodeDao(EntityManager em) {
		this(new JpaAdaptor(em));
	}

	private CodeDao(JpaAdaptor jpaAdaptor) {
		super(Code.class, jpaAdaptor);
	}
}
