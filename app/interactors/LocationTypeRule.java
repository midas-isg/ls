package interactors;

import java.util.List;

import dao.LocationTypeDao;
import dao.SuperTypeDao;
import dao.entities.LocationType;
import dao.entities.SuperType;

public class LocationTypeRule {
	public static List<SuperType> findSuperTypes() {
		return new SuperTypeDao().findAll();
	}

	public static List<LocationType> findAllBySuperTypeId(Long superTypeId) {
		return new LocationTypeDao().findAllBySuperTypeId(superTypeId);
	}

	public static LocationType findByName(String name) {
		LocationType type = new LocationTypeDao().findByName(name);
		return type;
	}

	public static LocationType findById(long id) {
		return new LocationTypeDao().read(id);
	}
}
