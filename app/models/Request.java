package models;

import java.sql.Date;
import java.util.List;

public class Request {

	String queryTerm;
	int limit;
	int offset;
	boolean unaccent;
	List<String> alsoSearch;
	Date start;
	Date end;
	List<Integer> typeId;

	public String getQueryTerm() {
		return queryTerm;
	}

	public void setQueryTerm(String queryTerm) {
		this.queryTerm = queryTerm;
	}

	public int getLimit() {
		return limit;
	}

	public void setLimit(int limit) {
		this.limit = limit;
	}

	public int getOffset() {
		return offset;
	}

	public void setOffset(int offset) {
		this.offset = offset;
	}

	public boolean getUnaccent() {
		return unaccent;
	}

	public void setUnaccent(boolean unaccent) {
		this.unaccent = unaccent;
	}

	public List<String> getAlsoSearch() {
		return alsoSearch;
	}

	public void setAlsoSearch(List<String> searchWithin) {
		this.alsoSearch = searchWithin;
	}

	public Date getStart() {
		return start;
	}

	public void setStart(Date start) {
		this.start = start;
	}

	public Date getEnd() {
		return end;
	}

	public void setEnd(Date end) {
		this.end = end;
	}

	public List<Integer> getTypeId() {
		return typeId;
	}

	public void setTypeId(List<Integer> typeId) {
		this.typeId = typeId;
	}
}
