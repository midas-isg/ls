package dao;

import static interactors.Util.getLong;
import static interactors.Util.getString;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;

import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.hibernate.transform.Transformers;

import play.db.jpa.JPA;
import dao.entities.Code;
import dao.entities.CodeType;
import dao.entities.Location;

public class CodeDao {
	public Map<Long, List<Code>> getGid2OtherCodes() {
		Map<Long, List<Code>> result = new HashMap<>();
		EntityManager em = JPA.em();
		Session s = em.unwrap(Session.class);
		String query = "SELECT * FROM alt_code;";
		SQLQuery q = s.createSQLQuery(query);
		q.setResultTransformer(Transformers.ALIAS_TO_ENTITY_MAP);
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> resultlist = (List<Map<String, Object>>) q
				.list();
		Code otherCode;
		CodeTypeDao codeTypeDao = new CodeTypeDao();
		Long gid;
		for (Map<String, Object> n : resultlist) {
			gid = getLong(n, "gid");
			otherCode = new Code();
			otherCode.setId(getLong(n, "id"));
			otherCode.setCode(getString(n, "code"));
			CodeType type = codeTypeDao.read(getLong(n, "code_type_id"));
			otherCode.setCodeType(type);
			Location l = new Location();
			l.setGid(gid);
			otherCode.setLocation(l);
			if (!result.containsKey(gid))
				result.put(gid, new ArrayList<Code>());
			result.get(gid).add(otherCode);
		}
		return result;
	}

	public static List<Code> toListOfCodes(List<String> otherCodes) {
		List<Code> codes = new ArrayList<>();
		if (otherCodes == null)
			return null;
		Code code;
		for (String c : otherCodes) {
			code = new Code();
			code.setCode(c);
			codes.add(code);
		}
		return codes;
	}

	public static void create(List<Code> codes) {
		if (codes == null)
			return;
		for (Code c : codes)
			create(c);
	}

	private static void create(Code c) {
		EntityManager em = JPA.em();
		em.persist(c);
	}

	public List<Long> delete(List<Code> codes) {
		if (codes == null)
			return null;
		List<Long> ids = new ArrayList<>();
		for (Code c : codes) {
			Long id = delete(c);
			ids.add(id);
		}
		return ids;
	}

	private Long delete(Code code) {
		if (code == null)
			return null;
		EntityManager em = JPA.em();
		Long id = code.getId();
		em.remove(code);
		return id;
	}
}
