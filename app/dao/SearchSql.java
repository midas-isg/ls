package dao;

import gateways.database.sql.SQLSanitizer;

import java.sql.Date;
import java.util.List;
import java.util.StringJoiner;

import models.Request;
import models.exceptions.BadRequest;

public class SearchSql {
	public String toQuerySqlString(Request req){
		//TODO: move sanitize() to a proper place
		sanitize(req.getQueryTerm());
		String nameCol = (req.isIgnoreAccent()) ? "unaccent_immutable(name)" : "name";
		String nameTsVector = "to_tsvector('simple', " + nameCol + ")";
		String codeCol = (req.isIgnoreAccent()) ? "unaccent_immutable(code)" : "code";
		String codeTsVector = "to_tsvector('simple', " + codeCol + ")";
		String locTempTable = "origin_names";
		String otherNamesTempTable = "alt_names";
		String codesTempTable = "codes";
		String queryText = toQueryText(req.getQueryTerm());
		String qt = (req.isIgnoreAccent()) ? 
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
		if (req.isSearchOtherNames()){
			q += " , " + otherNamesTempTable + " AS ( "
				+ searchOtherNames(nameTsVector, qt, locTempTable);
			if(hasConditions(req))
				q += " AND gid IN ( SELECT gid FROM location WHERE " 
					+ buildQueryConds(req) + " ) ";
			q += " ) ";
		}
		if (req.isSearchCodes()){
			q += " , " + codesTempTable + " AS ( " 
				+ searchCodes(req, codeTsVector, qt, locTempTable);
			if(req.isSearchOtherNames())
				q += " AND gid NOT IN (SELECT gid FROM " + otherNamesTempTable + " ) ";
			if(hasConditions(req))
				q += " AND gid IN ( SELECT gid FROM location WHERE " 
					+ buildQueryConds(req) + " ) ";
			q += " ) ";
		}
		q += " SELECT * FROM ( ";
		q += " SELECT gid, ts_headline('simple', " + nameCol + ", "+ qt + " ) headline, rank, name "
			+ " FROM " + locTempTable;
		if (req.isSearchOtherNames())
			q += union(nameCol, qt, otherNamesTempTable);
		if (req.isSearchCodes())
			q += union(nameCol, qt, codesTempTable);
		q += " ) AS foo ";
		q += " ORDER BY rank DESC, name ";
		//@formatter:on
		//Logger.debug("name=" + req.getQueryTerm() + " q=\n" + q);
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

	private void sanitize(String value) {
		String tokenized = SQLSanitizer.tokenize(value);
		if(SQLSanitizer.isUnsafe(tokenized)){
			throw new BadRequest("value [ " + value + " ] contains unsafe character(s)!");
		}
	}
}
