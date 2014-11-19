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

import play.db.jpa.JPA;

@Entity
@Table(name = "AUDIT_AU")
public class AuditAdministrativeUnit {

	private Long id;

	private Long gid;

	private Data data;

	private String operation;

	public static AuditAdministrativeUnit findById(Long id) {
		return JPA.em().find(AuditAdministrativeUnit.class, id);
	}

	@Id
	@Column(name = "id", columnDefinition = "serial")
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@Column(name = "gid")
	public Long getGid() {
		return gid;
	}

	public void setGid(Long gid) {
		this.gid = gid;
	}

	@Embedded
	@AttributeOverrides({
			@AttributeOverride(name = "protect", column = @Column(name = "protect", nullable = true)),
			@AttributeOverride(name = "startDate", column = @Column(name = "start_date", nullable = true)) })
	public Data getData() {
		return data;
	}

	public void setData(Data data) {
		this.data = data;
	}

	public String getOperation() {
		return operation;
	}

	public void setOperation(String operation) {
		this.operation = operation;
	}

}
