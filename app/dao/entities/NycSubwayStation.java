package dao.entities;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.annotations.Type;

import com.vividsolutions.jts.geom.Geometry;

@Entity
@Table(name="nyc_subway_stations")
public class NycSubwayStation {
	
	@Id
	@Column
	private Long gid;
	
	@Column
	private Double objectid;
	
	@Column
	private Double id;
	
	@Column
	private String name;
	
	@Column(name="alt_name")
	private String altName;
	
	@Column(name="cross_st")
	private String crossSt;
	
	@Column(name="long_name")
	private String longName;
	
	@Column
	private String label;
	
	@Column
	private String borough;
	
	@Column
	private String nghbhd;
	
	@Column
	private String routes;

	@Column
	private String transfers;
	
	@Column
	private String color;
	
	@Column
	private String express;
	
	@Column
	private String closed;

	@Type(type = "org.hibernate.spatial.GeometryType")
	@Column(name = "geom", nullable = true, columnDefinition="")
	private Geometry geom;

	public Long getGid() {
		return gid;
	}

	public void setGid(Long gid) {
		this.gid = gid;
	}

	public Double getObjectid() {
		return objectid;
	}

	public void setObjectid(Double objectid) {
		this.objectid = objectid;
	}

	public Double getId() {
		return id;
	}

	public void setId(Double id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getAltName() {
		return altName;
	}

	public void setAltName(String altName) {
		this.altName = altName;
	}

	public String getCrossSt() {
		return crossSt;
	}

	public void setCrossSt(String crossSt) {
		this.crossSt = crossSt;
	}

	public String getLongName() {
		return longName;
	}

	public void setLongName(String longName) {
		this.longName = longName;
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public String getBorough() {
		return borough;
	}

	public void setBorough(String borough) {
		this.borough = borough;
	}

	public String getNghbhd() {
		return nghbhd;
	}

	public void setNghbhd(String nghbhd) {
		this.nghbhd = nghbhd;
	}

	public String getRoutes() {
		return routes;
	}

	public void setRoutes(String routes) {
		this.routes = routes;
	}

	public String getTransfers() {
		return transfers;
	}

	public void setTransfers(String transfers) {
		this.transfers = transfers;
	}

	public String getColor() {
		return color;
	}

	public void setColor(String color) {
		this.color = color;
	}

	public String getExpress() {
		return express;
	}

	public void setExpress(String express) {
		this.express = express;
	}

	public String getClosed() {
		return closed;
	}

	public void setClosed(String closed) {
		this.closed = closed;
	}

	public Geometry getGeom() {
		return geom;
	}

	public void setGeom(Geometry geom) {
		this.geom = geom;
	}
	
}
