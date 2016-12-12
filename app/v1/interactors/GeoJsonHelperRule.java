package v1.interactors;

import static v1.interactors.RequestRule.isRequestedFeatureProperties;
import static v1.interactors.Util.putAsStringIfNotNull;

import java.sql.Date;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.vividsolutions.jts.geom.Point;

import play.Logger;
import v1.dao.ForestDao;
import v1.dao.LocationTypeDao;
import v1.dao.entities.AltName;
import v1.dao.entities.Code;
import v1.dao.entities.Data;
import v1.dao.entities.Forest;
import v1.dao.entities.Location;
import v1.dao.entities.LocationGeometry;
import v1.dao.entities.LocationType;
import v1.dao.entities.SpewLink;
import v1.gateways.configuration.ConfReader;
import v1.models.FeatureKey;
import v1.models.Request;
import v1.models.geo.Feature;
import v1.models.geo.FeatureCollection;

public class GeoJsonHelperRule {
	
	private static final String KEY_SPEW_URL = "url";
	private static final String SPEW_BASE_URL_CONF_KEY = "spew.base.url";
	
	static List<Location> wireLocationsIncluded(FeatureCollection fc, LocationType type) {
		LocationType allowType = type.getComposedOf();
		List<Location> locations = new ArrayList<>();
		for (Feature f : fc.getFeatures()) {
			String gid = f.getId();
			if (gid != null) {
				Location location = LocationRule.read(Long.parseLong(gid));
				if (location == null) {
					throw new RuntimeException("Cannot find LocationIncluded gid=" + gid);
				}
				LocationType foundType = location.getData().getLocationType();
				if (allowType != null && !foundType.equals(allowType))
					throw new RuntimeException(foundType.getName() + " type of gid=" + gid
							+ " is not allowed  to compose type " + type.getName());

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

	static void putAsAltNameObjectsIfNotNull(Map<String, Object> properties, String key, Location location) {
		if (location == null)
			return;

		List<Map<String, String>> names = new ArrayList<>();
		final String KEY_LANG = "language";
		final String KEY_DESC = "description";

		properties.put(key, names);

		List<AltName> otherNames = location.getAltNames();
		Map<String, String> anotherName;
		if (otherNames == null)
			return;
		for (AltName name : otherNames) {
			anotherName = new HashMap<>();
			anotherName.put(FeatureKey.NAME.valueOf(), name.getName());
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
		double[] result = new double[] { Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY,
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

	static void putAsSpewLinkObjectsIfNotNull(Map<String, Object> properties, String key, Location location) {
		if (location == null)
			return;

		List<Forest> forest = new ForestDao().findByChildALC(location.getGid());
		List<SpewLink> spLinks = findSpewLinks(forest);
		if (spLinks == null || spLinks.isEmpty())
			return;
		String spewBaseUrl = readSpewBaseUrl();
		putSpewLinkObjects(properties, key, spewBaseUrl, spLinks);
	}

	static void putSpewLinkObjects(Map<String, Object> properties, String key, String spewBaseUrl,
			List<SpewLink> spLinks) {
		if (spLinks == null)
			return;
		List<Map<String, Object>> links = new ArrayList<>();
		for (SpewLink spLink : spLinks) {
			Map<String, Object> link = new HashMap<>();
			link.put(KEY_SPEW_URL, spewBaseUrl + spLink.getUrl());
			if (spLink.getLocation() != null)
				link.put(FeatureKey.GID.valueOf(), spLink.getLocation().getGid());
			links.add(link);
		}
		properties.put(key, links);
	}

	static List<SpewLink> findSpewLinks(List<Forest> forest) {
		if (forest == null)
			return null;
		List<SpewLink> spLinks = new ArrayList<>();
		List<SpewLink> sp;
		for (Forest f : forest) {
			Location root = f.getRoot();
			sp = root.getSpewLinks();
			if (sp != null)
				spLinks.addAll(sp);
		}
		return spLinks;
	}

	private static String readSpewBaseUrl() {
		ConfReader reader = new ConfReader();
		String spewBaseUrl = reader.readString(SPEW_BASE_URL_CONF_KEY);
		return spewBaseUrl;
	}

	static void putAsCodeObjectsIfNotNull(Map<String, Object> properties, String string, Location location) {
		if (location == null)
			return;

		List<Map<String, String>> codes = new ArrayList<>();
		Map<String, String> code = new HashMap<>();
		code.put(FeatureKey.CODE.valueOf(), location.getData().getCode());
		code.put(FeatureKey.CODE_TYPE_NAME.valueOf(), location.getData().getCodeType().getName());
		codes.add(code);
		properties.put(string, codes);

		List<Code> otherCodes = location.getOtherCodes();
		if (otherCodes == null)
			return;
		for (Code c : otherCodes) {
			Map<String, String> anotherCode = new HashMap<>();
			anotherCode.put(FeatureKey.CODE.valueOf(), c.getCode());
			if (c.getCodeType() != null)
				anotherCode.put(FeatureKey.CODE_TYPE_NAME.valueOf(), c.getCodeType().getName());
			codes.add(anotherCode);
		}
	}

	static void putAsLocationObjectsIfNotNull(Map<String, Object> properties, String key, List<Location> locations) {
		if (locations == null)
			return;
		Map<String, Object> locationProperties;
		List<Map<String, Object>> list = new ArrayList<>();
		for (Location l : locations) {
			locationProperties = toProperties(l);
			locationProperties.remove(FeatureKey.HEADLINE.valueOf());
			locationProperties.remove(FeatureKey.RANK.valueOf());
			list.add(locationProperties);
		}
		properties.put(key, list);
	}

	static Map<String, Object> toProperties(Location location) {
		return toProperties(location, new Request());
	}

	static Map<String, Object> toProperties(Location location, Request req) {
		Map<String, Object> properties = new HashMap<>();
		String gid = getGid(location);
		Data data = location.getData();
		if (data == null) {
			Logger.warn(gid + " has null data!");
			return properties;
		}
		if (isRequestedFeatureProperties(req, toPropertiesPath(FeatureKey.GID.valueOf())))
			putAsStringIfNotNull(properties, FeatureKey.GID.valueOf(), gid);
		if (isRequestedFeatureProperties(req, toPropertiesPath(FeatureKey.NAME.valueOf())))
			putAsStringIfNotNull(properties, FeatureKey.NAME.valueOf(), data.getName());
		if (isRequestedFeatureProperties(req, toPropertiesPath(FeatureKey.LOCATION_DESCRIPTION.valueOf())))
			putAsStringIfNotNull(properties, FeatureKey.LOCATION_DESCRIPTION.valueOf(), data.getDescription());
		if (isRequestedFeatureProperties(req, toPropertiesPath(FeatureKey.HEADLINE.valueOf())))
			putAsStringIfNotNull(properties, FeatureKey.HEADLINE.valueOf(), location.getHeadline());
		if (isRequestedFeatureProperties(req, toPropertiesPath(FeatureKey.RANK.valueOf())))
			putAsStringIfNotNull(properties, FeatureKey.RANK.valueOf(), location.getRank());
		if (isRequestedFeatureProperties(req, toPropertiesPath(FeatureKey.START_DATE.valueOf())))
			putAsStringIfNotNull(properties, FeatureKey.START_DATE.valueOf(), data.getStartDate());
		if (isRequestedFeatureProperties(req, toPropertiesPath(FeatureKey.END_DATE.valueOf())))
			putAsStringIfNotNull(properties, FeatureKey.END_DATE.valueOf(), data.getEndDate());
		if (isRequestedFeatureProperties(req, toPropertiesPath(FeatureKey.LOCATION_TYPE_NAME.valueOf()))) {
			LocationTypeDao locationTypeDao = new LocationTypeDao();
			String locationTypeName = locationTypeDao.getLocationTypeName(data.getLocationType());
			putAsStringIfNotNull(properties, FeatureKey.LOCATION_TYPE_NAME.valueOf(), locationTypeName);
		}
		if (isRequestedFeatureProperties(req, toPropertiesPath(FeatureKey.PARENT_GID.valueOf()))) {
			Location parent = location.getParent();
			putAsStringIfNotNull(properties, FeatureKey.PARENT_GID.valueOf(), getGid(parent));
		}
		if (isRequestedFeatureProperties(req, toPropertiesPath(FeatureKey.MATCHED_TERM.valueOf())))
			putAsStringIfNotNull(properties,FeatureKey.MATCHED_TERM.valueOf(), location.getMatchedTerm());
		return properties;
	}

	static String toPropertiesPath(String key) {
		return FeatureKey.PROPERTIES.valueOf() + "." + key;
	}

	static LocationType findLocationType(FeatureCollection fc) {
		String id = getString(fc, FeatureKey.LOCATION_TYPE_ID.valueOf());
		String name = getString(fc, FeatureKey.LOCATION_TYPE_NAME.valueOf());
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
			throw new RuntimeException(
					"Requested location type not found: " + FeatureKey.LOCATION_TYPE_ID.valueOf() + "=" 
							+ id + ", " + FeatureKey.LOCATION_TYPE_NAME.valueOf() + "=" + name);
		}
		return type;
	}

	static LocationGeometry createLocationGeometry(FeatureCollection fc, Location l) {
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
