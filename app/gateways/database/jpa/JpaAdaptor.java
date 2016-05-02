package gateways.database.jpa;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import models.exceptions.NotFound;
import dao.entities.Entity;

public class JpaAdaptor {
	private EntityManager em;

	public JpaAdaptor(EntityManager em) {
		this.em = em;
	}

	public <T> List<T> query(Class<T> clazz) {
		CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();
		CriteriaQuery<T> criteriaQuery = criteriaBuilder.createQuery(clazz);
		Root<T> root = criteriaQuery.from(clazz);
		criteriaQuery.select(root);
		TypedQuery<T> query = em.createQuery(criteriaQuery);
		return query.getResultList();
	}

	public <T extends Entity> Long create(T data) {
		em.persist(data);
		return data.getId();
	}

	public <T> T read(Class<T> clazz, long id) {
		return find(clazz, id);
	}

	public <T extends Entity> T update(Class<T> clazz, long id, T data) {
		T original = find(clazz, id);
		data.setId(original.getId());
		em.merge(data);
		return data;
	}

	public <T> void delete(Class<T> clazz, long id) {
		T data = find(clazz, id);
		em.remove(data);
	}

	private <T> T find(Class<T> clazz, long id) {
		final T t = em.find(clazz, id);
		if (t == null) {
			final String message = clazz.getSimpleName() + " with ID = " + id
					+ " was not found!";
			throw new NotFound(message);
		}
		return t;
	}
}