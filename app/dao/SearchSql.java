package dao;

import gateways.database.sql.SQLSanitizer;

import static interactors.Util.isTrue;

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
		otherNames = (otherNames.isEmpty()) ? otherNames : otherNameTempTable + " AS ( " + otherNames + " ) ";
		searchSqls.add(otherNames);
		
		String codes = toCodeSearchSql(req, qt);
		codes = (codes.isEmpty()) ? codes : codeTempTable + " AS ( " + codes + " ) ";
		searchSqls.add(codes);

		String q = toWithStatementSql(searchSqls);
		return q;
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

	private String toQueryTerm(Request req) {
		String queryText = req.getQueryTerm();
		if(isTrue(req.isFuzzyMatch()))
			return (isTrue(req.isIgnoreAccent())) ? "unaccent_immutable("
				+ "'" + queryText + "'" + ")" : "'" + queryText	+ "'";
		queryText = toQueryText(req.getQueryTerm());
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
		q = " SELECT * FROM ( "
				+ " SELECT DISTINCT ON (gid) gid, headline, rank, name "
				+ " FROM ( " + q + " ) AS foo"
				+ " ORDER BY gid, rank DESC "
				+ " ) AS foo ";
		q += " ORDER BY rank DESC, name ";
		return q;
	}

	private String toCodeSearchSql(Request req, String qt) {
		String q = "";
		String actualTerm = toActualTerm(req, "code");
		if (isTrue(req.isSearchCodes())) {
			String rankingStatement = toRankingStatement(req, qt, actualTerm);
			String comparisonStatement = toComparisonStatement(req, qt, actualTerm);
			String headlineStatement = toHeadlineStatement(req, qt, "code");
			q += " SELECT DISTINCT ON(gid) gid, code AS name, "
					+ rankingStatement + " AS rank, "
					+ headlineStatement + " AS headline "
					+ " FROM ("
					+ " SELECT gid, code FROM location WHERE code_type_id != 2 ";
			if (containsFilters(req))
				q += " AND " + toQueryFiltersSql(req);
			q += " UNION select gid, code FROM alt_code) AS foo"
				+ " WHERE " + comparisonStatement;
			if (containsFilters(req))
				q += " AND gid IN ( SELECT gid FROM location WHERE "
						+ toQueryFiltersSql(req) + " ) ";
		}
		return q;
	}

	private String toOtherNameSearchSql(Request req, String qt) {
		String q = "";
		String actualTerm = toActualTerm(req, "name");
		if (isTrue(req.isSearchOtherNames())) {
			String rankingStatement = toRankingStatement(req, qt, actualTerm);
			String comparisonStatement = toComparisonStatement(req, qt, actualTerm);
			String headlineStatement = toHeadlineStatement(req, qt, "name");
			q += " SELECT DISTINCT ON(gid) gid, name, " + rankingStatement + " AS rank, "
			+ headlineStatement + " AS headline "
			+ " FROM alt_name "
			+ " WHERE " + comparisonStatement;
			if (containsFilters(req))
				q += " AND gid IN ( SELECT gid FROM location WHERE "
						+ toQueryFiltersSql(req) + " ) ";
		}
		return q;
	}

	private String toNameSearchSql(Request req, String qt) {
		String q = "";
		String actualTerm = toActualTerm(req, "name");
		if (isTrue(req.isSearchNames())) {
			String rankingStatement = toRankingStatement(req, qt, actualTerm);
			String comparisonStatement = toComparisonStatement(req, qt, actualTerm);
			String headlineStatement = toHeadlineStatement(req, qt, "name");
			q += " SELECT gid, name, " + rankingStatement + " AS rank, "
					+ headlineStatement + " AS headline " 
					+ " FROM location "
					+ " WHERE " + comparisonStatement;
			if (containsFilters(req))
				q += " AND " + toQueryFiltersSql(req);
		}
		return q;
	}

	private String toHeadlineStatement(Request req, String qt, String columnName) {
		String actualTerm = isTrue(req.isIgnoreAccent()) ? 
				"unaccent_immutable(" + columnName + ")" : columnName;
		if(isTrue(req.isFuzzyMatch())){
			qt = isTrue(req.isIgnoreAccent()) ? 
					"unaccent_immutable('" + toQueryText(req.getQueryTerm()) + "')" : 
						"'" + toQueryText(req.getQueryTerm()) + "'";
			qt = " to_tsquery(" + qt + ") ";
		}
		return " ts_headline('simple', " + actualTerm + ", " + qt + " ) ";
	}

	private String toComparisonStatement(Request req, String qt, String actualTerm) {
		if(isTrue(req.isFuzzyMatch()))
			return actualTerm + " % " + qt;
		return actualTerm + " @@ " + qt;
	}

	private String toActualTerm(Request req, String columnName) {
		String actualTerm = isTrue(req.isIgnoreAccent()) ? "unaccent_immutable("
				+ columnName + ")" : columnName;
		if(isTrue(req.isFuzzyMatch()))
			return actualTerm;
		String tsVector = "to_tsvector('simple', " + actualTerm + ")";
		return tsVector;
	}

	private String toRankingStatement(Request req, String qt, String actualTerm) {
		if(isTrue(req.isFuzzyMatch()))
			return " similarity(" + actualTerm + ", " + qt + ") ";
		return " ts_rank_cd(" + actualTerm + ", " + qt + ", 8) ";
	}

	private boolean containsFilters(Request req) {
		List<Long> typeIds = req.getLocationTypeIds();
		Date start = req.getStartDate();
		Date end = req.getEndDate();
		if (typeIds != null && !typeIds.isEmpty())
			return true;
		if (start != null || end != null)
			return true;
		return false;
	}

	private String toQueryFiltersSql(Request req) {
		String typeCond = listToSqlFilters( " location_type_id ", req.getLocationTypeIds());
		String dateCond = dateCond(req.getStartDate(), req.getEndDate());
		String gidCond = toRootGidCond(req);
		String qc = "";
		if (dateCond != null && typeCond != null)
			qc += typeCond + " AND " + dateCond;
		else if (typeCond != null)
			qc += typeCond;
		else if (dateCond != null)
			qc += dateCond;
		if(gidCond != null)
			qc += qc.isEmpty() ? gidCond : " AND " + gidCond;
		return qc;
	}

	private String toRootGidCond(Request req) {
		if(req.getRootALC() == null)
			return null;
		return " gid in ( SELECT child_gid FROM forest WHERE root_gid = " + req.getRootALC() + " ) ";
	}

	private String toSelectStatementSql(String column, String qt, String tempTable) {
		String q = "";
		if (tempTable == null)
			return q;
		q = " SELECT gid, headline, rank, name " + " FROM " + tempTable + " ";
		return q;
	}

	private String listToSqlFilters(String columnName,
			@SuppressWarnings("rawtypes") List list) {
		if (list == null)
			return null;
		if (list.isEmpty())
			return null;
		StringJoiner joiner = new StringJoiner(",", "(", ")");
		for (Object o : list) {
			joiner.add(o.toString());
		}
		return columnName + " in " + joiner.toString();
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

}
