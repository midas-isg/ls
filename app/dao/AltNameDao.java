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

	public static List<AltName> toListOfAltName(List<String> otherNames) {
		List<AltName> altNames = new ArrayList<>();
		if (otherNames == null)
			return null;
		AltName alt;
		for (String n : otherNames) {
			alt = new AltName();
			alt.setName(n);
			altNames.add(alt);
		}
		return altNames;
	}

	public static void create(List<AltName> altNames) {
		if (altNames == null)
			return;
		for (AltName altName : altNames)
			create(altName);
	}

	private static void create(AltName altName) {
		EntityManager em = JPA.em();
		em.persist(altName);
	}

	public List<Long> delete(List<AltName> altNames) {
		if (altNames == null)
			return null;
		List<Long> ids = new ArrayList<>();
		for (AltName n : altNames) {
			Long id = delete(n);
			ids.add(id);
		}
		return ids;
	}

	private Long delete(AltName altName) {
		if (altName == null)
			return null;
		EntityManager em = JPA.em();
		Long id = altName.getId();
		em.remove(altName);
		return id;
	}
}
