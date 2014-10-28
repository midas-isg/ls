package dao.entities;

import java.sql.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.annotations.Type;

import play.db.jpa.JPA;

import com.vividsolutions.jts.geom.Geometry;

@Entity
@Table(name = "administrative_unit")
public class administrative_unit {
	@Id
	@Column(name = "id", columnDefinition = "serial")
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String name;
	private Date start_date;
	private Date end_date;

	@Type(type = "org.hibernate.spatial.GeometryType")
	private Geometry geom;

	private boolean locked;
	private Date update_date;
	private String user_id;

	
	public static administrative_unit findById(Long id) {
		return JPA.em().find(administrative_unit.class, id);
	}
	
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Date getStartDate() {
		return start_date;
	}

	public void setStartDate(Date startDate) {
		this.start_date = startDate;
	}

	public Date getEndDate() {
		return end_date;
	}

	public void setEndDate(Date endDate) {
		this.end_date = endDate;
	}

	public Geometry getGeom() {
		return geom;
	}

	public void setGeom(Geometry geom) {
		this.geom = geom;
	}

	public boolean isPretected() {
		return locked;
	}

	public void setPretected(boolean pretected) {
		this.locked = pretected;
	}

	public Date getUpdateDate() {
		return update_date;
	}

	public void setUpdateDate(Date updateDate) {
		this.update_date = updateDate;
	}

	public String getUser() {
		return user_id;
	}

	public void setUser(String user) {
		this.user_id = user;
	}

}
