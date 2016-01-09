package dao;

import interactors.LocationProxyRule;
import interactors.SQLSanitizer;

import java.math.BigInteger;
import java.sql.Date;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;

import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.hibernate.transform.Transformers;

import play.Logger;
import play.db.jpa.JPA;
import dao.entities.Data;
import dao.entities.Location;
import dao.entities.LocationGeometry;

public class LocationDao {
	static final int SRID =  4326;
	
	public Long create(Location location) {
		EntityManager em = JPA.em();
		LocationGeometry geometry = prepareGeometry(location);
		em.persist(geometry);
		em.persist(location);
		LocationProxyRule.notifyChange();
		Long gid = location.getGid();
		Logger.info("persisted " + gid);
		
		return gid;
	}

	public Location read(long gid) {
		EntityManager em = JPA.em();
		Location result = em.find(Location.class, gid);
		
		return result;
	}

	public Long update(Location location) {
		EntityManager em = JPA.em();
		LocationGeometry geometry = prepareGeometry(location);
		em.merge(geometry);
		em.merge(location);
		LocationProxyRule.notifyChange();
		Long gid = location.getGid();
		Logger.info("merged " + gid);
		
		return gid;
	}

	private LocationGeometry prepareGeometry(Location location) {
		LocationGeometry geometry = location.getGeometry();
		setSridToDefault(geometry);
		geometry.setLocation(location);
		geometry.setGid(location.getGid());
		
		return geometry;
	}

	private void setSridToDefault(LocationGeometry geo) {
		geo.getShapeGeom().setSRID(SRID);
	}

	public Long delete(Location location) {
		EntityManager em = JPA.em();
		Long gid = null;
		if (location != null){
			gid = location.getGid();
			em.remove(location);
			Logger.info("removed Location with gid=" + gid);
		}
		
		return gid;
	}
	
	public List<Location> findByName(String name, Integer limit, Integer offset) {
		EntityManager em = JPA.em();
		String tsVector = "to_tsvector('simple', name)";
		String queryText = toQueryText(name);
		String qt = "'" + queryText + "'";
		//@formatter:off
		String q = 
			"SELECT gid, ts_headline('simple', name, "+ qt + ") headline, rank" 
			+ " FROM (SELECT gid, name, ts_rank_cd(ti, " + qt + ") AS rank"
			+ "  FROM location, " + tsVector + " ti"
			+ "  WHERE ti @@ " + qt 
			+ "  ORDER BY rank DESC, name"
			+ " ) AS foo";
		//@formatter:on
		//Logger.debug("name=" + name + " q=\n" + q);
		Query query = em.createNativeQuery(q);
		if (limit != null)
			query.setMaxResults(limit);
		if (offset != null)
			query.setFirstResult(offset);
		List<?> resultList = query.getResultList();
		List<BigInteger> result = getGids(resultList);
		List<Location> locations = LocationProxyRule.getLocations(result);
		int i = 0;
		for (Location l : locations){
			Object[] objects = (Object[])resultList.get(i++);
			l.setHeadline(objects[1].toString());
			l.setRank(objects[2].toString());
		}
		
		return locations;
	}
	
	public List<Location> find(String name, List<Integer> locTypeIds, Date startDate, Date endDate) {
		EntityManager em = JPA.em();
		String tsVector = "to_tsvector('simple', name)";
		sanitize(name);
		String queryText = toQueryText(name);
		String qt = "'" + queryText + "'";
		String typeIdsList = toList(locTypeIds);
		//@formatter:off
		String q = 
			"SELECT gid, ts_headline('simple', name, "+ qt + ") as headline, rank" 
			+ " FROM (SELECT gid, name, ts_rank_cd(ti, " + qt + ") AS rank"
			+ " FROM location, " + tsVector + " ti"
			+ " WHERE ti @@ " + qt;
		if(typeIdsList != null)
			q += " AND "
			+ " location_type_id in " + typeIdsList;
		if(startDate != null && endDate != null)
			q += " AND "
			+ " ( "
			+ " :start BETWEEN start_date AND LEAST( :start, end_date) "
			+ " OR "
			+ " :end BETWEEN start_date AND LEAST( :end ,end_date) "
			+ " ) ";
		else if(startDate != null)
			q += " AND :start BETWEEN start_date AND LEAST( :start, end_date) ";
		else if(endDate != null)
			q += " AND :end BETWEEN start_date AND LEAST( :end ,end_date) ";
			
		q += " ORDER BY rank DESC, name"
			+ " ) AS foo";
		//@formatter:on
		Logger.debug("name=" + name + " q=\n" + q);
		
		Query query = em.createNativeQuery(q);
		if(startDate != null)
			query = query.setParameter("start", startDate);
		if(endDate != null)
			query = query.setParameter("end", endDate);
		List<?> resultList = query.getResultList();
		List<BigInteger> result = getGids(resultList);
		List<Location> locations = LocationProxyRule.getLocations(result);
		int i = 0;
		for (Location l : locations){
			Object[] objects = (Object[])resultList.get(i++);
			l.setHeadline(objects[1].toString());
			l.setRank(objects[2].toString());
		}
		
		return locations;
	}

	private void sanitize(String value) {
		String tokenized = SQLSanitizer.tokenize(value);
		if(SQLSanitizer.isUnsafe(tokenized)){
			throw new RuntimeException("Unsafe request!");
		}
		
	}

	private String toList(List<Integer> locTypeIds) {
		if (locTypeIds == null){
			return null;
		}
		if(locTypeIds.isEmpty())
			return null;
		StringJoiner joiner = new StringJoiner(",", "(", ")");
		for(int i: locTypeIds){
			joiner.add(Integer.toString(i));
		}
		String list = joiner.toString();
		return list;
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

		q = q.replaceAll("[',.-]", " ");
		String[] tokens = q.trim().split(" +");
		String del = "";
		String result = "";
		for (String t : tokens){
			result += del + t.toLowerCase();
			del = "&";
		}
		
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
		Map<Long, Location> gid2location = new HashMap<>();
		Map<Long, List<Location>> gid2orphans = new HashMap<>();
		for(Location location : all) {
			em.detach(location);
			List<Location> children = null;
			Long gid = location.getGid();
			if (gid2orphans.containsKey(gid)){
				children = gid2orphans.remove(gid);
			}
			else {
				children = new ArrayList<Location>();
			}
			location.setChildren(children);
			
			gid2location.put(gid, location);
			Location parentFromDb = location.getParent();
			if (parentFromDb == null){
				result.add(location);
			}
			else {
				Long parentGid = parentFromDb.getGid();
				Location foundParent = gid2location.get(parentGid);
				if (foundParent == null){
					List<Location> parentChildren = gid2orphans.get(parentGid);
					if (parentChildren == null) {
						parentChildren = new ArrayList<>();
						gid2orphans.put(parentGid, parentChildren);
					}
					parentChildren.add(location);
				} else {
					foundParent.getChildren().add(location);
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
				if(parent == null) {
					orphants.put(gid, parentGid);
				}
				else {
					parent.getChildren().add(loc);
				}
			}
			loc.setData(data);
			loc.setChildren(new ArrayList<Location>());
			result.put(gid, loc);
		}
		for(Map.Entry<Long, Long> pair : orphants.entrySet()) {
			Long gid = pair.getKey();
			Location parent = result.get(pair.getValue());
			if (parent == null) {
				Logger.warn(gid + " has no parent!");
			}
			else {
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
		if (obj == null) {
			return null;
		}
		
		return String.valueOf(obj);
	}

	private Long getLong(Map<String, Object> m, String key) {
		Object object = m.get(key);
		if (object == null) {
			return null;
		}
		
		if (object instanceof BigInteger) {
			return ((BigInteger)object).longValue();
		}
		//else {
			return Long.parseLong(object.toString());
		//}
	}
}
