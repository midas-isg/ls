package interactors;

import java.util.List;

import dao.CodeTypeDao;
import dao.entities.CodeType;
import play.db.jpa.JPA;

public class CodeTypeRule {
	public static List<CodeType> findCodeTypes() {
		return new CodeTypeDao(JPA.em()).findAll();
	}

	public static List<String> getCodeTypeNames(List<Long> codeTypeIds) {
		CodeTypeDao codeTypeDao = new CodeTypeDao(JPA.em());
		return codeTypeDao.getCodeTypeNames(codeTypeIds);
	}
}
