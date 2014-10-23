package dao.entities;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.annotations.Type;

import play.db.jpa.JPA;

import com.vividsolutions.jts.geom.Geometry;

@Entity
@Table(name="nyc_census_blocks")
public class NycCensusBlock {
	
	@Id
	private Long gid;
	
	@Column(name="blkid")
	private String blkid;
	
	@Column(name="popn_total")
	private Double popnTotal;
	
	@Column(name="popn_white")
	private Double popnWhite;
	
	@Column(name="popn_black")
	private Double popnBlack;

	@Column(name="popn_nativ")
	private Double popnNative;
	
	@Column(name="popn_asian")
	private Double popnAsian;
	
	@Column(name="popn_other")
	private Double popnOther;
	
	@Column(name="hous_total")
	private Double houseTotal;

	@Column(name="hous_own")
	private Double houseOwn;

	@Column(name="hous_rent")
	private Double houseRent;
	
	@Column(name="boroname")
	private String boroname;

	@Type(type = "org.hibernate.spatial.GeometryType")
	@Column(name = "geom", nullable = true)
	private Geometry geom;
	
	public static NycCensusBlock findById(Long id) {
		return JPA.em().find(NycCensusBlock.class, id);
	}

	// getters/setters
	public Long getGid() {
		return gid;
	}

	public void setGid(Long gid) {
		this.gid = gid;
	}

	public String getBlkid() {
		return blkid;
	}

	public void setBlkid(String blkid) {
		this.blkid = blkid;
	}

	public Double getPopnTotal() {
		return popnTotal;
	}

	public void setPopnTotal(Double popnTotal) {
		this.popnTotal = popnTotal;
	}

	public Double getPopnWhite() {
		return popnWhite;
	}

	public void setPopnWhite(Double popnWhite) {
		this.popnWhite = popnWhite;
	}

	public Double getPopnBlack() {
		return popnBlack;
	}

	public void setPopnBlack(Double popnBlack) {
		this.popnBlack = popnBlack;
	}

	public Double getPopnNative() {
		return popnNative;
	}

	public void setPopnNative(Double popnNative) {
		this.popnNative = popnNative;
	}

	public Double getPopnAsian() {
		return popnAsian;
	}

	public void setPopnAsian(Double popnAsian) {
		this.popnAsian = popnAsian;
	}

	public Double getPopnOther() {
		return popnOther;
	}

	public void setPopnOther(Double popnOther) {
		this.popnOther = popnOther;
	}

	public Double getHouseTotal() {
		return houseTotal;
	}

	public void setHouseTotal(Double houseTotal) {
		this.houseTotal = houseTotal;
	}

	public Double getHouseOwn() {
		return houseOwn;
	}

	public void setHouseOwn(Double houseOwn) {
		this.houseOwn = houseOwn;
	}

	public Double getHouseRent() {
		return houseRent;
	}

	public void setHouseRent(Double houseRent) {
		this.houseRent = houseRent;
	}

	public String getBoroname() {
		return boroname;
	}

	public void setBoroname(String boroname) {
		this.boroname = boroname;
	}

	public Geometry getGeom() {
		return geom;
	}

	public void setGeom(Geometry geom) {
		this.geom = geom;
	}


}
