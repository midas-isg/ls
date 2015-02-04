package dao.entities;

import java.sql.Date;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;


@Embeddable
public class Data {

	private String name;
	private String code;
	private CodeType codeType;
	private LocationType locationType;
	private Date startDate;
	private Date endDate;
	private Boolean protect;
	private Date updateDate;
	private Long userId;
	private GisSource gisSource;
	private String description;
	private String kml;

	@Column(name = "name")
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Column(name = "start_date", nullable = false)
	public Date getStartDate() {
		return startDate;
	}

	public void setStartDate(Date startDate) {
		this.startDate = startDate;
	}

	@Column(name = "end_date")
	public Date getEndDate() {
		return endDate;
	}

	public void setEndDate(Date endDate) {
		this.endDate = endDate;
	}

	@Column(nullable = false, columnDefinition = "boolean default false")
	public Boolean getProtect() {
		return protect;
	}

	public void setProtect(Boolean protect) {
		this.protect = protect;
	}

	@Column(name = "update_date", nullable = false)
	public Date getUpdateDate() {
		return updateDate;
	}

	public void setUpdateDate(Date updateDate) {
		this.updateDate = updateDate;
	}

	@Column(name = "user_id")
	public Long getUserId() {
		return userId;
	}

	public void setUserId(Long userId) {
		this.userId = userId;
	}

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "location_type_id", nullable = false)
	public LocationType getLocationType() {
		return locationType;
	}

	public void setLocationType(LocationType locationType) {
		this.locationType = locationType;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "code_type_id", nullable = false)
	public CodeType getCodeType() {
		return codeType;
	}

	public void setCodeType(CodeType codeType) {
		this.codeType = codeType;
	}

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "gis_src_id", nullable = false)
	public GisSource getGisSource() {
		return gisSource;
	}

	public void setGisSource(GisSource gisSource) {
		this.gisSource = gisSource;
	}

	@Column(length=2500)
	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	@Column(length = 1048576) // 1,048,576 = 1 MB
	public String getKml() {
		return kml;
	}

	public void setKml(String kml) {
		this.kml = kml;
	}
}
