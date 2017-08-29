package interactors;

import dao.GisSourceDao;
import dao.entities.GisSource;
import play.db.jpa.JPA;

public class GisSourceRule {
	public static GisSource read(long id) {
		GisSourceDao gsd = new GisSourceDao(JPA.em());
		return gsd.read(id);
	}

	public static long create(GisSource s) {
		GisSourceDao gsd = new GisSourceDao(JPA.em());
		return gsd.create(s);
	}

	public static GisSource findByUrl(String url) {
		return new GisSourceDao(JPA.em()).findByUrl(url);
	}
}
