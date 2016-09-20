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
	private Boolean verbose;
	private Long rootALC;
	private List<String> includeOnly;
	private List<String> exclude;
	private Boolean fuzzyMatch;
	private Float fuzzyMatchThreshold = 0.3F;

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

	public Boolean getVerbose() {
		return verbose;
	}

	public void setVerbose(Boolean verbose) {
		this.verbose = verbose;
	}

	public Long getRootALC() {
		return rootALC;
	}

	public void setRootALC(Long rootALC) {
		this.rootALC = rootALC;
	}

	public List<String> getIncludeOnly() {
		return includeOnly;
	}

	public void setIncludeOnly(List<String> includeOnly) {
		this.includeOnly = includeOnly;
	}

	public List<String> getExclude() {
		return exclude;
	}

	public void setExclude(List<String> exclude) {
		this.exclude = exclude;
	}

	public Boolean isFuzzyMatch() {
		return fuzzyMatch;
	}

	public void setFuzzyMatch(Boolean fuzzyMatch) {
		this.fuzzyMatch = fuzzyMatch;
	}

	public Float getFuzzyMatchThreshold() {
		return fuzzyMatchThreshold;
	}

	public void setFuzzyMatchThreshold(Float fuzzyMatchThreshold) {
		this.fuzzyMatchThreshold = fuzzyMatchThreshold;
	}
}
