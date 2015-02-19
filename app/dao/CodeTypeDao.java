package dao;

import javax.persistence.EntityManager;

import play.db.jpa.JPA;
import dao.entities.CodeType;

public class CodeTypeDao {
	public CodeType read(long id){
		EntityManager em = JPA.em();
		CodeType result = em.find(CodeType.class, id);
		return result;
	}
}
