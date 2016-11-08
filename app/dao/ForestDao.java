package dao;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import dao.entities.Forest;
import play.db.jpa.JPA;

public class ForestDao {
	
	public List<Forest> findByChildALC(Long childALC){
		EntityManager em = JPA.em();
		CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();
		CriteriaQuery<Forest> criteriaQuery = criteriaBuilder.createQuery(Forest.class);
		Root<Forest> forest = criteriaQuery.from(Forest.class);
		Predicate pred = criteriaBuilder.equal(forest.get("child").get("gid"), childALC);
		criteriaQuery.where(pred);
		return em.createQuery(criteriaQuery).getResultList();
	}
}
