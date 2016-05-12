package interactors;

import static interactors.Util.putAsStringIfNotNull;

import java.sql.Date;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import models.geo.Feature;
import models.geo.FeatureCollection;
import play.Logger;

import com.vividsolutions.jts.geom.Point;

import dao.LocationTypeDao;
import dao.entities.AltName;
import dao.entities.Code;
import dao.entities.Data;
import dao.entities.Location;
import dao.entities.LocationGeometry;
import dao.entities.LocationType;

public class GeoJsonHelperRule {
	static List<Location> wireLocationsIncluded(FeatureCollection fc,
			LocationType type) {
		LocationType allowType = type.getComposedOf();
		List<Location> locations = new ArrayList<>();
		for (Feature f : fc.getFeatures()) {
			String gid = f.getId();
			if (gid != null) {
				Location location = LocationRule.read(Long.parseLong(gid));
				if (location == null) {
					throw new RuntimeException(
							"Cannot find LocationIncluded gid=" + gid);
				}
				LocationType foundType = location.getData().getLocationType();
				if (allowType != null && !foundType.equals(allowType))
					throw new RuntimeException(foundType.getName()
							+ " type of gid=" + gid
							+ " is not allowed  to compose type "
							+ type.getName());
	
				locations.add(location);
			}
		}
		return locations;
	}

	static double[] getRepPoint(LocationGeometry geometry) {
		if (geometry == null)
			return null;
		Point repPoint = geometry.getRepPoint();
		if (repPoint == null)
			return null;
		return GeoOutputRule.toPoint(repPoint.getCoordinate());
	}

	static void putAsAltNameObjectsIfNotNull(
			Map<String, Object> properties, String key, Location location) {
		if (location == null)
			return;

		List<Map<String, String>> names = new ArrayList<>();
		final String KEY_NAME = "name";
		final String KEY_LANG = "language";
		final String KEY_DESC = "description";

		properties.put(key, names);

		List<AltName> otherNames = location.getAltNames();
		Map<String, String> anotherName;
		if (otherNames == null)
			return;
		for (AltName name : otherNames) {
			anotherName = new HashMap<>();
			anotherName.put(KEY_NAME, name.getName());
			anotherName.put(KEY_LANG, name.getLanguage());
			anotherName.put(KEY_DESC, name.getDescription());
			names.add(anotherName);
		}
	}

	static double[] computeBbox(Location l) {
		if (l == null)
			return null;
		LocationGeometry geometry = l.getGeometry();
		if (geometry == null)
			return null;
		return GeometryRule.computeBbox(geometry.getShapeGeom());
	}

	static double[] computeBbox(List<Feature> features) {
		if (features == null || features.isEmpty())
			return null;
		double[] result = new double[] { Double.POSITIVE_INFINITY,
				Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY,
				Double.NEGATIVE_INFINITY };
		for (Feature f : features) {
			double[] bbox = f.getBbox();
			if (bbox == null || bbox.length < 3)
				continue;
			if (result[0] > bbox[0])
				result[0] = bbox[0];
			if (result[1] > bbox[1])
				result[1] = bbox[1];
			if (result[2] < bbox[2])
				result[2] = bbox[2];
			if (result[3] < bbox[3])
				result[3] = bbox[3];
		}
		for (double d : result) {
			if (Double.isInfinite(d))
				return null;
		}
		return result;
	}

	static void putAsCodeObjectsIfNotNull(
			Map<String, Object> properties, String string, Location location) {
		if (location == null)
			return;

		List<Map<String, String>> codes = new ArrayList<>();
		Map<String, String> code = new HashMap<>();
		final String KEY_CODE = "code";
		final String KEY_TYPE = "codeTypeName";
		code.put(KEY_CODE, location.getData().getCode());
		code.put(KEY_TYPE, location.getData().getCodeType().getName());
		codes.add(code);
		properties.put(string, codes);

		List<Code> otherCodes = location.getOtherCodes();
		if (otherCodes == null)
			return;
		for (Code c : otherCodes) {
			Map<String, String> anotherCode = new HashMap<>();
			anotherCode.put(KEY_CODE, c.getCode());
			if(c.getCodeType() != null)
				anotherCode.put(KEY_TYPE, c.getCodeType().getName());
			codes.add(anotherCode);
		}
	}

	static void putAsLocationObjectsIfNotNull(
			Map<String, Object> properties, String key, List<Location> locations) {
		if (locations == null)
			return;
		Map<String, Object> locationProperties;
		List<Map<String, Object>> list = new ArrayList<>();
		for (Location l : locations) {
			locationProperties = toProperties(l);
			locationProperties.remove("headline");
			locationProperties.remove("rank");
			list.add(locationProperties);
		}
		properties.put(key, list);
	}
	
	static Map<String, Object> toProperties(Location location) {
		Map<String, Object> properties = new HashMap<>();
		LocationTypeDao locationTypeDao = new LocationTypeDao();
		String gid = getGid(location);
		putAsStringIfNotNull(properties, "gid", gid);
		Data data = location.getData();
		if (data == null) {
			Logger.warn(gid + " has null data!");
			return properties;
		}
		putAsStringIfNotNull(properties, "name", data.getName());
		putAsStringIfNotNull(properties, "locationDescription",
				data.getDescription());
		putAsStringIfNotNull(properties, "headline", location.getHeadline());
		putAsStringIfNotNull(properties, "rank", location.getRank());
		putAsStringIfNotNull(properties, "startDate", data.getStartDate());
		putAsStringIfNotNull(properties, "endDate", data.getEndDate());
		String locationTypeName = locationTypeDao.getLocationTypeName(
				data.getLocationType());
		putAsStringIfNotNull(properties, "locationTypeName", locationTypeName);
		Location parent = location.getParent();
		putAsStringIfNotNull(properties, "parentGid", getGid(parent));
		return properties;
	}

	static LocationType findLocationType(FeatureCollection fc) {
		String idKey = "locationTypeId";
		String id = getString(fc, idKey);
		String nameKey = "locationTypeName";
		String name = getString(fc, nameKey);
		if (id == null && name == null)
			throw new RuntimeException("undefined location type by id or name");
		LocationType type = null;

		try {
			if (id != null)
				type = LocationTypeRule.findById(Long.parseLong(id));
		} catch (Exception e) {
			Logger.info(e.getMessage(), e);
		}
		try {
			if (type == null)
				type = LocationTypeRule.findByName(name);
		} catch (Exception e2) {
			throw new RuntimeException("Requested location type not found: "
					+ idKey + "=" + id + ", " + nameKey + "=" + name);
		}
		return type;
	}

	static LocationGeometry createLocationGeometry(
			FeatureCollection fc, Location l) {
		LocationGeometry lg = new LocationGeometry();
		lg.setShapeGeom(GeoInputRule.toGeometry(fc));
		lg.setLocation(l);
		Date now = l.getData().getUpdateDate();
		lg.setUpdateDate(now);
		return lg;
	}

	static String getString(FeatureCollection fc, String key) {
		Object object = fc.getProperties().get(key);
		if (object == null)
			return null;
		return object.toString();
	}
	
	private static String getGid(Location location) {
		if (location == null)
			return null;
		return String.valueOf(location.getGid());
	}
}
