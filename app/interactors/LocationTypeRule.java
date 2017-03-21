package interactors;

import java.util.List;

import dao.LocationTypeDao;
import dao.SuperTypeDao;
import dao.entities.LocationType;
import dao.entities.SuperType;
import play.db.jpa.JPA;

public class LocationTypeRule {
	public static List<SuperType> findSuperTypes() {
		return new SuperTypeDao(JPA.em()).findAll();
	}

	public static List<LocationType> findAllBySuperTypeId(Long superTypeId) {
		return new LocationTypeDao(JPA.em()).findAllBySuperTypeId(superTypeId);
	}

	public static LocationType findByName(String name) {
		LocationType type = new LocationTypeDao(JPA.em()).findByName(name);
		return type;
	}

	public static LocationType read(long id) {
		return new LocationTypeDao(JPA.em()).read(id);
	}

	public static List<String> getLocationTypeNames(List<Long> locationTypeIds) {
		LocationTypeDao locationTypeDao = new LocationTypeDao(JPA.em());
		return locationTypeDao.getLocationTypeNames(locationTypeIds);
	}

	public static String getLocationTypeName(LocationType locationType) {
		LocationTypeDao locationTypeDao = new LocationTypeDao(JPA.em());
		return locationTypeDao.getLocationTypeName(locationType);
	}

	public static long create(LocationType type) {
		return new LocationTypeDao(JPA.em()).create(type);
	}
}
