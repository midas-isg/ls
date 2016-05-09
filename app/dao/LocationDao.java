package dao;

import static interactors.Util.getDate;
import static interactors.Util.getLong;
import static interactors.Util.getString;
import interactors.LocationProxyRule;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import models.Request;
import models.exceptions.PostgreSQLException;

import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.hibernate.transform.Transformers;
import org.postgresql.util.PSQLException;

import play.Logger;
import play.db.jpa.JPA;
import dao.entities.AltName;
import dao.entities.Code;
import dao.entities.Data;
import dao.entities.Entity;
import dao.entities.Location;
import dao.entities.LocationGeometry;

public class LocationDao {
	static final int SRID =  4326;
	
	public Long create(Location location) {
		EntityManager em = JPA.em();
		LocationGeometry geometry = prepareGeometry(location);
		try {
			em.persist(geometry);
			em.persist(location);
			em.flush();
		} catch (Exception e){
			PSQLException pe = getPSQLException(e);
			if(pe != null){
				throw new PostgreSQLException(pe, pe.getSQLState());
			}
			else
				throw new RuntimeException(e);
		}
		AltNameDao altNameDao = new AltNameDao(em);
		CodeDao codeDao = new CodeDao(em);
		altNameDao.createAll(location.getAltNames());
		codeDao.createAll(location.getOtherCodes());
		LocationProxyRule.notifyChange();
		Long gid = location.getGid();
		Logger.info("persisted " + gid);
		
		return gid;
	}

	private PSQLException getPSQLException(Exception e) {
		Throwable cause = e.getCause();
		while (cause != null){
			if(cause instanceof PSQLException)
				return (PSQLException) cause;
			cause = cause.getCause();
		}
		return null;
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
		AltNameDao altNameDao = new AltNameDao(em);
		CodeDao codeDao = new CodeDao(em);
		Long gid = null;
		if (location != null){
			gid = location.getGid();
			altNameDao.deleteAll(location.getAltNames());
			codeDao.deleteAll(location.getOtherCodes());
			em.remove(location);
			Logger.info("removed Location with gid=" + gid);
		}
		
		return gid;
	}

	public List<Location> findByTerm(Request req) {
		EntityManager em = JPA.em();
		String q = new SearchSql().toQuerySqlString(req);
		Query query = em.createNativeQuery(q);
		query = setQueryParameters(req, query);
		List<?> resultList = query.getResultList();
		List<Location> locations = queryResult2LocationList(resultList);
		return locations;
	}

	private Query setQueryParameters(Request req, Query query) {
		if(req.getStartDate() != null)
			query = query.setParameter("start", req.getStartDate());
		if(req.getEndDate() != null)
			query = query.setParameter("end", req.getEndDate());
		if (req.getLimit() != null)
			query.setMaxResults(req.getLimit());
		if (req.getOffset() != null)
			query.setFirstResult(req.getOffset());
		return query;
	}
	
	private List<Location> queryResult2LocationList(List<?> resultList) {
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

	private List<BigInteger> getGids(List<?> resultList) {
		List<BigInteger> list = new ArrayList<>();
		for (Object o : resultList){
			Object[] l = (Object[])o;
			list.add((BigInteger)l[0]);
		}
		
		return list;
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
		AltNameDao altNameDao = new AltNameDao(em);
		Map<Long, List<AltName>> otherNames = getGid2Entity(altNameDao);
		CodeDao codeDao = new CodeDao(em);
		Map<Long, List<Code>> otherCodes = getGid2Entity(codeDao);
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
			if(otherNames.containsKey(gid))
				loc.setAltNames(otherNames.get(gid));
			if(otherCodes.containsKey(gid))
				loc.setOtherCodes(otherCodes.get(gid));
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

	private <T extends Entity> Map<Long, List<T>> getGid2Entity(
			DataAccessObject<T> daoClass) {
		List<T> all = daoClass.findAll();
		Map<Long, List<T>> result = new HashMap<>();
		Long gid;
		for(T entity: all){
			gid = entity.getLocation().getGid();
			if(!result.containsKey(gid))
				result.put(gid, new ArrayList<T>());
			result.get(gid).add(entity);
		}	
		return result;
	}
}