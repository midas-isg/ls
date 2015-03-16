package dao.entities;

import java.sql.Date;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.ConstraintMode;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.OneToOne;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.Table;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.Type;

import com.vividsolutions.jts.geom.Geometry;

@Entity
@Table(name = "location_geometry")
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
public class LocationGeometry {
	
	private Long gid;
	private Geometry shapeGeom;
	private Location location;
	private Double area;
	private Date updateDate;

	@GenericGenerator(name = "generator", strategy = "foreign", 
			parameters = @Parameter(name = "property", value = "location")
	)
	@Id
	@GeneratedValue(generator = "generator")
	@Column(unique = true, nullable = false)
	public Long getGid() {
		return gid;
	}

	public void setGid(Long gid) {
		this.gid = gid;
	}

	@Basic(fetch = FetchType.LAZY)
	@Type(type = "org.hibernate.spatial.GeometryType")
	@Column(name = "multipolygon")
	public Geometry getShapeGeom() {
		return shapeGeom;
	}

	public void setShapeGeom(Geometry shapeGeom) {
		this.shapeGeom = shapeGeom;
	}

	@OneToOne(fetch = FetchType.LAZY)
	@PrimaryKeyJoinColumn(foreignKey=@ForeignKey(value=ConstraintMode.CONSTRAINT))
	public Location getLocation() {
		return location;
	}

	public void setLocation(Location location) {
		this.location = location;
	}

	public Double getArea() {
		return area;
	}

	public void setArea(Double area) {
		this.area = area;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((area == null) ? 0 : area.hashCode());
		result = prime * result + ((gid == null) ? 0 : gid.hashCode());
		result = prime
				* result
				+ ((shapeGeom == null) ? 0 : shapeGeom.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		LocationGeometry other = (LocationGeometry) obj;
		if (area == null) {
			if (other.area != null)
				return false;
		} else if (!area.equals(other.area))
			return false;
		if (gid == null) {
			if (other.gid != null)
				return false;
		} else if (!gid.equals(other.gid))
			return false;
		if (shapeGeom == null) {
			if (other.shapeGeom != null)
				return false;
		} else if (!shapeGeom.equals(other.shapeGeom))
			return false;
		return true;
	}

	@Column(name="update_date")
	public Date getUpdateDate() {
		return updateDate;
	}

	public void setUpdateDate(Date updateDate) {
		this.updateDate = updateDate;
	}
}
