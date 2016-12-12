package v1.dao.entities;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table (name = "location_type")
public class LocationType {
	
	private long id;
	private String name;
	private SuperType superType;
	private LocationType composedOf;
	private Boolean userDefinable;
	
	@Id
	@Column(name = "id", columnDefinition = "serial")
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	public long getId() {
		return id;
	}
	
	public void setId(long id) {
		this.id = id;
	}
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}

	@ManyToOne
	@JoinColumn(name = "super_type_id", nullable = false)
	public SuperType getSuperType() {
		return superType;
	}

	public void setSuperType(SuperType superType) {
		this.superType = superType;
	}

	@ManyToOne
	@JoinColumn(columnDefinition="integer", name = "composed_of_id", nullable = true)
	public LocationType getComposedOf() {
		return composedOf;
	}

	public void setComposedOf(LocationType composedOf) {
		this.composedOf = composedOf;
	}

	@Column(name = "user_definable", 
			nullable = false, 
			columnDefinition="boolean default false")
	public Boolean getUserDefinable() {
		return userDefinable;
	}
	
	public void setUserDefinable(Boolean userDefinable) {
		this.userDefinable = userDefinable;
	}
}
