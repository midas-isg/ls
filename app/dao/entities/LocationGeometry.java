package dao.entities;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.Table;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.Type;

import com.vividsolutions.jts.geom.Geometry;


@Entity
@Table(name = "location_geometry")
public class LocationGeometry {
	
	private Long gid;
	private Geometry multiPolygonGeom;
	private AdministrativeUnit location;

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

	@Type(type = "org.hibernate.spatial.GeometryType")
	@Column(name = "multipolygon")
	public Geometry getMultiPolygonGeom() {
		return multiPolygonGeom;
	}

	public void setMultiPolygonGeom(Geometry multiPolygonGeom) {
		this.multiPolygonGeom = multiPolygonGeom;
	}

	@OneToOne(fetch = FetchType.LAZY)
	@PrimaryKeyJoinColumn
	public AdministrativeUnit getLocation() {
		return location;
	}

	public void setLocation(AdministrativeUnit location) {
		this.location = location;
	}
}
