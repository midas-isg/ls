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
import dao.entities.LocationGeometry;

public class AuDao {
	private static final int SRID =  4326;
	private GeometryDao geoDao = null;
	
	
	public Long create(Location location) {
		EntityManager em = JPA.em();
		LocationGeometry geometry = prepareGeometry(location);
		em.persist(geometry);
		em.persist(location);
		AuHierarchyRule.notifyChange();
		Long gid = location.getGid();
		Logger.info("persisted " + gid);
		return gid;
	}

	public Location read(long gid) {
		return read(gid, LocationGeometry.class);
	}
	
	public Location read(long gid, Class<LocationGeometry> geometry) {
		EntityManager em = JPA.em();
		Location result = em.find(Location.class, gid);
		if (result != null){
			LocationGeometry geo = getGeoDao().read(em, gid, geometry);
			result.setGeometry(geo);
		}
		return result;
	}

	public Long update(Location location) {
		EntityManager em = JPA.em();
		LocationGeometry geometry = prepareGeometry(location);
		em.merge(geometry);
		em.merge(location);
		AuHierarchyRule.notifyChange();
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
		geo.getMultiPolygonGeom().setSRID(SRID);
	}

	public Long delete(long gid) {
		EntityManager em = JPA.em();
		Location location = read(gid);
		Long result = null;
		LocationGeometry lg = null;
		if (location != null){
			lg = location.getGeometry();
			em.remove(location);
			Logger.info("removed Location with gid=" + gid);
			result = gid;
		}
		if (lg == null)
			lg = getGeoDao().read(gid);
		if (lg != null){
			em.remove(lg);
			Logger.info("removed LocationGeometry with gid=" + gid);
			result = gid;
		}
		//TODO should delete sub-class of LocationGeometry!
		return result;
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
		for (Location location : all){
			em.detach(location);
			List<Location> children = null;
			Long gid = location.getGid();
			if (gid2orphans.containsKey(gid)){
				children = gid2orphans.remove(gid);
			} else {
				children = new ArrayList<Location>();
			}
			location.setChildren(children);
			
			gid2location.put(gid, location);
			Location parentFromDb = location.getParent();
			if (parentFromDb == null){
				result.add(location);
			} else {
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

	public List<Location> findByPoint(double latitude, double longitude) {
		EntityManager em = JPA.em();
		//@formatter:off
		String point = "ST_MakePoint(" + longitude + ", " + latitude +")";
		String geometry = "ST_SetSRID("+ point+", "+ SRID + ")";
		String q = "SELECT gid " 
			+ "  FROM location_geometry "
			+ "  WHERE ST_Contains(multipolygon, " + geometry + ")"
			+ "  ORDER BY ST_Area(multipolygon);";
		//@formatter:on
		Query query = em.createNativeQuery(q);
		List<?> resultList = query.getResultList();
		@SuppressWarnings("unchecked")
		List<BigInteger> result = (List<BigInteger>)resultList;
		List<Location> locations = AuHierarchyRule.getLocations(result);
		return locations;
	}
	
	public String asKmlMultiGeometry(long gid) {
		EntityManager em = JPA.em();
		String query = "select ST_AsKML(multipolygon) from location_geometry where gid = " + gid;
		Object singleResult = em.createNativeQuery(query).getSingleResult();
		return singleResult.toString();
	}

	private GeometryDao getGeoDao() {
		if (geoDao == null)
			geoDao = new GeometryDao();
		return geoDao;
	}
}
