package dao.entities;

import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import org.hibernate.annotations.Type;

import com.vividsolutions.jts.geom.Point;

@Entity
@Table(name = "circle_geometry")
public class CircleGeometry implements dao.entities.Entity {

	private long id;
	private Point center;
	private Double radius;
	private Integer quarterSegments = 8;
	private LocationGeometry locationGeometry;

	@Id
	@Column(name = "id", columnDefinition = "serial")
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@Basic(fetch = FetchType.LAZY)
	@Type(type = "org.hibernate.spatial.GeometryType")
	@Column(name = "center")
	public Point getCenter() {
		return center;
	}

	public void setCenter(Point center) {
		this.center = center;
	}

	@Column(nullable = false)
	public Double getRadius() {
		return radius;
	}

	public void setRadius(Double radius) {
		this.radius = radius;
	}

	@Column(name = "quad_segs", nullable = false)
	public Integer getQuarterSegments() {
		return quarterSegments;
	}

	public void setQuarterSegments(Integer quarterSegments) {
		this.quarterSegments = quarterSegments;
	}

	@OneToOne(cascade = {CascadeType.ALL})
	@JoinColumn(name = "gid", nullable = false, foreignKey = @ForeignKey(name = "circle_geometry_gid_fk"))
	public LocationGeometry getLocationGeometry() {
		return locationGeometry;
	}

	public void setLocationGeometry(LocationGeometry locationGeometry) {
		this.locationGeometry = locationGeometry;
	}

}
