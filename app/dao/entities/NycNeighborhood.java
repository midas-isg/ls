package dao.entities;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.annotations.Type;

import play.db.jpa.JPA;

import com.vividsolutions.jts.geom.Geometry;

@Entity
@Table(name="nyc_neighborhoods")
public class NycNeighborhood {

	@Id
	private Long gid;
	
	@Column
	private String boroname;
	
	@Column
	private String name;

	@Type(type = "org.hibernate.spatial.GeometryType")
	@Column(name = "geom", nullable = true, columnDefinition="")
	private Geometry geom;

	public static NycNeighborhood findById(Long id) {
		return JPA.em().find(NycNeighborhood.class, id);
	}

	// getters/setters
	public Long getGid() {
		return gid;
	}

	public void setGid(Long gid) {
		this.gid = gid;
	}

	public String getBoroname() {
		return boroname;
	}

	public void setBoroname(String boroname) {
		this.boroname = boroname;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Geometry getGeom() {
		return geom;
	}

	public void setGeom(Geometry geom) {
		this.geom = geom;
	}

}
