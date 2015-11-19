package dao;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import play.db.jpa.JPA;
import dao.entities.SuperType;

public class SuperTypeDao {
	public  List<SuperType> findAll() {
		EntityManager em = JPA.em();
		Query query = em.createQuery("from " + SuperType.class.getSimpleName());
		@SuppressWarnings("unchecked")
		List<SuperType> result = (List<SuperType>)query.getResultList();
		return result;
	}
}
