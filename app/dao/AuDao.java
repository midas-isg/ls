package dao;

import javax.persistence.EntityManager;

import play.Logger;
import play.db.jpa.JPA;
import dao.entities.AdministrativeUnit;

public class AuDao {

	public Long create(AdministrativeUnit au) {
		EntityManager em = JPA.em();
		em.persist(au);
		Long gid = au.getGid();
		Logger.debug("persisted " + gid);
		return gid;
	}

	public AdministrativeUnit read(long gid) {
		EntityManager em = JPA.em();
		AdministrativeUnit result = em.find(AdministrativeUnit.class, gid);
		return result;
	}
	
	public Long update(AdministrativeUnit au) {
		EntityManager em = JPA.em();
		em.merge(au);
		Long gid = au.getGid();
		Logger.debug("merged " + gid);
		return gid;
	}

	public Long delete(AdministrativeUnit au) {
		EntityManager em = JPA.em();
		Long gid = au.getGid();
		em.remove(au);
		Logger.debug("removed " + gid);
		return gid;
	}
}
