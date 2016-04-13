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
}
