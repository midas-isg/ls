package dao.entities;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "super_type")
public class SuperType {
	private Long id;
	private String name;
	private Boolean userDefinable;
	
	@Id
	@Column(columnDefinition = "serial")
	@GeneratedValue(strategy = GenerationType.IDENTITY)
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
	
	@Column(name = "user_definable", 
			nullable = false, 
			columnDefinition="boolean default false")
	public Boolean getUserDefinable() {
		return userDefinable;
	}
	
	public void setUserDefinable(Boolean userDefinable) {
		this.userDefinable = userDefinable;
	}

	@Override
	public String toString() {
		return "SuperType [id=" + id + ", name=" + name + ", userDefinable="
				+ userDefinable + "]";
	}
}
