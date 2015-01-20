package dao;

import javax.persistence.EntityManager;

import play.Logger;
import play.db.jpa.JPA;
import dao.entities.CodeType;

public class CodeTypeDao {
	CodeType read(long id){
		EntityManager em = JPA.em();
		CodeType result = em.find(CodeType.class, id);
		Logger.debug(id + ": " + result);
		return result;
	}
}
