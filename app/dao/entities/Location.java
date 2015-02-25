package dao.entities;

import java.util.List;

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
import javax.persistence.Table;
import javax.persistence.Transient;

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
	
	private String headline;
	private String rank;

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

	@OneToMany(fetch = FetchType.LAZY, mappedBy = "location")
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

	/*@OneToOne(fetch = FetchType.LAZY, mappedBy = "location", cascade = CascadeType.ALL)
	@JoinColumn(name = "gid")*/
	@Transient
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
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((children == null) ? 0 : children.hashCode());
		result = prime * result + ((data == null) ? 0 : data.hashCode());
		result = prime * result + ((gid == null) ? 0 : gid.hashCode());
		result = prime
				* result
				+ ((locationsIncluded == null) ? 0 : locationsIncluded
						.hashCode());
		result = prime
				* result
				+ ((multiPolygonGeom == null) ? 0 : multiPolygonGeom.hashCode());
		result = prime * result
				+ ((otherCodes == null) ? 0 : otherCodes.hashCode());
		result = prime * result + ((parent == null) ? 0 : parent.hashCode());
		result = prime
				* result
				+ ((relatedLocations == null) ? 0 : relatedLocations.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Location other = (Location) obj;
		if (children == null) {
			if (other.children != null)
				return false;
		} else if (!children.equals(other.children))
			return false;
		if (data == null) {
			if (other.data != null)
				return false;
		} else if (!data.equals(other.data))
			return false;
		if (gid == null) {
			if (other.gid != null)
				return false;
		} else if (!gid.equals(other.gid))
			return false;
		if (locationsIncluded == null) {
			if (other.locationsIncluded != null)
				return false;
		} else if (!locationsIncluded.equals(other.locationsIncluded))
			return false;
		if (multiPolygonGeom == null) {
			if (other.multiPolygonGeom != null)
				return false;
		} else if (!multiPolygonGeom.equals(other.multiPolygonGeom))
			return false;
		if (otherCodes == null) {
			if (other.otherCodes != null)
				return false;
		} else if (!otherCodes.equals(other.otherCodes))
			return false;
		if (parent == null) {
			if (other.parent != null)
				return false;
		} else if (!parent.equals(other.parent))
			return false;
		if (relatedLocations == null) {
			if (other.relatedLocations != null)
				return false;
		} else if (!relatedLocations.equals(other.relatedLocations))
			return false;
		return true;
	}

	@Transient
	public String getHeadline() {
		return headline;
	}

	public void setHeadline(String headline) {
		this.headline = headline;
	}

	@Transient
	public String getRank() {
		return rank;
	}

	public void setRank(String rank) {
		this.rank = rank;
	}

}
