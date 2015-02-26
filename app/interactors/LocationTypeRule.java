package interactors;

import java.util.List;

import dao.LocationTypeDao;
import dao.entities.LocationType;

public class LocationTypeRule {
	public static LocationType findByName(String name) {
		LocationType type = new LocationTypeDao().findByName(name);
		return type;
	}

	public static List<LocationType> finaAllBySuperTypeName(String stName) {
		return new LocationTypeDao().finaAllBySuperTypeName(stName);
	}
}
