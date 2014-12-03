package dao.entities;

import java.util.List;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
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
@Table(name = "au")
public class AdministrativeUnit {

	private Long gid;

	private Data data;

	private AdministrativeUnit parent;

	private List<AdministrativeUnit> children;

	private List<AdministrativeUnit> locationsIncluded;
	
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
	@AttributeOverrides({ 
		@AttributeOverride(
				name = "protect", 
				column = @Column(
						name = "protect", 
						columnDefinition = "boolean default false"
				)
		) 
	})
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

}