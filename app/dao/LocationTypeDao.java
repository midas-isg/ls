package dao;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import play.db.jpa.JPA;
import dao.entities.LocationType;

public class LocationTypeDao {
	public LocationType read(long id){
		EntityManager em = JPA.em();
		LocationType result = em.find(LocationType.class, id);
		return result;
	}

	public LocationType findByName(String name) {
		EntityManager em = JPA.em();
		String q = "from LocationType where name='" + name + "'";
		Query query = em.createQuery(q);
		System.out.println(q);
		LocationType result = (LocationType)query.getSingleResult();
		return result;
	}
}
