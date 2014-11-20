package dao.entities;

import java.sql.Date;

import javax.persistence.Column;
import javax.persistence.Embeddable;

import org.hibernate.annotations.Type;

import com.vividsolutions.jts.geom.Geometry;

@Embeddable
public class Data {

	private String name;

	private String pathName;

	private long auTypeId;

	private Date startDate;

	private Date endDate;

	private Geometry multiPolygonGeom;

	private boolean protect;

	private Date updateDate;

	private String userId;

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
	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	@Column(name = "path_name")
	public String getPathName() {
		return pathName;
	}

	public void setPathName(String pathName) {
		this.pathName = pathName;
	}

	@Column(name = "au_type_id")
	public long getAuTypeId() {
		return auTypeId;
	}

	public void setAuTypeId(long auTypeId) {
		this.auTypeId = auTypeId;
	}

}
