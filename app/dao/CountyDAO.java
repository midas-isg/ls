package dao;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import play.db.jpa.JPA;
import dao.entities.County;


public class CountyDAO {
	public List<County> findAllCounties() {
		
		EntityManager em = JPA.em();
		Query q = em.createQuery ("FROM County");

		@SuppressWarnings("unchecked")
		List<County> result = q.getResultList();	
		
		return result;
	}

}
