package models;

import java.sql.Date;
import java.util.List;

public class Request {

	String queryTerm;
	Integer limit;
	Integer offset;
	Boolean ignoreAccent = true;
	Boolean searchNames = true;
	Boolean searchOtherNames = true;
	Boolean searchCodes = true;
	Date start;
	Date end;
	List<Integer> typeId;

	public String getQueryTerm() {
		return queryTerm;
	}

	public void setQueryTerm(String queryTerm) {
		this.queryTerm = queryTerm;
	}

	public Integer getLimit() {
		return limit;
	}

	public void setLimit(Integer limit) {
		this.limit = limit;
	}

	public Integer getOffset() {
		return offset;
	}

	public void setOffset(Integer offset) {
		this.offset = offset;
	}

	public Boolean isIgnoreAccent() {
		return ignoreAccent;
	}

	public void setIgnoreAccent(Boolean ignoreAccent) {
		this.ignoreAccent = ignoreAccent;
	}

	public Boolean isSearchNames() {
		return searchNames;
	}

	public void setSearchNames(Boolean searchNames) {
		this.searchNames = searchNames;
	}

	public Boolean isSearchOtherNames() {
		return searchOtherNames;
	}

	public void setSearchOtherNames(Boolean searchOtherNames) {
		this.searchOtherNames = searchOtherNames;
	}

	public Boolean isSearchCodes() {
		return searchCodes;
	}

	public void setSearchCodes(Boolean searchCodes) {
		this.searchCodes = searchCodes;
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
