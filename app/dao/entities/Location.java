package dao.entities;

import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import org.hibernate.annotations.Check;

@Entity
@Check(constraints = "end_date > start_date")
@Table(name = "location")
public class Location implements Comparable<Location> {

	private Long gid;
	private Data data;
	private Location parent;
	private List<Location> children;
	private List<Location> locationsIncluded;
	private List<Code> otherCodes;
	private List<Location> relatedLocations;
	private LocationGeometry multiPolygonGeom;

	@Id
	@Column(name = "gid")
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	public Long getGid() {
		return gid;
	}

	public void setGid(Long gid) {
		this.gid = gid;
	}

	@Embedded
	public Data getData() {
		return data;
	}

	public void setData(Data data) {
		this.data = data;
	}

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "parent_gid", nullable = true)
	public Location getParent() {
		return parent;
	}

	public void setParent(Location parent) {
		this.parent = parent;
	}

	@OneToMany(mappedBy = "parent")
	public List<Location> getChildren() {
		return children;
	}

	public void setChildren(List<Location> children) {
		this.children = children;
	}

	@ManyToMany(fetch = FetchType.LAZY)
	@JoinTable(
			name = "location_definition", 
			joinColumns = {
					@JoinColumn(name = "gid", nullable = false)
			},
			inverseJoinColumns ={
					@JoinColumn(name = "included_gid", nullable = false)
			}
	)
	public List<Location> getLocationsIncluded() {
		return locationsIncluded;
	}

	public void setLocationsIncluded(List<Location> locations) {
		this.locationsIncluded = locations;
	}

	@OneToMany(fetch = FetchType.LAZY, mappedBy = "id")
	public List<Code> getOtherCodes() {
		return otherCodes;
	}

	public void setOtherCodes(List<Code> otherCodes) {
		this.otherCodes = otherCodes;
	}

	@ManyToMany(fetch = FetchType.LAZY)
	@JoinTable(
			name = "related_location", 
			joinColumns = {
					@JoinColumn(name = "gid1", nullable = false)
			},
			inverseJoinColumns ={
					@JoinColumn(name = "gid2", nullable = false)
			}
	)
	public List<Location> getRelatedLocations() {
		return relatedLocations;
	}

	public void setRelatedLocations(List<Location> locations) {
		this.relatedLocations = locations;
	}

	@OneToOne(fetch = FetchType.LAZY, mappedBy = "location", cascade = CascadeType.ALL)
	@JoinColumn(name = "gid")
	public LocationGeometry getGeometry() {
		return multiPolygonGeom;
	}

	public void setGeometry(LocationGeometry geom) {
		this.multiPolygonGeom = geom;
	}

	@Override
	public String toString() {
		String parentText = "";
		if (parent != null)
			parentText = ", parentGid=" + parent.gid;
		String string = "{gid=" + gid + ", name=" + data.getName() 
				+ parentText + "}";
		return string;
	}

	@Override
	public int compareTo(Location o) {
		return toLowerCase(data).compareTo(toLowerCase(o.data));
	}

	private String toLowerCase(Data data) {
		final String defaultResult = "";
		if (data == null) {
			return defaultResult;
		}
		String name = data.getName();
		
		return (name == null) ? defaultResult : name.toLowerCase();
	}
}
