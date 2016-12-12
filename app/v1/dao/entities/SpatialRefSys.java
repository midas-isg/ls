package v1.dao.entities;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name="spatial_ref_sys")
public class SpatialRefSys {

	@Id
	@Column(name="srid")
	private Long srid;
	
	@Column(name="auth_name")
	private String authName;
	
	@Column(name="auth_srid")
	private Long authSrid;
	
	@Column(name="srtext")
	private String srtext;
	
	@Column(name="proj4text")
	private String proj4text;
	
	public Long getSrid() {
		return srid;
	}

	public void setSrid(Long srid) {
		this.srid = srid;
	}

	public String getAuthName() {
		return authName;
	}

	public void setAuthName(String authName) {
		this.authName = authName;
	}

	public Long getAuthSrid() {
		return authSrid;
	}

	public void setAuthSrid(Long authSrid) {
		this.authSrid = authSrid;
	}

	public String getSrtext() {
		return srtext;
	}

	public void setSrtext(String srtext) {
		this.srtext = srtext;
	}

	public String getProj4text() {
		return proj4text;
	}

	public void setProj4text(String proj4text) {
		this.proj4text = proj4text;
	}

	
}
