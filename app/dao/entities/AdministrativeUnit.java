package dao.entities;

import java.util.List;

import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.hibernate.annotations.Check;

@Entity
@Check(constraints = "end_date > start_date")
@Table(name = "location")
public class AdministrativeUnit {

	private Long gid;
	private Data data;
	private AdministrativeUnit parent;
	private List<AdministrativeUnit> children;
	private List<AdministrativeUnit> locationsIncluded;
	private List<Code> otherCodes;
	private List<AdministrativeUnit> relatedLocations;

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

	@ManyToOne
	@JoinColumn(name = "parent_gid", nullable = true)
	public AdministrativeUnit getParent() {
		return parent;
	}

	public void setParent(AdministrativeUnit parent) {
		this.parent = parent;
	}

	@OneToMany(mappedBy = "parent")
	public List<AdministrativeUnit> getChildren() {
		return children;
	}

	public void setChildren(List<AdministrativeUnit> children) {
		this.children = children;
	}

	@ManyToMany
	@JoinTable(
			name = "location_definition", 
			joinColumns = {
					@JoinColumn(name = "gid", nullable = false)
			},
			inverseJoinColumns ={
					@JoinColumn(name = "included_gid", nullable = false)
			}
	)
	public List<AdministrativeUnit> getLocationsIncluded() {
		return locationsIncluded;
	}

	public void setLocationsIncluded(List<AdministrativeUnit> locations) {
		this.locationsIncluded = locations;
	}

	@OneToMany(mappedBy = "id")
	public List<Code> getOtherCodes() {
		return otherCodes;
	}

	public void setOtherCodes(List<Code> otherCodes) {
		this.otherCodes = otherCodes;
	}

	@ManyToMany
	@JoinTable(
			name = "related_location", 
			joinColumns = {
					@JoinColumn(name = "gid1", nullable = false)
			},
			inverseJoinColumns ={
					@JoinColumn(name = "gid2", nullable = false)
			}
	)
	public List<AdministrativeUnit> getRelatedLocations() {
		return relatedLocations;
	}

	public void setRelatedLocations(List<AdministrativeUnit> locations) {
		this.relatedLocations = locations;
	}
}
