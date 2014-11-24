package dao.entities;

import java.sql.Date;

import javax.persistence.Column;
import javax.persistence.Embeddable;

import org.hibernate.annotations.Type;

import com.vividsolutions.jts.geom.Geometry;

@Embeddable
public class Data {

	private String name;

	private String code;
	
	private String codePath;
	
	private Long codeTypeId;

	private Long auTypeId;

	private Date startDate;

	private Date endDate;

	private Geometry multiPolygonGeom;

	private boolean protect;

	private Date updateDate;

	private Long userId;
	
	private String gisSource;

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

	@Type(type = "org.hibernate.spatial.GeometryType")
	@Column(name = "multipolygon")
	public Geometry getMultiPolygonGeom() {
		return multiPolygonGeom;
	}

	public void setMultiPolygonGeom(Geometry geom) {
		this.multiPolygonGeom = geom;
	}

	public boolean getProtect() {
		return protect;
	}

	public void setProtect(boolean protect) {
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

	@Column(name = "au_type_id")
	public Long getAuTypeId() {
		return auTypeId;
	}

	public void setAuTypeId(Long auTypeId) {
		this.auTypeId = auTypeId;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	@Column (name = "code_path" )
	public String getCodePath() {
		return codePath;
	}

	public void setCodePath(String codePath) {
		this.codePath = codePath;
	}

	@Column (name = "code_type_id")
	public Long getCodeTypeId() {
		return codeTypeId;
	}

	public void setCodeTypeId(Long codeTypeId) {
		this.codeTypeId = codeTypeId;
	}

	@Column (name = "gis_src")
	public String getGisSource() {
		return gisSource;
	}

	public void setGisSource(String gisSource) {
		this.gisSource = gisSource;
	}

}
