package dao;

import static interactors.Util.getDate;
import static interactors.Util.getLong;
import static interactors.Util.getString;
import gateways.database.sql.SQLSanitizer;
import interactors.LocationProxyRule;

import java.math.BigInteger;
import java.sql.Date;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import models.Request;
import models.exceptions.BadRequest;
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
		Long gid = null;
		if (location != null){
			gid = location.getGid();
			em.remove(location);
			Logger.info("removed Location with gid=" + gid);
		}
		
		return gid;
	}
	
	public List<Location> findByName(String name, Integer limit, Integer offset, boolean altNames) {
		//TODO: move sanitize() to a proper place
		sanitize(name);
		EntityManager em = JPA.em();
		String tsVector = "to_tsvector('simple', name)";
		String queryText = toQueryText(name);
		String qt = "'" + queryText + "'";
		//@formatter:off
		String q = "WITH origin_names AS ( "
				+ " SELECT gid, name, ts_rank_cd(ti, "+ qt + " ) AS rank "
				+ " FROM location, " + tsVector + " ti " 
				+ " WHERE ti @@ "+ qt
				+ " ORDER BY rank DESC,	name )";
		if (altNames)
				q += ", "
				+ " alt_names AS ( "
				+ " SELECT gid, name, ts_rank_cd(ti, "+ qt + " ) AS rank "
				+ " FROM alt_name , " + tsVector + " ti "
				+ " WHERE gid not in (select gid from origin_names) and ti @@ "+ qt
				+ " ORDER BY rank DESC, name ) ";
		
		q += " SELECT gid, ts_headline('simple', name, "+ qt + " ) headline, rank, name "
			+ " FROM origin_names ";
		if (altNames)
			q += " UNION "
			+ " SELECT gid, ts_headline('simple', name, "+ qt + " ) headline, rank, name "
			+ " FROM alt_names ";
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
			l.getData().setName(objects[3].toString());

		}
		
		return locations;
	}
	
	public List<Location> find2(Request req) {
		//TODO: move sanitize() to a proper place
		sanitize(req.getQueryTerm());
		EntityManager em = JPA.em();
		String nameCol = (req.getUnaccent()) ? "unaccent_immutable(name)" : "name";
		String nameTsVector = "to_tsvector('simple', " + nameCol + ")";
		String codeCol = (req.getUnaccent()) ? "unaccent_immutable(code)" : "code";
		String codeTsVector = "to_tsvector('simple', " + codeCol + ")";
		String locTempTable = "origin_names";
		String otherNamesTempTable = "alt_names";
		String codesTempTable = "codes";
		String queryText = toQueryText(req.getQueryTerm());
		String qt = (req.getUnaccent()) ? 
				"unaccent_immutable(" + "'" + queryText + "'" + ")\\:\\:tsquery" 
				: "'" + queryText + "'";
		//@formatter:off
		String q = "WITH origin_names AS ( "
				+ " SELECT gid, name, ts_rank_cd(ti, "+ qt + ", 8) AS rank "
				+ " FROM location, " + nameTsVector + " ti " 
				+ " WHERE ti @@ "+ qt;
		if(hasConditions(req))
			q += " AND " + buildQueryConds(req);
		q += " ) ";
		if (contains(req.getAlsoSearch(), "otherNames")){
			q += " , " + otherNamesTempTable + " AS ( "
				+ searchOtherNames(nameTsVector, qt, locTempTable);
			if(hasConditions(req))
				q += " AND gid IN ( SELECT gid FROM location WHERE " 
					+ buildQueryConds(req) + " ) ";
			q += " ) ";
		}
		if (contains(req.getAlsoSearch(), "codes")){
			q += " , " + codesTempTable + " AS ( " 
				+ searchCodes(req, codeTsVector, qt, locTempTable);
			if(contains(req.getAlsoSearch(),"otherNames"))
				q += " AND gid NOT IN (SELECT gid FROM " + otherNamesTempTable + " ) ";
			if(hasConditions(req))
				q += " AND gid IN ( SELECT gid FROM location WHERE " 
					+ buildQueryConds(req) + " ) ";
			q += " ) ";
		}
		q += " SELECT * FROM ( ";
		q += " SELECT gid, ts_headline('simple', " + nameCol + ", "+ qt + " ) headline, rank, name "
			+ " FROM " + locTempTable;
		if (contains(req.getAlsoSearch(), "otherNames"))
			q += union(nameCol, qt, otherNamesTempTable);
		if (contains(req.getAlsoSearch(), "codes"))
			q += union(nameCol, qt, codesTempTable);
		q += " ) AS foo ";
		//q += buildQueryConds(req);
		q += " ORDER BY rank DESC, name ";
		//@formatter:on
		Logger.debug("name=" + req.getQueryTerm() + " q=\n" + q);
		Query query = em.createNativeQuery(q);
		if(req.getStart() != null)
			query = query.setParameter("start", req.getStart());
		if(req.getEnd() != null)
			query = query.setParameter("end", req.getEnd());
		if (req.getLimit() != 0)
			query.setMaxResults(req.getLimit());
		if (req.getOffset() != 0)
			query.setFirstResult(req.getOffset());
		List<?> resultList = query.getResultList();
		List<BigInteger> result = getGids(resultList);
		List<Location> locations = LocationProxyRule.getLocations(result);
		int i = 0;
		for (Location l : locations){
			Object[] objects = (Object[])resultList.get(i++);
			l.setHeadline(objects[1].toString());
			l.setRank(objects[2].toString());
			//l.getData().setName(objects[3].toString());
		}
		return locations;
	}

	private boolean hasConditions(Request req) {
		List<Integer> typeIds = req.getTypeId();
		Date start = req.getStart();
		Date end = req.getEnd();
		if (typeIds != null && !typeIds.isEmpty())
			return true;
		if (start != null || end != null)
			return true; 
		return false;
	}

	private String buildQueryConds(Request req) {
		String typeCond = locTypesToSqlCond(req.getTypeId());
		String dateCond = dateCond(req.getStart(), req.getEnd());
		String qc = "";
		if (dateCond != null && typeCond != null)
			qc += typeCond + " AND " + dateCond;
		else if (typeCond != null)
			qc += typeCond;
		else if (dateCond != null)
			qc += dateCond;
		return qc;
	}

	private String union(String column, String qt, String tempTable) {
		String q = " UNION "
				+ " SELECT gid, ts_headline('simple', " + column + ", " + qt + " ) headline, rank, name "
				+ " FROM " + tempTable + " ";
		return q;
	}

	private String searchOtherNames(String nameTsVector, String qt, String tempTableName) {
		String q = " SELECT DISTINCT ON(gid) gid, name, ts_rank_cd(ti, "+ qt + ", 8) AS rank "
					+ " FROM alt_name , " + nameTsVector + " ti "
					+ " WHERE gid NOT IN (SELECT gid FROM " + tempTableName + " ) AND ti @@ "+ qt;
		return q;
	}

	private String searchCodes(Request req, String codeTsVector, String qt, String tempTableName) {
		String q = " SELECT DISTINCT ON(gid) gid, code AS name, ts_rank_cd(ti, "+ qt + ", 8) AS rank "
				+ " FROM ("
				+ " SELECT gid, code FROM location WHERE code_type_id != 2 ";
		if(hasConditions(req))
			q += " AND " + buildQueryConds(req);
		q += " UNION select gid, code FROM alt_code) AS foo"
			+ " , " + codeTsVector + " ti "
			+ " WHERE gid NOT IN (SELECT gid FROM " + tempTableName + " ) AND ti @@ " + qt;
		return q;
	}
	
	private boolean contains(List<String> searchWithin, String string) {
		return searchWithin != null && searchWithin.contains(string);
	}

	public List<Location> find(String name, List<Integer> locTypeIds, Date startDate, Date endDate) {
		EntityManager em = JPA.em();
		String tsVector = "to_tsvector('simple', name)";
		//TODO: Move sanitize to a proper place
		sanitize(name);
		String queryText = toQueryText(name);
		String qt = "'" + queryText + "'";
		String typeIdsList = toList(locTypeIds);
		String typeCond = locTypeCond(typeIdsList);
		String dateCond = dateCond(startDate, endDate);
		String orderByRankAndName = " ORDER BY rank DESC, name ";
		//@formatter:off
		String q = 
			" WITH origin_names AS ( "
			+ " SELECT gid, name, ts_rank_cd(ti, "+ qt + " ) AS rank "
			+ " FROM location, " + tsVector + " ti " 
			+ " WHERE ti @@ "+ qt ;
		q += (typeCond != null) ? " AND " + typeCond : "";
		q += (dateCond != null) ? " AND " + dateCond : "";
		q += orderByRankAndName + " ), ";
		q += 
			" alt_names_tmp AS ( "
			+ " SELECT gid, name, ts_rank_cd(ti, "+ qt + " ) AS rank "
			+ " FROM alt_name , " + tsVector + " ti "
			+ " WHERE gid not in (select gid from origin_names) and ti @@ "+ qt
			+ orderByRankAndName + " ), ";
		q += 	
			" alt_names AS( " 
			+ " SELECT alt.gid, alt.name , alt.rank "
			+ " FROM alt_names_tmp alt INNER JOIN location loc ON (alt.gid = loc.gid) ";
		q += (typeCond != null || dateCond != null) ? " WHERE " : "";
		q += (typeCond != null) ? typeCond : "";
		if (dateCond != null && typeCond != null)
			q += " AND " + dateCond;
		else
			q += (dateCond != null) ? dateCond : "";
		q += (typeCond != null || dateCond != null) ? orderByRankAndName : ""; 
		q += " ) ";
		q +=
			" ( SELECT gid, ts_headline('simple', name, "+ qt + " ) headline, rank, name " 
			+ " FROM origin_names "
			+ " UNION "
			+ " SELECT gid, ts_headline('simple', name, "+ qt + " ) headline, rank, name " 
			+ " FROM alt_names ) "
			+ " ORDER BY rank DESC ";
				
		//@formatter:on
		//Logger.debug("name=" + name + " q=\n" + q);
		
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
			l.getData().setName(objects[3].toString());
		}
		
		return locations;
	}

	private String locTypeCond(String typeIdsList) {
		if(typeIdsList != null)
			return " location_type_id in " + typeIdsList;
		return null;
	}

	private String dateCond(Date startDate, Date endDate) {
		String startCond = " :start BETWEEN start_date AND LEAST( :start, end_date) ";
		String endCond = " :end BETWEEN start_date AND LEAST( :end ,end_date) ";
		String startEndCond = " ( "	+ startCond	+ " OR " + endCond + " ) ";
		if(startDate != null && endDate != null)
			return startEndCond;
		else if(startDate != null)
			return startCond;
		else if(endDate != null)
			return endCond;
		return null;
	}

	private void sanitize(String value) {
		String tokenized = SQLSanitizer.tokenize(value);
		if(SQLSanitizer.isUnsafe(tokenized)){
			throw new BadRequest("value [ " + value + " ] contains unsafe character(s)!");
		}
		
	}
	
	private String locTypesToSqlCond(@SuppressWarnings("rawtypes") List list) {
		if (list == null)
			return null;
		if(list.isEmpty())
			return null;
		StringJoiner joiner = new StringJoiner(",", "(", ")");
		for(Object o: list){
			joiner.add(o.toString());
		}
		return " location_type_id in " + joiner.toString();
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
		Map<Long, List<AltName>> otherNames = new AltNameDao().getGid2OtherNames();
		Map<Long, List<Code>> otherCodes = new CodeDao().getGid2OtherCodes();
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
}
