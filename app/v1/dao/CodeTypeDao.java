package v1.dao;

import javax.persistence.EntityManager;

import play.db.jpa.JPA;
import v1.dao.entities.CodeType;

public class CodeTypeDao {
	public CodeType read(Long id){
		if(id == null)
			return null;
		EntityManager em = JPA.em();
		CodeType result = em.find(CodeType.class, id);
		return result;
	}
}
