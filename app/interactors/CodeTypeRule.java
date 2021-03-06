package interactors;

import java.util.List;

import dao.CodeTypeDao;
import dao.entities.Code;
import dao.entities.CodeType;
import dao.entities.Location;
import play.db.jpa.JPA;

public class CodeTypeRule {

	private static final Long APOLLO_LOCATION_CODE_TYPE_ID = 14L;

	public static List<CodeType> findCodeTypes() {
		return new CodeTypeDao(JPA.em()).findAll();
	}

	public static List<String> getCodeTypeNames(List<Long> codeTypeIds) {
		CodeTypeDao codeTypeDao = new CodeTypeDao(JPA.em());
		return codeTypeDao.getCodeTypeNames(codeTypeIds);
	}

	public static Code createApolloLocationCode(Location location) {
		Code apolloLocationCode = new Code();
		apolloLocationCode.setCode(Long.toString(location.getGid()));
		CodeType AlcType = CodeTypeRule.read(APOLLO_LOCATION_CODE_TYPE_ID);
		apolloLocationCode.setCodeType(AlcType);
		apolloLocationCode.setLocation(location);

		return apolloLocationCode;
	}

	public static CodeType read(long id) {
		return new CodeTypeDao(JPA.em()).read(id);
	}

	public static CodeType findByName(String name) {
		return new CodeTypeDao(JPA.em()).findByName(name);
	}
}
