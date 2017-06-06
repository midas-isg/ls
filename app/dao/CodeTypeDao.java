package dao;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;

import dao.entities.CodeType;
import gateways.database.jpa.JpaAdaptor;

public class CodeTypeDao extends DataAccessObject<CodeType> {
	public CodeTypeDao(EntityManager em) {
		this(new JpaAdaptor(em));
	}

	private CodeTypeDao(JpaAdaptor jpaAdaptor) {
		super(CodeType.class, jpaAdaptor);
	}

	public List<String> getCodeTypeNames(List<Long> codeTypeIds) {
		if (codeTypeIds == null)
			return null;
		List<String> codeTypeNames = new ArrayList<>();
		CodeType codeType;
		for (Long id : codeTypeIds) {
			codeType = null;
			try {
				codeType = read(id);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
			if (codeType != null)
				codeTypeNames.add(codeType.getName());
		}
		return codeTypeNames;
	}
}
