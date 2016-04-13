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
import dao.entities.AltName;
import dao.entities.GisSource;
import dao.entities.Location;

public class AltNameDao {

	public Map<Long, List<AltName>> getGid2OtherNames() {
		Map<Long, List<AltName>> result = new HashMap<>();
		EntityManager em = JPA.em();
		Session s = em.unwrap(Session.class);
		String query = "SELECT * FROM alt_name;";
		SQLQuery q = s.createSQLQuery(query);
		q.setResultTransformer(Transformers.ALIAS_TO_ENTITY_MAP);
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> resultlist = (List<Map<String, Object>>) q
				.list();
		AltName otherName;
		Long gid;
		for (Map<String, Object> n : resultlist) {
			gid = getLong(n, "gid");
			otherName = new AltName();
			otherName.setId(getLong(n, "id"));
			otherName.setName(getString(n, "name"));
			otherName.setDescription(getString(n, "description"));
			otherName.setLanguage(getString(n, "lang"));
			Location l = new Location();
			l.setGid(gid);
			otherName.setLocation(l);
			GisSource gisSrc = new GisSource();
			gisSrc.setId(getLong(n, "gis_src_id"));
			otherName.setGisSource(gisSrc);
			if (!result.containsKey(gid))
				result.put(gid, new ArrayList<AltName>());
			result.get(gid).add(otherName);
		}
		return result;
	}
}
