package dao;

import interactors.AuHierarchyRule;

import java.math.BigInteger;
import java.sql.Date;
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
		String tsVector = "to_tsvector('simple', name)";//"to_tsvector('english', name)";
		String queryText = toQueryText(name);
		String qt = /*"to_tsquery(*/"'" + queryText + "'";//+ "')";
		
		String q = "SELECT gid, ts_headline(name, "+ qt + ") headline, rank" 
				+ " FROM (SELECT gid, name, ts_rank_cd(ti, " + qt + ") AS rank"
				+ "  FROM location, " + tsVector + " ti"
				+ "  WHERE ti @@ " + qt 
				+ "  ORDER BY rank DESC, name"
				+ " ) AS foo;";
		Logger.debug("q=\n" + q);
		Query query = em.createNativeQuery(q);
		if (limit != null)
			query.setMaxResults(limit);
		if (offset != null)
			query.setFirstResult(offset);
		List<?> resultList = query.getResultList();
		List<BigInteger> result = getGids(resultList);
		List<Location> locations = AuHierarchyRule.getLocations(result);
		int i = 0;
		for (Location l : locations){
			Object[] objects = (Object[])resultList.get(i++);
			l.setHeadline(objects[1].toString());
			l.setRank(objects[2].toString());
		}
		return locations;
	}

	private List<BigInteger> getGids(List<?> resultList) {
		List<BigInteger> list = new ArrayList<>();
		for (Object o : resultList){
			Object[] l = (Object[])o;
			list.add((BigInteger)l[0]);
		}
		return list;
	}

	private String toQueryText(String q) {
		if (q == null)
			return null;
		q = q.replaceAll(":*\\*", ":*");
		q = q.replaceAll(" *\\| *", "|");
		q = q.replaceAll(" *& *", "&");
		String[] tokens = q.split(" +");
		String del = "";
		String result = "";
		for (String t : tokens){
			result += del + t.toLowerCase();
			del = "&";
		}
		Logger.debug(result);
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
		String query = "SELECT gid, name, parent_gid, code, code_type_id, "
				+ " description, start_date, end_date, location_type_id"
				+ " FROM location"
				;
		SQLQuery q = s.createSQLQuery(query);
		q.setResultTransformer(Transformers.ALIAS_TO_ENTITY_MAP);
		Map<Long, Long> orphants = new HashMap<>();
		LocationTypeDao locationTypeDao = new LocationTypeDao();
		CodeTypeDao codeTypeDao = new CodeTypeDao();
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> l = (List<Map<String, Object>>)q.list();
		for (Map<String, Object> m : l){
			Long gid = getLong(m, "gid");
			Long parentGid = getLong(m, "parent_gid");
			
			Location loc = new Location();
			loc.setGid(gid);
			Data data = new Data();
			data.setName(getString(m, "name"));
			data.setCode(getString(m, "code"));
			data.setDescription(getString(m, "description"));
			data.setStartDate(getDate(m, "start_date"));
			data.setEndDate(getDate(m, "end_date"));
			Long locationTypeId = getLong(m, "location_type_id");
			data.setLocationType(locationTypeDao.read(locationTypeId));
			Long codeTypeId = getLong(m, "code_type_id");
			data.setCodeType(codeTypeDao.read(codeTypeId));
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

	private Date getDate(Map<String, Object> m, String key) {
		return (Date)m.get(key);
	}

	private String getString(Map<String, Object> m, String key) {
		Object obj = m.get(key);
		if (obj == null)
			return null;
		return String.valueOf(obj);
	}

	private Long getLong(Map<String, Object> m, String key) {
		Object object = m.get(key);
		if (object == null)
			return null;
		if (object instanceof BigInteger)
			return ((BigInteger)object).longValue();
		else
			return Long.parseLong(object.toString());
	}
}
