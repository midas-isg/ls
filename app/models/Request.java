package models;

import java.sql.Date;
import java.util.List;

public class Request {

	private String queryTerm;
	private Integer limit;
	private Integer offset;
	private Boolean ignoreAccent;
	private Boolean searchNames;
	private Boolean searchOtherNames;
	private Boolean searchCodes;
	private Date startDate;
	private Date endDate;
	private List<Integer> locationTypeIds;

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

	public Date getStartDate() {
		return startDate;
	}

	public void setStartDate(Date startDate) {
		this.startDate = startDate;
	}

	public Date getEndDate() {
		return endDate;
	}

	public void setEndDate(Date endDate) {
		this.endDate = endDate;
	}

	public List<Integer> getLocationTypeIds() {
		return locationTypeIds;
	}

	public void setLocationTypeIds(List<Integer> locationTypeIds) {
		this.locationTypeIds = locationTypeIds;
	}
}
