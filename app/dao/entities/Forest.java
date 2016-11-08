package dao.entities;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name = "forest")
public class Forest {

	private Long id;
	private Location root;
	private Location child;
	private Integer adminLevel;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@ManyToOne
	@JoinColumn(name = "root_gid", nullable = false)
	public Location getRoot() {
		return root;
	}

	public void setRoot(Location root) {
		this.root = root;
	}

	@ManyToOne
	@JoinColumn(name = "child_gid", nullable = false)
	public Location getChild() {
		return child;
	}

	public void setChild(Location child) {
		this.child = child;
	}

	@Column(name = "admin_level")
	public Integer getAdminLevel() {
		return adminLevel;
	}

	public void setAdminLevel(Integer adminLevel) {
		this.adminLevel = adminLevel;
	}
}
