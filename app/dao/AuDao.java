package dao;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.Query;

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
	
	public List<AdministrativeUnit> findRoots() {
		return putIntoHierarchy(findAll());
	}

	public List<AdministrativeUnit> findRoots2() {
		EntityManager em = JPA.em();
		Query query = em.createQuery("from AdministrativeUnit where parent=null");
		@SuppressWarnings("unchecked")
		List<AdministrativeUnit> result = (List<AdministrativeUnit>)query.getResultList();
		return result;
	}

	private List<AdministrativeUnit> putIntoHierarchy(List<AdministrativeUnit> all) {
		EntityManager em = JPA.em();
		List<AdministrativeUnit> result = new ArrayList<>();
		Map<Long, AdministrativeUnit> gid2au = new HashMap<>();
		Map<Long, List<AdministrativeUnit>> gid2orphans = new HashMap<>();
		for (AdministrativeUnit au : all){
			em.detach(au);
			List<AdministrativeUnit> children = null;
			Long gid = au.getGid();
			if (gid2orphans.containsKey(gid)){
				children = gid2orphans.remove(gid);
			} else {
				children = new ArrayList<AdministrativeUnit>();
			}
			au.setChildren(children);
			
			gid2au.put(gid, au);
			AdministrativeUnit parentFromDb = au.getParent();
			if (parentFromDb == null){
				result.add(au);
			} else {
				Long parentGid = parentFromDb.getGid();
				AdministrativeUnit foundParent = gid2au.get(parentGid);
				if (foundParent == null){
					List<AdministrativeUnit> parentChildren = gid2orphans.get(parentGid);
					if (parentChildren == null) {
						parentChildren = new ArrayList<>();
						gid2orphans.put(parentGid, parentChildren);
					}
					parentChildren.add(au);
				} else {
					foundParent.getChildren().add(au);
				}
			}
		}
		return result;
	}

	private List<AdministrativeUnit> findAll() {
		EntityManager em = JPA.em();
		Query query = em.createQuery("from AdministrativeUnit");
		@SuppressWarnings("unchecked")
		List<AdministrativeUnit> result = (List<AdministrativeUnit>)query.getResultList();
		return result;
	}
}
