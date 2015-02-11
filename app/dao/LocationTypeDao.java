package dao;

import javax.persistence.EntityManager;

import play.db.jpa.JPA;
import dao.entities.LocationType;

public class LocationTypeDao {
	LocationType read(long id){
		EntityManager em = JPA.em();
		LocationType result = em.find(LocationType.class, id);
		return result;
	}
}
