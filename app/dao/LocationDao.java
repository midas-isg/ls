package dao;

import static interactors.Util.isTrue;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

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
	static final int SRID = 4326;
	private static final int AU_SUPERTYPE_ID = 3;

	public Long create(Location location) {
		EntityManager em = JPA.em();
		createLocation(location, em);
		createGeometry(location, em);
		createAlternativeNames(location, em);
		createAlternativeCodes(location, em);

		LocationProxyRule.scheduleCacheUpdate(location); // TODO: decouple

		Long gid = location.getGid();
		Logger.info("persisted " + gid);

		return gid;
	}

	private void createAlternativeCodes(Location location, EntityManager em) {
		CodeDao codeDao = new CodeDao(em);
		codeDao.createAll(location.getOtherCodes());
	}

	private void createAlternativeNames(Location location, EntityManager em) {
		AltNameDao altNameDao = new AltNameDao(em);
		altNameDao.createAll(location.getAltNames());
	}

	private void createLocation(Location location, EntityManager em) {
		try {
			em.persist(location);
			em.flush();
		} catch (Exception e) {
			PSQLException pe = PostgreSQLException.toPSQLException(e);
			if (pe != null) {
				throw new PostgreSQLException(pe, pe.getSQLState());
			} else
				throw new RuntimeException(e);
		}
	}

	private void createGeometry(Location location, EntityManager em) {
		LocationGeometry geometry = prepareGeometry(location);
		GeometryDao dao = new GeometryDao();
		dao.create(geometry, em);
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
		if (geometry.getCircleGeometry() != null)
			em.merge(geometry.getCircleGeometry());
		em.merge(location);
		LocationProxyRule.scheduleCacheUpdate(location); // TODO: decouple
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
		if (location != null) {
			gid = location.getGid();
			altNameDao.deleteAll(location.getAltNames());
			codeDao.deleteAll(location.getOtherCodes());
			em.remove(location);
			Logger.info("removed Location with gid=" + gid);
		}

		return gid;
	}

	public List<?> findByTerm(Request req) {
		EntityManager em = JPA.em();
		if (isTrue(req.isFuzzyMatch()) && req.getFuzzyMatchThreshold() != null)
			setFuzzyMatchThresholdForSession(req.getFuzzyMatchThreshold(), em);
		Query query = toNativeSQL(req, em);
		List<?> resultList = query.getResultList();
		return resultList;
	}

	private void setFuzzyMatchThresholdForSession(Float fuzzyMatchThreshold, EntityManager em) {
		em.createNativeQuery("SELECT set_limit(" + fuzzyMatchThreshold + ")").getSingleResult();
	}

	private Query toNativeSQL(Request req, EntityManager em) {
		SearchSql searchSql = new SearchSql();
		String stringQuery = searchSql.toQuerySqlString(req);
		Query query = searchSql.toNativeSQL(stringQuery, req, em);
		return query;
	}

	public List<Location> queryResult2LocationList(List<?> resultList) {
		List<BigInteger> result = getGids(resultList);
		List<Location> locations = findAll(result);
		Map<Long, Location> gid2Location = locations.stream()
				.collect(Collectors.toMap(x -> x.getGid(), Function.identity()));
		Location l;
		List<Location> list = new ArrayList<>();
		for (Object elm : resultList) {
			Object[] objects = (Object[]) elm;
			l = gid2Location.get(Long.valueOf(objects[0].toString()));
			l.setHeadline(objects[1].toString());
			l.setRank(objects[2].toString());
			l.setMatchedTerm(objects[3].toString());
			list.add(l);
		}
		return list;
	}

	public List<BigInteger> getGids(List<?> resultList) {
		List<BigInteger> list = new ArrayList<>();
		for (Object o : resultList) {
			Object[] l = (Object[]) o;
			list.add((BigInteger) l[0]);
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
		List<Location> result = (List<Location>) query.getResultList();

		return result;
	}

	private List<Location> putIntoHierarchy(List<Location> all) {
		EntityManager em = JPA.em();
		List<Location> result = new ArrayList<>();
		Map<Long, Location> gid2location = new HashMap<>();
		Map<Long, List<Location>> gid2orphans = new HashMap<>();
		for (Location location : all) {
			em.detach(location);
			List<Location> children = null;
			Long gid = location.getGid();
			if (gid2orphans.containsKey(gid)) {
				children = gid2orphans.remove(gid);
			} else {
				children = new ArrayList<Location>();
			}
			location.setChildren(children);

			gid2location.put(gid, location);
			Location parentFromDb = location.getParent();
			if (parentFromDb == null) {
				result.add(location);
			} else {
				Long parentGid = parentFromDb.getGid();
				Location foundParent = gid2location.get(parentGid);
				if (foundParent == null) {
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
		List<Location> result = (List<Location>) query.getResultList();

		return result;
	}

	public List<Location> findAll(List<BigInteger> ids) {
		EntityManager em = JPA.em();
		List<Location> result = new ArrayList<>();
		if (ids == null || ids.isEmpty())
			return result;
		CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();
		CriteriaQuery<Location> criteriaQuery = criteriaBuilder.createQuery(Location.class);
		Root<Location> location = criteriaQuery.from(Location.class);
		Expression<String> exp = location.get("gid");
		criteriaQuery.where(exp.in(ids));
		TypedQuery<Location> query = em.createQuery(criteriaQuery);
		return query.getResultList();
	}

	public List<Location> findAllAndSetNameAsHeadline(List<BigInteger> ids) {
		List<Location> result = new ArrayList<>();
		result = findAll(ids);
		for (Location location : result) {
			if (location == null) {
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
		Expression<String> superType = location.get("data").get("locationType").get("superType").get("id");
		Predicate isAU = criteriaBuilder.equal(superType, AU_SUPERTYPE_ID);
		Predicate isParentNull = criteriaBuilder.isNull(location.get("parent"));
		criteriaQuery.where(isAU, isParentNull);
		return em.createQuery(criteriaQuery).getResultList();
	}

	public List<BigInteger> findByFilters(Request req) {

		EntityManager em = JPA.em();
		SearchSql searchSql = new SearchSql();
		String stringQuery = searchSql.toFindByFiltersSQL(req);
		Query query = searchSql.toNativeSQL(stringQuery, req, em);
		@SuppressWarnings("unchecked")
		List<BigInteger> resultList = query.getResultList();
		return resultList;
	}

	public List<Long> getDescendants(Long alc, List<Long> constaintList) {
		//@formatter:off
		String q = " WITH RECURSIVE descendants(gid) AS ( "
					+ " SELECT gid FROM location "
					+ " WHERE gid = ?1 "
					+ "	UNION "
					+ " SELECT l.gid FROM location l, descendants d "
					+ "	WHERE l.parent_gid = d.gid"
					+ "	) "
					+ "	SELECT gid FROM descendants WHERE gid != ?1 ";
		//@formatter:on
		String constraint = SearchSql.listToSqlFilters("gid", constaintList);
		if (constraint != null)
			q += " AND " + constraint;
		EntityManager em = JPA.em();
		Query query = em.createNativeQuery(q);
		query.setParameter(1, alc);
		@SuppressWarnings("unchecked")
		List<BigInteger> resultList = query.getResultList();
		return resultList.stream().map(BigInteger::longValue).collect(Collectors.toList());
	}
}