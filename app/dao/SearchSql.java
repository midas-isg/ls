package dao;

import static interactors.Util.isTrue;

import java.sql.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import gateways.database.sql.SQLSanitizer;
import models.Request;
import models.exceptions.BadRequest;

public class SearchSql {
	public String toQuerySqlString(Request req) {
		verifyQueryTerm(req.getQueryTerm());
		return toSqlQuery(req);
	}

	public String toFindByFiltersSQL(Request req) {
		String query = " SELECT gid " + " FROM {h-schema}location " + " WHERE " + toQueryConditionSQL(req);
		return query;
	}

	public Query toNativeSQL(String stringQuery, Request req, EntityManager em) {
		Query query = em.createNativeQuery(stringQuery);
		query = setQueryParameters(req, query);
		return query;
	}

	private String toSqlQuery(Request req) {

		String queryTerm = toQueryTerm(req);
		
		String nameTempTable = isTrue(req.isSearchNames()) ? "name_temp_table"
				: null;
		String otherNameTempTable = isTrue(req.isSearchOtherNames()) ? "othername_temp_table"
				: null;
		String codeTempTable = isTrue(req.isSearchCodes()) ? "code_temp_table"
				: null;

		String q = toTempTablesSql(req, queryTerm, nameTempTable, otherNameTempTable,
				codeTempTable);
		q += unionTempTablesSql(req, queryTerm, nameTempTable, otherNameTempTable,
				codeTempTable);
		
		return q;
	}

	private String toTempTablesSql(Request req, String queryTerm,
			String nameTempTable, String otherNameTempTable,
			String codeTempTable) {
		List<String> searchSqls = new ArrayList<>();
				
		String names = toNameSearchSql(req, queryTerm);
		names = (names.isEmpty()) ? names : nameTempTable + " AS ( " + names + " ) ";
		searchSqls.add(names);
		
		String otherNames = toOtherNameSearchSql(req, queryTerm);
		otherNames = (otherNames.isEmpty()) ? otherNames : otherNameTempTable + " AS ( " + otherNames + " ) ";
		searchSqls.add(otherNames);
		
		String codes = toCodeSearchSql(req, queryTerm);
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
			if (!sql.isEmpty())
				joiner.add(sql);
		}
		String q = joiner.toString();
		return q;
	}

	private String toQueryTerm(Request req) {

		String queryTerm = req.getQueryTerm();
		queryTerm = "'" + queryTerm	+ "'";
		if(isTrue(req.isFuzzyMatch()))
			return (isTrue(req.isIgnoreAccent())) ? "unaccent_immutable("
				+ queryTerm + ")" : queryTerm;

		if(isTrue(req.isIgnoreAccent()))
			queryTerm = "unaccent_immutable(" + queryTerm + ")";
		
		String logic = "'" + getLogicChar(req) + "'";
		queryTerm = "replace(strip(" + toTSVector(queryTerm) + ")\\:\\:text, ' ', " + logic + " )\\:\\:tsquery ";
		
		return queryTerm;
	}

	private Character getLogicChar(Request req) {
		String searchLogic = getLogic(req);
		Character logicChar = null;
		if(isConjunction(searchLogic))
			logicChar = '&';
		else if(isDisjunction(searchLogic))
			logicChar = '|';
		return logicChar;
	}

	private String getLogic(Request req) {
		String searchLogic;
		if(req.getLogic() == null)
			searchLogic = "OR";
		else {
			searchLogic = req.getLogic().trim().toUpperCase();
			if(!(isConjunction(searchLogic) || isDisjunction(searchLogic)))
				throw new BadRequest("Invalid 'logic': " + req.getLogic() + ". expected 'AND' or 'OR'");
		}
		return searchLogic;
	}

	private boolean isDisjunction(String searchLogic) {
		return searchLogic.equals("OR");
	}

	private boolean isConjunction(String searchLogic) {
		return searchLogic.equals("AND");
	}

	private String unionTempTablesSql(Request req, String qt, String nameTempTable, String otherNamesTempTable,
			String codesTempTable) {
		String nameCol = isTrue(req.isIgnoreAccent()) ? "unaccent_immutable(name)" : "name";
		List<String> sqlQueries = new ArrayList<>();
		sqlQueries.add(toSelectStatementSql(nameCol, qt, nameTempTable));
		sqlQueries.add(toSelectStatementSql(nameCol, qt, otherNamesTempTable));
		sqlQueries.add(toSelectStatementSql(nameCol, qt, codesTempTable));

		String q = joinStringList(sqlQueries, " UNION ");
		if (q.isEmpty())
			return q;
		q = " SELECT unionTables.*, area FROM ( "
				+ " SELECT DISTINCT ON (gid) gid, headline, rank, name "
				+ " FROM ( " + q + " ) AS foo"
				+ toOrderByStatementWhenDistictGids("name")
				+ " ) AS unionTables "
				+ " JOIN {h-schema}location_geometry USING (gid) ";
		q += toOrderByAreaStatement();
		return q;
	}

	private String toOrderByAreaStatement() {
		return toOrderByStatement(false, true, "name");
	}

	private String toCodeSearchSql(Request req, String qt) {
		String q = "";
		String colName = "code";
		String actualTerm = toActualTerm(req, colName);
		if (isTrue(req.isSearchCodes())) {
			String rankingStatement = toRankingStatement(req, qt, actualTerm);
			String comparisonStatement = toComparisonStatement(req, qt, actualTerm);
			String headlineStatement = toHeadlineStatement(req, qt, colName);
			String unionAllCodes = " SELECT gid, code FROM {h-schema}location WHERE code_type_id != 2 "
									+ " UNION ALL "
									+ " SELECT gid, code FROM {h-schema}alt_code) AS all_codes ";
			if(isConjunction(getLogic(req)) || isTrue(req.isFuzzyMatch())){
				q += " SELECT DISTINCT ON(gid) gid, code AS name, "
						+ rankingStatement + " AS rank, "
						+ headlineStatement + " AS headline "
						+ " FROM ( "
						+ unionAllCodes
						+ " WHERE " + comparisonStatement;
				if (containsFilters(req))
					q += " AND gid IN ( SELECT gid FROM {h-schema}location WHERE "
							+ toQueryConditionSQL(req) + " ) ";
				q += toOrderByStatementWhenDistictGids(colName);
			}
			else if(isDisjunction(getLogic(req))){
				
				String queryTerms2TableSQL = " ( SELECT term "
											+ " FROM " + splitQueryTermsSQL(req.getQueryTerm()) + " AS term "
											+ " ) AS terms ";
				String textComparisonSQL = toTSVector(colName) + " @@ terms.term\\:\\:tsquery ";
				
				q += " SELECT DISTINCT ON(matches.gid) matches.gid, code AS name, sum(match) AS rank, "
						+ headlineStatement + " AS headline "
						+ " FROM ( "
							+ " SELECT gid, code, 1 AS match FROM {h-schema}location, "
							+ queryTerms2TableSQL
							+ " WHERE code_type_id != 2 "
							+ " AND " + textComparisonSQL;
							if (containsFilters(req))
								q += " AND " + toQueryConditionSQL(req);
							q += " UNION ALL "
							+ " SELECT gid, code, 1 AS match FROM {h-schema}alt_code, "
							+ queryTerms2TableSQL
							+ " WHERE " + textComparisonSQL;
							if (containsFilters(req))
								q += " AND gid IN ( SELECT gid FROM {h-schema}location WHERE "
										+ toQueryConditionSQL(req) + " ) ";
				q += " ) AS matches "
					+ " GROUP BY matches.gid, code ";
				q += toOrderByStatementWhenDistictGids(colName);
			}	
		}
		return q;
	}

	private String toOtherNameSearchSql(Request req, String qt) {
		String q = "";
		String colName = "name";
		String actualTerm = toActualTerm(req, colName);
		if (isTrue(req.isSearchOtherNames())) {
			String rankingStatement = toRankingStatement(req, qt, actualTerm);
			String comparisonStatement = toComparisonStatement(req, qt, actualTerm);
			String headlineStatement = toHeadlineStatement(req, qt, colName);
			
			if(isConjunction(getLogic(req)) || isTrue(req.isFuzzyMatch())){
				q += " SELECT DISTINCT ON(gid) gid, name, " + rankingStatement + " AS rank, "
						+ headlineStatement + " AS headline "
						+ " FROM {h-schema}alt_name "
						+ " WHERE " + comparisonStatement;
						if (containsFilters(req))
							q += " AND gid IN ( SELECT gid FROM {h-schema}location WHERE "
									+ toQueryConditionSQL(req) + " ) ";
						q += toOrderByStatementWhenDistictGids(colName);
			}
			else if(isDisjunction(getLogic(req))){
				q += " SELECT DISTINCT ON(matches.gid) matches.gid, name, sum(match) AS rank, "
						+ headlineStatement + " AS headline "
						+ " FROM "
							+ " ( SELECT gid, name, 1 AS match "
							+ " FROM {h-schema}alt_name, "
								+ " ( SELECT term "
								+ " FROM " + splitQueryTermsSQL(req.getQueryTerm()) + " AS term "
								+ " ) AS terms "
								+ " WHERE " + toTSVector(colName) + " @@ terms.term\\:\\:tsquery ";
				if (containsFilters(req))
					q += " AND gid IN ( SELECT gid FROM {h-schema}location WHERE "
							+ toQueryConditionSQL(req) + " ) ";
				
				q += " ) AS matches"
						+ " GROUP BY matches.gid, name ";
				q += toOrderByStatementWhenDistictGids(colName);
			}
		}
		return q;
	}
	
	private String toOrderByStatementWhenDistictGids(String nameOrCode){
		return toOrderByStatement(true, false, nameOrCode);
	}
	
	private String toOrderByStatement(boolean withinDistinctQuery, boolean byArea, String nameOrCode){
		String q = " ORDER BY ";
		if (withinDistinctQuery)
			q += " gid, ";
		q += " rank DESC, length(" + toTSVector(nameOrCode) + "), ";
		q += (byArea) ? " area DESC, " : "";
		q += nameOrCode + " ";
		return q;
	}

	private String toNameSearchSql(Request req, String queryTerm) {
		String q = "";
		String colName = "name";
		String actualTerm = toActualTerm(req, colName);
		if (isTrue(req.isSearchNames())) {
			String rankingStatement = toRankingStatement(req, queryTerm, actualTerm);
			String comparisonStatement = toComparisonStatement(req, queryTerm, actualTerm);
			String headlineStatement = toHeadlineStatement(req, queryTerm, colName);
			if(isConjunction(getLogic(req)) || isTrue(req.isFuzzyMatch())){
				q += " SELECT gid, name, " + rankingStatement + " AS rank, "
						+ headlineStatement + " AS headline " 
						+ " FROM {h-schema}location "
						+ " WHERE " + comparisonStatement;
				if (containsFilters(req))
					q += " AND " + toQueryConditionSQL(req);
			}
			else if(isDisjunction(getLogic(req))){
				q += " SELECT matches.gid, name, sum(match) AS rank , "
						+ headlineStatement + " AS headline "
						+ " FROM "
							+ " ( SELECT gid, name, 1 AS match "
							+ " FROM {h-schema}location, "
								+ " ( SELECT term "
								+ " FROM " + splitQueryTermsSQL(req.getQueryTerm()) + " as term "
								+ " ) AS terms "
								+ " WHERE " + toTSVector(colName) + " @@ terms.term\\:\\:tsquery ";
				if (containsFilters(req))
					q += " AND " + toQueryConditionSQL(req);
				q += " ) AS matches"
				+ " GROUP BY matches.gid, name ";
			}
		}
		return q;
	}

	private String splitQueryTermsSQL(String queryTerm) {
		String tsvector = toTSVector("'" + queryTerm	+ "'");
		return "regexp_split_to_table(strip(" + tsvector + ")\\:\\:text, E'\\\\s+')";
	}

	private String toTSVector(String queryTerm) {
		return "to_tsvector('simple', " + queryTerm + ")";
	}

	private String toHeadlineStatement(Request req, String qt, String columnName) {
		String actualTerm = isTrue(req.isIgnoreAccent()) ? "unaccent_immutable(" + columnName + ")" : columnName;
		if (isTrue(req.isFuzzyMatch())) {
			qt = isTrue(req.isIgnoreAccent()) ? "unaccent_immutable('" + toQueryText(req.getQueryTerm()) + "')"
					: "'" + toQueryText(req.getQueryTerm()) + "'";
			qt = " to_tsquery(" + qt + ") ";
		}
		return " ts_headline('simple', " + actualTerm + ", " + qt + " ) ";
	}

	private String toComparisonStatement(Request req, String qt, String actualTerm) {
		if (isTrue(req.isFuzzyMatch()))
			return actualTerm + " % " + qt;
		return actualTerm + " @@ " + qt;
	}

	private String toActualTerm(Request req, String columnName) {
		String actualTerm = isTrue(req.isIgnoreAccent()) ? "unaccent_immutable(" + columnName + ")" : columnName;
		if (isTrue(req.isFuzzyMatch()))
			return actualTerm;
		String tsVector = toTSVector(actualTerm);
		return tsVector;
	}

	private String toRankingStatement(Request req, String qt, String actualTerm) {

		String rankingStatement;
		if(isTrue(req.isFuzzyMatch()))
			rankingStatement = " similarity(" + actualTerm + ", " + qt + ") ";
		else 
			rankingStatement = " 1 ";
		return rankingStatement;
	}

	private boolean containsFilters(Request req) {
		List<Long> typeIds = req.getLocationTypeIds();
		Date start = req.getStartDate();
		Date end = req.getEndDate();
		Long rootALC = req.getRootALC();
		List<Long> codeTypeIds = req.getCodeTypeIds();
		if ((typeIds != null && !typeIds.isEmpty()) || (codeTypeIds != null && !codeTypeIds.isEmpty()) 
				|| start != null || end != null || rootALC != null)
			return true;
		return false;
	}

	private String toQueryConditionSQL(Request req) {
		String typeCond = toLocationTypeCondition(req.getLocationTypeIds());
		String dateCond = toDateCondition(req.getStartDate(), req.getEndDate());
		String gidCond = toRootGidCondition(req);
		String codeTypeCond = toCodeTypeCondition(req.getCodeTypeIds());
		String[] condList = { typeCond, dateCond, gidCond, codeTypeCond };

		StringJoiner joiner = new StringJoiner(" AND ");
		for (String c : condList) {
			if (c != null)
				joiner.add(c);
		}
		String qc = joiner.toString();
		return qc;
	}

	private String toLocationTypeCondition(List<Long> typeIds) {
		return listToSqlFilters(" location_type_id ", typeIds);
	}

	private String toCodeTypeCondition(List<Long> locationCodeTypeIds) {

		String condition = listToSqlFilters(" code_type_id ", locationCodeTypeIds);
		if (condition == null)
			return null;
		//@formatter:off
		return " gid IN ("
				+ " SELECT gid FROM {h-schema}alt_code WHERE " + condition
				+ " UNION "
				+ " SELECT gid FROM {h-schema}location WHERE " + condition
				+ " ) ";
		//@formatter:on

	}

	private String toRootGidCondition(Request req) {
		if (req.getRootALC() == null)
			return null;
		//@formatter:off
		return " gid IN ( SELECT child_gid FROM {h-schema}forest WHERE root_gid = " + req.getRootALC() + " ) ";
		//@formatter:on
	}

	private String toSelectStatementSql(String column, String qt, String tempTable) {
		String q = "";
		if (tempTable == null)
			return q;
		q = " SELECT gid, headline, rank, name " + " FROM " + tempTable + " ";
		return q;
	}

	static String listToSqlFilters(String columnName, @SuppressWarnings("rawtypes") List list) {
		if (list == null)
			return null;
		if (list.isEmpty())
			return null;
		StringJoiner joiner = new StringJoiner(",", "(", ")");
		for (Object o : list) {
			joiner.add(o.toString());
		}
		return columnName + " IN " + joiner.toString();
	}

	private String toDateCondition(Date startDate, Date endDate) {
		if (startDate != null && endDate != null)
			return " ( (start_date, COALESCE(end_date, CURRENT_DATE)) "
					+ "OVERLAPS (:start, :end) ) ";
		
		else if(startDate != null)
			return " start_date >= :start ";
		
		else if(endDate != null)
			return " end_date <= :end ";
		
		return null;
	}

	private String toQueryText(String q) {
		if (q == null)
			return null;
		q = q.replaceAll(":*\\*", ":*");
		q = q.replaceAll(" *\\| *", "|");
		q = q.replaceAll(" *& *", "&");
		q = q.replaceAll(" *& *", "&");

		q = q.replaceAll("[\\[\\]\\(\\)',.-]", " ");
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
			throw new BadRequest("value [ " + value + " ] contains unsafe character(s)!");
		}
	}

	private Query setQueryParameters(Request req, Query query) {
		if (req.getStartDate() != null)
			query = query.setParameter("start", req.getStartDate());
		if (req.getEndDate() != null)
			query = query.setParameter("end", req.getEndDate());
		if (req.getLimit() != null)
			query.setMaxResults(req.getLimit());
		if (req.getOffset() != null)
			query.setFirstResult(req.getOffset());
		return query;
	}

}
