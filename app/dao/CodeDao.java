package dao;

import gateways.database.jpa.JpaAdaptor;

import javax.persistence.EntityManager;

import dao.entities.Code;

public class CodeDao extends DataAccessObject<Code> {

	public CodeDao(EntityManager em) {
		this(new JpaAdaptor(em));
	}

	private CodeDao(JpaAdaptor jpaAdaptor) {
		super(Code.class, jpaAdaptor);
	}
}
