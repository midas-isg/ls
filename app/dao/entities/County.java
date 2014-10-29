package dao.entities;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.annotations.Type;

import com.vividsolutions.jts.geom.Geometry;

@Entity
@Table(name="tl_2014_us_county")
public class County {
	@Override
	public String toString() {
		String geo = geom == null ? "null" : geom.toText();
		return "County [gid=" + gid + ", name=" + name + ", geom=" + geo + "]";
	}

	@Id
	public Long gid;
	
	public String name;
	
	@Type(type = "org.hibernate.spatial.GeometryType")
	@Column(name = "geom", nullable = true, columnDefinition="")
	public Geometry geom;
	
	@Transient
	public String geomText;

}
