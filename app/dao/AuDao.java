package dao;

import javax.persistence.EntityManager;

import play.Logger;
import play.db.jpa.JPA;
import dao.entities.AdministrativeUnit;

public class AuDao {

	public Long save(AdministrativeUnit au) {
		EntityManager em = JPA.em();
		em.merge(au);
		Long gid = au.getGid();
		Logger.debug("Saved " + gid);
		return gid;
	}

	public AdministrativeUnit findByGid(long gid) {
		EntityManager em = JPA.em();
		AdministrativeUnit result = em.find(AdministrativeUnit.class, gid);
		return result;
	}

}
