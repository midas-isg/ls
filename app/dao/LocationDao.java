package dao;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.postgresql.util.PSQLException;

import dao.entities.AltName;
import dao.entities.Location;
import dao.entities.LocationGeometry;
import interactors.LocationProxyRule;
import models.Request;
import models.exceptions.PostgreSQLException;
import play.Logger;
import play.db.jpa.JPA;

public class LocationDao {
	static final int SRID =  4326;
	private static final int AU_SUPERTYPE_ID = 3;
	
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
		LocationProxyRule.scheduleCacheUpdate();;
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
		LocationProxyRule.scheduleCacheUpdate();;
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
		List<Location> locations = findAll(result);
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

	private List<Location> findRoots() {
		return putIntoHierarchy(findAll());
	}

	private List<Location> findRoots2() {
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
	
	private static List<Location> findAll(List<BigInteger> ids) {
		EntityManager em = JPA.em();
		List<Location> result = new ArrayList<>();
		if(ids == null || ids.isEmpty())
			return result;
		CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();
		CriteriaQuery<Location> criteriaQuery = criteriaBuilder.createQuery(Location.class);
		Root<Location> location = criteriaQuery.from(Location.class);
		Expression<String> exp = location.get("gid");
		criteriaQuery.where(exp.in(ids));
		TypedQuery<Location> query = em.createQuery(criteriaQuery);
		return query.getResultList();
	}

	public static List<Location> getLocations(List<BigInteger> ids) {
		List<Location> result = new ArrayList<>();
		result = findAll(ids);
		for (Location location: result){
			if (location == null){
				Logger.warn("not found!");
			} else {
				location.setHeadline(location.getData().getName());
			}
		}
		return result;
	}
	
	public List<String> readUniqueNames() {
		EntityManager em = JPA.em();
		Set<String> allNames = new HashSet<>();
		CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();
		
		CriteriaQuery<String> criteriaQuery = criteriaBuilder.createQuery(String.class);
		Root<Location> location = criteriaQuery.from(Location.class);
		criteriaQuery.select(location.get("data").get("name")).distinct(true);
		List<String> names = em.createQuery(criteriaQuery).getResultList();
		
		criteriaQuery = criteriaBuilder.createQuery(String.class);
		Root<AltName> altName = criteriaQuery.from(AltName.class);
		criteriaQuery.select(altName.get("name")).distinct(true);
		List<String> altames = em.createQuery(criteriaQuery).getResultList();

		allNames.addAll(names);
		allNames.addAll(altames);
		return new ArrayList<String>(allNames);
	}
	
	public List<Location> readRoots() {
		EntityManager em = JPA.em();
		CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();
		CriteriaQuery<Location> criteriaQuery = criteriaBuilder.createQuery(Location.class);
		Root<Location> location = criteriaQuery.from(Location.class);
		Expression<String> superType = location.get("data").get("locationType")
				.get("superType").get("id");
		Predicate isAU = criteriaBuilder.equal(superType, AU_SUPERTYPE_ID);
		Predicate isParentNull = criteriaBuilder.isNull(location.get("parent"));
		criteriaQuery.where(isAU, isParentNull);
		return em.createQuery(criteriaQuery).getResultList();
	}
}