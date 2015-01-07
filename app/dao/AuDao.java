package dao;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import play.Logger;
import play.db.jpa.JPA;
import dao.entities.Location;

public class AuDao {
	public Long create(Location au) {
		EntityManager em = JPA.em();
		em.persist(au);
		Long gid = au.getGid();
		Logger.debug("persisted " + gid);
		return gid;
	}

	public Location read(long gid) {
		EntityManager em = JPA.em();
		Location result = em.find(Location.class, gid);
		return result;
	}
	
	public Long update(Location au) {
		EntityManager em = JPA.em();
		em.merge(au);
		Long gid = au.getGid();
		Logger.debug("merged " + gid);
		return gid;
	}

	public Long delete(Location au) {
		EntityManager em = JPA.em();
		Long gid = au.getGid();
		em.remove(au);
		Logger.debug("removed " + gid);
		return gid;
	}
	
	public List<Location> findRoots() {
		return putIntoHierarchy(findAll());
	}

	public List<Location> findRoots2() {
		EntityManager em = JPA.em();
		Query query = em.createQuery("from AdministrativeUnit where parent=null");
		@SuppressWarnings("unchecked")
		List<Location> result = (List<Location>)query.getResultList();
		return result;
	}

	private List<Location> putIntoHierarchy(List<Location> all) {
		EntityManager em = JPA.em();
		List<Location> result = new ArrayList<>();
		Map<Long, Location> gid2au = new HashMap<>();
		Map<Long, List<Location>> gid2orphans = new HashMap<>();
		for (Location au : all){
			em.detach(au);
			List<Location> children = null;
			Long gid = au.getGid();
			if (gid2orphans.containsKey(gid)){
				children = gid2orphans.remove(gid);
			} else {
				children = new ArrayList<Location>();
			}
			au.setChildren(children);
			
			gid2au.put(gid, au);
			Location parentFromDb = au.getParent();
			if (parentFromDb == null){
				result.add(au);
			} else {
				Long parentGid = parentFromDb.getGid();
				Location foundParent = gid2au.get(parentGid);
				if (foundParent == null){
					List<Location> parentChildren = gid2orphans.get(parentGid);
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

	private List<Location> findAll() {
		EntityManager em = JPA.em();
		Query query = em.createQuery("from AdministrativeUnit");
		@SuppressWarnings("unchecked")
		List<Location> result = (List<Location>)query.getResultList();
		return result;
	}
}
