package dao.entities;

import java.sql.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.annotations.Type;

import com.vividsolutions.jts.geom.Geometry;

@Entity
@Table(name = "audit_location_geometry")
public class AuditLocationGeometry {

	private Long id;
	private Long gid;
	private Geometry multiPolygonGeom;
	private Double area;
	private String operation;
	private Date updateDate;
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	public Long getId() {
		return id;
	}
	
	public void setId(Long id) {
		this.id = id;
	}
	
	@Column(nullable = true)
	public Long getGid() {
		return gid;
	}
	
	public void setGid(Long gid) {
		this.gid = gid;
	}
	
	@Type(type = "org.hibernate.spatial.GeometryType")
	@Column(name = "multipolygon", nullable = true)
	public Geometry getMultiPolygonGeom() {
		return multiPolygonGeom;
	}
	
	public void setMultiPolygonGeom(Geometry multiPolygonGeom) {
		this.multiPolygonGeom = multiPolygonGeom;
	}

	public Double getArea() {
		return area;
	}

	public void setArea(Double area) {
		this.area = area;
	}
	
	public String getOperation() {
		return operation;
	}

	public void setOperation(String operation) {
		this.operation = operation;
	}

	@Column(name="update_date")
	public Date getUpdateDate() {
		return updateDate;
	}

	public void setUpdateDate(Date updateDate) {
		this.updateDate = updateDate;
	}
}
