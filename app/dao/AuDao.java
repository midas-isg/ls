package dao;

import interactors.LocationRule;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.hibernate.transform.Transformers;

import play.Logger;
import play.db.jpa.JPA;
import dao.entities.Data;
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
		/*LocationGeometry lg = result.getGeometry();
		Geometry input = lg.getMultiPolygonGeom();
		DouglasPeuckerSimplifier sim = new DouglasPeuckerSimplifier(input);
		em.detach(result);
		lg.setMultiPolygonGeom(sim.getResultGeometry());*/
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
	
	public List<Location> findByName(String name, Integer limit, Integer offset) {
		EntityManager em = JPA.em();
		Query query = em.createQuery("from Location where LOWER(data.name) like LOWER('%" + name + "%')");
		if (limit != null)
			query.setMaxResults(limit);
		if (offset != null)
			query.setFirstResult(offset);
		
		@SuppressWarnings("unchecked")
		List<Location> result = (List<Location>)query.getResultList();
		return result;
	}

	public List<Location> findRoots() {
		return putIntoHierarchy(findAll());
	}

	public List<Location> findRoots2() {
		EntityManager em = JPA.em();
		Query query = em.createQuery("from Location where parent=null");
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
		Query query = em.createQuery("from Location");
		@SuppressWarnings("unchecked")
		List<Location> result = (List<Location>)query.getResultList();
		return result;
	}

	public Map<Long, Location> getGid2location() {
		Map<Long, Location> result = new HashMap<>();
		EntityManager em = JPA.em();
		Session s = em.unwrap(Session.class);
		String query = "select gid, name, parent_gid from location"
				+ " where location_type_id <> " + LocationRule.EPIDEMIC_ZONE_ID;
		SQLQuery q = s.createSQLQuery(query);
		q.setResultTransformer(Transformers.ALIAS_TO_ENTITY_MAP);
		Map<Long, Long> orphants = new HashMap<>();
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> l = (List<Map<String, Object>>)q.list();
		for (Map<String, Object> m : l){
			Long gid = getLong(m, "gid");
			String name = String.valueOf(m.get("name"));
			Long parentGid = getLong(m, "parent_gid");
			
			Location loc = new Location();
			loc.setGid(gid);
			Data data = new Data();
			data.setName(name);
			if (parentGid != null){
				Location parent = result.get(parentGid);
				loc.setParent(parent);
				if (parent == null){
					orphants.put(gid, parentGid);
				} else {
					parent.getChildren().add(loc);
				}
			}
			loc.setData(data);
			loc.setChildren(new ArrayList<Location>());
			result.put(gid, loc);
		}
		for (Map.Entry<Long, Long> pair : orphants.entrySet()){
			Long gid = pair.getKey();
			Location parent = result.get(pair.getValue());
			if (parent == null){
				Logger.warn(gid + " has no parent!");
			} else {
				Location child = result.get(gid);
				child.setParent(parent);
				parent.getChildren().add(child);
			}
		}
		return result;
	}

	private Long getLong(Map<String, Object> m, String key) {
		Object object = m.get(key);
		if (object == null)
			return null;
		return ((BigInteger)object).longValue();
	}
}
