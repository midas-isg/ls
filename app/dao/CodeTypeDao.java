package dao;

import javax.persistence.EntityManager;

import dao.entities.CodeType;
import gateways.database.jpa.JpaAdaptor;

public class CodeTypeDao extends DataAccessObject<CodeType>{
	public CodeTypeDao(EntityManager em) {
		this(new JpaAdaptor(em));
	}

	private CodeTypeDao(JpaAdaptor jpaAdaptor) {
		super(CodeType.class, jpaAdaptor);
	}
}
