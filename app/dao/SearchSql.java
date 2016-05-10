package dao;

import gateways.database.sql.SQLSanitizer;

import java.sql.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

import models.Request;
import models.exceptions.BadRequest;

public class SearchSql {
	public String toQuerySqlString(Request req) {
		verifyQueryTerm(req.getQueryTerm());
		return toSqlQuery(req);
	}

	private String toSqlQuery(Request req) {
		String qt = toQueryTerm(req);
		
		String nameTempTable = isTrue(req.isSearchNames()) ? "name_temp_table"
				: null;
		String otherNameTempTable = isTrue(req.isSearchOtherNames()) ? "othername_temp_table"
				: null;
		String codeTempTable = isTrue(req.isSearchCodes()) ? "code_temp_table"
				: null;

		String q = toTempTablesSql(req, qt, nameTempTable, otherNameTempTable,
				codeTempTable);
		q += unionTempTablesSql(req, qt, nameTempTable, otherNameTempTable,
				codeTempTable);
		//Logger.debug("\n q= " + q + "\n");
		return q;
	}

	private String toTempTablesSql(Request req, String qt,
			String nameTempTable, String otherNameTempTable,
			String codeTempTable) {
		List<String> searchSqls = new ArrayList<>();
		
		String names = toNameSearchSql(req, qt);
		names = (names.isEmpty()) ? names : nameTempTable + " AS ( " + names + " ) ";
		searchSqls.add(names);
		
		String otherNames = toOtherNameSearchSql(req, qt);
		otherNames = (otherNames.isEmpty()) ? otherNames : otherNameTempTable + " AS ( " + otherNames;
		if (!otherNames.isEmpty()){
			if(!names.isEmpty())
				otherNames += "  AND " + exclude(nameTempTable);
			otherNames += " ) ";
		}
		searchSqls.add(otherNames);
		
		String codes = toCodeSearchSql(req, qt);
		codes = (codes.isEmpty()) ? codes : codeTempTable + " AS ( " + codes;
		if (!codes.isEmpty()){
			if(!names.isEmpty())
				codes += "  AND " + exclude(nameTempTable);
			if(!otherNames.isEmpty())
				codes += "  AND " + exclude(otherNameTempTable);
			codes += " ) ";
		}
		searchSqls.add(codes);

		String q = toWithStatementSql(searchSqls);
		return q;
	}

	private String exclude(String nameTempTable) {
		return " gid NOT IN (SELECT gid FROM " + nameTempTable + " ) ";
	}

	private String toWithStatementSql(List<String> searchSqls) {
		String q = joinStringList(searchSqls, " , ");
		if (q.isEmpty())
			return q;
		return " WITH " + q;
	}
	
	private String joinStringList(List<String> list, String delimiter) {
		StringJoiner joiner = new StringJoiner(delimiter);
		for (String sql : list) {
			if(!sql.isEmpty())
				joiner.add(sql);
		}
		String q = joiner.toString();
		return q;
	}

	private String toTsVector(Request req, String columnName) {
		String col = isTrue(req.isIgnoreAccent()) ? "unaccent_immutable("
				+ columnName + ")" : columnName;
		String tsVector = "to_tsvector('simple', " + col + ")";
		return tsVector;
	}

	private String toQueryTerm(Request req) {
		String queryText = toQueryText(req.getQueryTerm());
		String qt = (isTrue(req.isIgnoreAccent())) ? "unaccent_immutable("
				+ "'" + queryText + "'" + ")\\:\\:tsquery" : "'" + queryText
				+ "'";
		return qt;
	}

	private String unionTempTablesSql(Request req, String qt, String nameTempTable,
			String otherNamesTempTable, String codesTempTable) {
		String nameCol = isTrue(req.isIgnoreAccent()) ? "unaccent_immutable(name)"
				: "name";
		List<String> sqlQueries = new ArrayList<>();
		sqlQueries.add(toSelectStatementSql(nameCol, qt, nameTempTable));
		sqlQueries.add(toSelectStatementSql(nameCol, qt, otherNamesTempTable));
		sqlQueries.add(toSelectStatementSql(nameCol, qt, codesTempTable));
		
		String q = joinStringList(sqlQueries, " UNION ");
		if (q.isEmpty())
			return q;
		q = " SELECT * FROM ( " + q + " ) AS foo ";
		q += " ORDER BY rank DESC, name ";
		return q;
	}

	private String toCodeSearchSql(Request req, String qt) {
		String q = "";
		String codeTsVector = toTsVector(req, "code");
		String codeCol = isTrue(req.isIgnoreAccent()) ? "unaccent_immutable(code)"
				: "code";
		if (isTrue(req.isSearchCodes())) {
			q += " SELECT DISTINCT ON(gid) gid, code AS name, "
					+ "ts_rank_cd(ti, "	+ qt + ", 8) AS rank, "
					+ " ts_headline('simple', " + codeCol + ", " + qt + " ) headline "
					+ " FROM ("
					+ " SELECT gid, code FROM location WHERE code_type_id != 2 ";
			if (containsFilters(req))
				q += " AND " + toQueryFiltersSql(req);
			q += " UNION select gid, code FROM alt_code) AS foo" + " , "
					+ codeTsVector + " ti " + " WHERE ti @@ " + qt;
			if (containsFilters(req))
				q += " AND gid IN ( SELECT gid FROM location WHERE "
						+ toQueryFiltersSql(req) + " ) ";
		}
		return q;
	}

	private String toOtherNameSearchSql(Request req, String qt) {
		String q = "";
		String nameTsVector = toTsVector(req, "name");
		String nameCol = isTrue(req.isIgnoreAccent()) ? "unaccent_immutable(name)" : "name";
		if (isTrue(req.isSearchOtherNames())) {
			q += " SELECT DISTINCT ON(gid) gid, name, ts_rank_cd(ti, " + qt + ", 8) AS rank, "
			+ " ts_headline('simple', " + nameCol + ", " + qt + " ) headline "
			+ " FROM alt_name , " + nameTsVector + " ti "
			+ " WHERE ti @@ " + qt;
			if (containsFilters(req))
				q += " AND gid IN ( SELECT gid FROM location WHERE "
						+ toQueryFiltersSql(req) + " ) ";
		}
		return q;
	}

	private String toNameSearchSql(Request req, String qt) {
		String q = "";
		String nameTsVector = toTsVector(req, "name");
		String nameCol = isTrue(req.isIgnoreAccent()) ? "unaccent_immutable(name)"
				: "name";
		if (isTrue(req.isSearchNames())) {
			q += " SELECT gid, name, ts_rank_cd(ti, " + qt + ", 8) AS rank, "
					+ " ts_headline('simple', " + nameCol + ", " + qt + " ) headline " 
					+ " FROM location, " + nameTsVector	+ " ti " 
					+ " WHERE ti @@ " + qt;
			if (containsFilters(req))
				q += " AND " + toQueryFiltersSql(req);
		}
		return q;
	}

	private boolean containsFilters(Request req) {
		List<Integer> typeIds = req.getLocationTypeIds();
		Date start = req.getStartDate();
		Date end = req.getEndDate();
		if (typeIds != null && !typeIds.isEmpty())
			return true;
		if (start != null || end != null)
			return true;
		return false;
	}

	private String toQueryFiltersSql(Request req) {
		String typeCond = locationTypeIdsToSqlFilters(req.getLocationTypeIds());
		String dateCond = dateCond(req.getStartDate(), req.getEndDate());
		String qc = "";
		if (dateCond != null && typeCond != null)
			qc += typeCond + " AND " + dateCond;
		else if (typeCond != null)
			qc += typeCond;
		else if (dateCond != null)
			qc += dateCond;
		return qc;
	}

	private String toSelectStatementSql(String column, String qt, String tempTable) {
		String q = "";
		if (tempTable == null)
			return q;
		q = " SELECT gid, ts_headline('simple', " + column + ", " + qt
				+ " ) headline, rank, name " + " FROM " + tempTable + " ";
		return q;
	}

	private String locationTypeIdsToSqlFilters(
			@SuppressWarnings("rawtypes") List list) {
		if (list == null)
			return null;
		if (list.isEmpty())
			return null;
		StringJoiner joiner = new StringJoiner(",", "(", ")");
		for (Object o : list) {
			joiner.add(o.toString());
		}
		return " location_type_id in " + joiner.toString();
	}

	private String dateCond(Date startDate, Date endDate) {
		if (startDate != null && endDate != null) {
			return " ( (start_date, COALESCE(end_date, CURRENT_DATE)) "
					+ "OVERLAPS (:start, :end) ) ";
		}
		return null;
	}

	private String toQueryText(String q) {
		if (q == null)
			return null;
		q = q.replaceAll(":*\\*", ":*");
		q = q.replaceAll(" *\\| *", "|");
		q = q.replaceAll(" *& *", "&");
		q = q.replaceAll(" *& *", "&");

		q = q.replaceAll("[\\(\\)',.-]", " ");
		String[] tokens = q.trim().split(" +");
		String del = "";
		String result = "";
		for (String t : tokens) {
			result += del + t.toLowerCase();
			del = "&";
		}

		return result;
	}

	private void verifyQueryTerm(String value) {
		String tokenized = SQLSanitizer.tokenize(value);
		if (SQLSanitizer.isUnsafe(tokenized)) {
			throw new BadRequest("value [ " + value
					+ " ] contains unsafe character(s)!");
		}
	}

	private Boolean isTrue(Boolean param) {
		return (param == null) ? false : param;
	}
}
