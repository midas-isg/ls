package dao;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import dao.entities.SuperType;
import gateways.database.jpa.JpaAdaptor;
import play.db.jpa.JPA;

public class SuperTypeDao extends DataAccessObject<SuperType> {
	
	public SuperTypeDao(EntityManager em) {
		this(new JpaAdaptor(em));
	}

	private SuperTypeDao(JpaAdaptor jpaAdaptor) {
		super(SuperType.class, jpaAdaptor);
	}
	
	@Override
	public  List<SuperType> findAll() {
		EntityManager em = JPA.em();
		Query query = em.createQuery("from " + SuperType.class.getSimpleName());
		@SuppressWarnings("unchecked")
		List<SuperType> result = (List<SuperType>)query.getResultList();
		return result;
	}
}
