package interactors;

import static interactors.RequestRule.isRequestedFeatureProperties;
import static interactors.Util.putAsStringIfNotNull;
import static models.FeatureKey.asFullPath;

import java.sql.Date;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.vividsolutions.jts.geom.Point;

import dao.ForestDao;
import dao.entities.AltName;
import dao.entities.CircleGeometry;
import dao.entities.Code;
import dao.entities.CodeType;
import dao.entities.Data;
import dao.entities.Forest;
import dao.entities.GisSource;
import dao.entities.Location;
import dao.entities.LocationGeometry;
import dao.entities.LocationType;
import dao.entities.SpewLink;
import gateways.configuration.ConfReader;
import models.FeatureKey;
import models.Request;
import models.geo.Circle;
import models.geo.Feature;
import models.geo.FeatureCollection;
import models.geo.FeatureGeometry;
import play.Logger;

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
			anotherName.put(FeatureKey.NAME, name.getName());
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
				link.put(FeatureKey.GID, spLink.getLocation().getGid());
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
		if (location.getData().getCode() != null)
			codes.add(getCodeAsMap(location));

		List<Code> otherCodes = location.getOtherCodes();
		if (otherCodes != null)
			for (Code c : otherCodes)
				codes.add(asMap(c));

		if (!codes.isEmpty())
			properties.put(string, codes);

	}

	private static Map<String, String> asMap(Code c) {
		Map<String, String> code = new HashMap<>();
		code.put(FeatureKey.CODE, c.getCode());
		String codeTypeName = null;
		if (c.getCodeType() != null)
			codeTypeName = c.getCodeType().getName();
		code.put(FeatureKey.CODE_TYPE_NAME, codeTypeName);
		return code;
	}

	private static Map<String, String> getCodeAsMap(Location location) {
		Map<String, String> code = new HashMap<>();
		code.put(FeatureKey.CODE, location.getData().getCode());
		CodeType codeType = location.getData().getCodeType();
		String codeTypeName = null;
		if (codeType != null)
			codeTypeName = codeType.getName();
		code.put(FeatureKey.CODE_TYPE_NAME, codeTypeName);

		return code;
	}

	static void putAsLocationObjectsIfNotNull(Map<String, Object> properties, String key, List<Location> locations) {
		if (locations == null)
			return;
		Map<String, Object> locationProperties;
		List<Map<String, Object>> list = new ArrayList<>();
		for (Location l : locations) {
			locationProperties = toProperties(l);
			locationProperties.remove(FeatureKey.HEADLINE);
			locationProperties.remove(FeatureKey.RANK);
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
		if (isRequestedFeatureProperties(req, asFullPath(FeatureKey.GID)))
			putAsStringIfNotNull(properties, FeatureKey.GID, gid);
		if (isRequestedFeatureProperties(req, asFullPath(FeatureKey.NAME)))
			putAsStringIfNotNull(properties, FeatureKey.NAME, data.getName());
		if (isRequestedFeatureProperties(req, asFullPath(FeatureKey.LOCATION_DESCRIPTION)))
			putAsStringIfNotNull(properties, FeatureKey.LOCATION_DESCRIPTION, data.getDescription());
		if (isRequestedFeatureProperties(req, asFullPath(FeatureKey.HEADLINE)))
			putAsStringIfNotNull(properties, FeatureKey.HEADLINE, location.getHeadline());
		if (isRequestedFeatureProperties(req, asFullPath(FeatureKey.RANK)))
			putAsStringIfNotNull(properties, FeatureKey.RANK, location.getRank());
		if (isRequestedFeatureProperties(req, asFullPath(FeatureKey.START_DATE)))
			putAsStringIfNotNull(properties, FeatureKey.START_DATE, data.getStartDate());
		if (isRequestedFeatureProperties(req, asFullPath(FeatureKey.END_DATE)))
			putAsStringIfNotNull(properties, FeatureKey.END_DATE, data.getEndDate());
		if (isRequestedFeatureProperties(req, asFullPath(FeatureKey.LOCATION_TYPE_NAME))) {
			String locationTypeName = LocationTypeRule.getLocationTypeName(data.getLocationType());
			putAsStringIfNotNull(properties, FeatureKey.LOCATION_TYPE_NAME, locationTypeName);
		}
		if (isRequestedFeatureProperties(req, asFullPath(FeatureKey.PARENT_GID))) {
			Location parent = location.getParent();
			putAsStringIfNotNull(properties, FeatureKey.PARENT_GID, getGid(parent));
		}
		if (isRequestedFeatureProperties(req, asFullPath(FeatureKey.MATCHED_TERM)))
			putAsStringIfNotNull(properties, FeatureKey.MATCHED_TERM, location.getMatchedTerm());
		return properties;
	}

	static LocationType findLocationType(FeatureCollection fc) {
		String id = getString(fc, FeatureKey.LOCATION_TYPE_ID);
		String name = getString(fc, FeatureKey.LOCATION_TYPE_NAME);
		if (id == null && name == null)
			throw new RuntimeException("undefined location type by id or name");
		LocationType type = null;

		try {
			if (id != null)
				type = LocationTypeRule.read(Long.parseLong(id));
		} catch (Exception e) {
			Logger.info(e.getMessage(), e);
		}
		try {
			if (type == null)
				type = LocationTypeRule.findByName(name);
		} catch (Exception e2) {
			throw new RuntimeException("Requested location type not found: " + FeatureKey.LOCATION_TYPE_ID + "=" + id
					+ ", " + FeatureKey.LOCATION_TYPE_NAME + "=" + name);
		}
		return type;
	}

	static CodeType findCodeType(String id, String name) {
		if (id == null && name == null)
			return null;

		CodeType type = null;
		try {
			if (id != null)
				type = CodeTypeRule.read(Long.parseLong(id));
		} catch (Exception e) {
			Logger.info(e.getMessage(), e);
		}
		try {
			if (type == null)
				type = CodeTypeRule.findByName(name);
		} catch (Exception e) {
			throw new RuntimeException("Requested code type not found: " + FeatureKey.CODE_TYPE_ID + "=" + id + ", "
					+ FeatureKey.CODE_TYPE_NAME + "=" + name);
		}
		return type;
	}

	static GisSource findOrCreateGisSource(String id, String url) {
		if (id == null && url == null)
			throw new RuntimeException(FeatureKey.GIS_SOURCE_URL + " or " + FeatureKey.GIS_SOURCE_ID + " is required.");

		GisSource source = null;
		try {
			if (id != null)
				source = GisSourceRule.read(Long.parseLong(id));
		} catch (Exception e) {
			Logger.info(e.getMessage(), e);
		}

		if (source == null)
			source = GisSourceRule.findByUrl(url);
		if (source == null) {
			source = createGisSource(url);
		}
		return source;
	}

	private static GisSource createGisSource(String url) {
		GisSource source;
		source = new GisSource();
		source.setUrl(url);
		source.setId(GisSourceRule.create(source));
		return source;
	}

	static LocationGeometry createLocationGeometry(FeatureCollection fc, Location l) {
		LocationGeometry lg = new LocationGeometry();
		lg.setShapeGeom(GeoInputRule.toGeometry(fc));
		lg.setLocation(l);
		Date now = l.getData().getUpdateDate();
		lg.setUpdateDate(now);
		setCircleGeometryIfIsCircle(fc, lg);
		return lg;
	}

	private static void setCircleGeometryIfIsCircle(FeatureCollection fc, LocationGeometry lg) {
		FeatureGeometry geometry = fc.getFeatures().get(0).getGeometry();
		if (geometry instanceof Circle) {
			CircleGeometry cg = toCircleGeometry((Circle) geometry);
			cg.setLocationGeometry(lg);
			lg.setCircleGeometry(cg);
		}
	}

	private static CircleGeometry toCircleGeometry(Circle circle) {
		CircleGeometry cg = new CircleGeometry();
		cg.setCenter(circle.getCenter());
		cg.setRadius(circle.getRadius());
		cg.setQuarterSegments(circle.getQuarterSegments());
		return cg;
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
