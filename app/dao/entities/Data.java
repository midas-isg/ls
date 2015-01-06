package dao.entities;

import java.sql.Date;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;

@Embeddable
public class Data {

	private String name;
	private String code;
	private String codePath;
	private CodeType codeType;
	private AdministrativeUnitType locationType;
	private Date startDate;
	private Date endDate;
	private LocationGeometry multiPolygonGeom;
	private Boolean protect;
	private Date updateDate;
	private Long userId;
	private GisSource gisSource;
	private String description;

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

	@OneToOne(fetch = FetchType.LAZY, mappedBy = "location", cascade = CascadeType.ALL)
	public LocationGeometry getGeometry() {
		return multiPolygonGeom;
	}

	public void setGeometry(LocationGeometry geom) {
		this.multiPolygonGeom = geom;
	}

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

	@ManyToOne
	@JoinColumn(name = "location_type_id", nullable = false)
	public AdministrativeUnitType getLocationType() {
		return locationType;
	}

	public void setLocationType(AdministrativeUnitType locationType) {
		this.locationType = locationType;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	@Column (name = "gid_path" )
	public String getCodePath() {
		return codePath;
	}

	public void setCodePath(String codePath) {
		this.codePath = codePath;
	}

	@ManyToOne
	@JoinColumn(name = "code_type_id", nullable = false)
	public CodeType getCodeType() {
		return codeType;
	}

	public void setCodeType(CodeType codeType) {
		this.codeType = codeType;
	}

	@ManyToOne
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
}
