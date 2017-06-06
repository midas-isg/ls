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
	private List<Long> locationTypeIds;
	private List<Long> codeTypeIds;
	private Boolean verbose;
	private Long rootALC;
	private List<String> onlyFeatureFields;
	private List<String> excludedFeatureFields;
	private Boolean fuzzyMatch;
	private Float fuzzyMatchThreshold = 0.3F;
	private Double latitude;
	private Double longitude;
	private String logic;

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

	public List<Long> getLocationTypeIds() {
		return locationTypeIds;
	}

	public void setLocationTypeIds(List<Long> locationTypeIds) {
		this.locationTypeIds = locationTypeIds;
	}

	public List<Long> getCodeTypeIds() {
		return codeTypeIds;
	}

	public void setCodeTypeIds(List<Long> codeTypeIds) {
		this.codeTypeIds = codeTypeIds;

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

	public List<String> getOnlyFeatureFields() {
		return onlyFeatureFields;
	}

	public void setOnlyFeatureFields(List<String> onlyFeatureFields) {
		this.onlyFeatureFields = onlyFeatureFields;
	}

	public List<String> getExcludedFeatureFields() {
		return excludedFeatureFields;
	}

	public void setExcludedFeatureFields(List<String> excludedFeatureFields) {
		this.excludedFeatureFields = excludedFeatureFields;
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

	public Double getLatitude() {
		return latitude;
	}

	public void setLatitude(Double latitude) {
		this.latitude = latitude;
	}

	public Double getLongitude() {
		return longitude;
	}

	public void setLongitude(Double longitude) {
		this.longitude = longitude;
	}

	public String getLogic() {
		return logic;
	}

	public void setLogic(String logic) {
		this.logic = logic;
	}

}
