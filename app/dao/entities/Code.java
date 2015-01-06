package dao.entities;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name = "alt_code")
public class Code {
	
	private Long id;
	private String code;
	private CodeType codeType;
	private AdministrativeUnit location;
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	public Long getId() {
		return id;
	}
	
	public void setId(Long id) {
		this.id = id;
	}
	
	public String getCode() {
		return code;
	}
	
	public void setCode(String code) {
		this.code = code;
	}
	
	@ManyToOne
	@JoinColumn(name = "code_type_id", nullable = true)
	public CodeType getCodeType() {
		return codeType;
	}
	
	public void setCodeType(CodeType codeType) {
		this.codeType = codeType;
	}

	@ManyToOne
	@JoinColumn(name = "gid", nullable = true)
	public AdministrativeUnit getLocation() {
		return location;
	}

	public void setLocation(AdministrativeUnit location) {
		this.location = location;
	}
}
