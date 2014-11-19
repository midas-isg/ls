package dao.entities;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.annotations.Check;

import play.db.jpa.JPA;

@Entity
@Check (constraints = "end_date > start_date")
@Table(name = "AU")
public class AdministrativeUnit {

	private Long gid;

	private Data data;

	public static AdministrativeUnit findById(Long id) {
		return JPA.em().find(AdministrativeUnit.class, id);
	}

	@Id
	@Column(name = "gid", columnDefinition = "serial")
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	public Long getGid() {
		return gid;
	}

	public void setGid(Long gid) {
		this.gid = gid;
	}
	
	@Embedded
	@AttributeOverrides({
		@AttributeOverride(name = "protect", column = @Column(name = "protect", columnDefinition = "boolean default false"))})
	public Data getData() {
		return data;
	}

	public void setData(Data data) {
		this.data = data;
	}

//	@PrePersist
//	public void onInsert() {
//		Logger.debug("before insert");
//	}

}
