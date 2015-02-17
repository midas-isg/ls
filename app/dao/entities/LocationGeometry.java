package dao.entities;

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
	private Geometry multiPolygonGeom;
	private Location location;
	private Double area;

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
	public Geometry getMultiPolygonGeom() {
		return multiPolygonGeom;
	}

	public void setMultiPolygonGeom(Geometry multiPolygonGeom) {
		this.multiPolygonGeom = multiPolygonGeom;
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
}
