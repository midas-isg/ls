package interactors;

import static interactors.GeoJsonHelperRule.computeBbox;
import static interactors.GeoJsonHelperRule.createLocationGeometry;
import static interactors.GeoJsonHelperRule.findLocationType;
import static interactors.GeoJsonHelperRule.getRepPoint;
import static interactors.GeoJsonHelperRule.getString;
import static interactors.GeoJsonHelperRule.putAsAltNameObjectsIfNotNull;
import static interactors.GeoJsonHelperRule.putAsCodeObjectsIfNotNull;
import static interactors.GeoJsonHelperRule.putAsLocationObjectsIfNotNull;
import static interactors.GeoJsonHelperRule.toProperties;
import static interactors.GeoJsonHelperRule.wireLocationsIncluded;
import static interactors.Util.containsKey;
import static interactors.Util.getNowDate;
import static interactors.Util.includeField;
import static interactors.Util.listToString;
import static interactors.Util.newDate;
import static interactors.Util.putAsStringIfNotNull;
import static interactors.Util.toDate;
import static interactors.Util.toListOfInt;
import static interactors.Util.toStringValue;

import java.sql.Date;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import models.Request;
import models.Response;
import models.exceptions.BadRequest;
import models.geo.Feature;
import models.geo.FeatureCollection;
import models.geo.FeatureGeometry;
import play.Logger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.vividsolutions.jts.geom.Geometry;

import dao.LocationTypeDao;
import dao.entities.Data;
import dao.entities.Location;
import dao.entities.LocationGeometry;
import dao.entities.LocationType;

public class GeoJsonRule {
	private static final String KEY_PROPERTIES = "properties";
	private static final String KEY_CHILDREN = "children";
	private static final String KEY_BBOX = "bbox";
	private static final String KEY_REPPOINT = "rep_point";
	private static final String KEY_GEOMETRY = "geometry";
	public static final List<String> DEFAULT_KEYS = Arrays.asList(new String[] {
			KEY_PROPERTIES, KEY_CHILDREN });

	public static FeatureCollection asFeatureCollection(Location location) {
		long locationTypeId = location.getData().getLocationType().getId();
		if (locationTypeId == LocationRule.PUMA_TYPE_ID
				|| locationTypeId == LocationRule.COMPOSITE_LOCATION_ID) {
			return toFeatureCollection(location);
		}
		List<Location> list = new ArrayList<>();
		list.add(location);
		return toFeatureCollection(list, null);
	}

	public static FeatureCollection toFeatureCollection(
			List<Location> locations, List<String> fields) {
		FeatureCollection fc = new FeatureCollection();
		List<Feature> features = toFeatures(locations, fields);
		fc.setFeatures(features);
		fc.setType("FeatureCollection");
		fc.setBbox(computeBbox(features));
		return fc;
	}

	private static FeatureCollection toFeatureCollection(Location composite) {
		FeatureCollection fc = new FeatureCollection();
		List<Location> locationsIncluded = composite.getLocationsIncluded();
		List<Feature> features = toFeatures(locationsIncluded, null);
		fc.setFeatures(features);
		fc.setType("FeatureCollection");
		fc.setProperties(toPropertiesOfFeature(composite, false));
		double[] computeBbox = computeBbox(composite);
		if (computeBbox == null)
			computeBbox = computeBbox(features);
		fc.setBbox(computeBbox);
		fc.setId(composite.getGid() + "");
		return fc;
	}

	private static List<Feature> toFeatures(List<Location> locations,
			List<String> fields) {
		List<Feature> features = new ArrayList<>();
		for (Location location : locations) {
			if (location == null) {
				Logger.warn("toFeatures got an element in the list as null.");
				continue;
			}
			features.add(toFeature(location, fields));
		}

		return features;
	}

	private static Feature toFeature(Location location, List<String> fields) {
		Feature feature = new Feature();
		if (includeField(fields, KEY_PROPERTIES, KEY_CHILDREN)) {
			Map<String, Object> properties = toPropertiesOfFeature(location,
					true);
			feature.setProperties(properties);
		} else if (includeField(fields, KEY_PROPERTIES)) {
			Map<String, Object> properties = toPropertiesOfFeature(location,
					false);
			feature.setProperties(properties);
		}
		LocationGeometry geometry = null;
		if (includeField(fields, KEY_GEOMETRY)) {
			final LocationGeometry locationGeometry = location.getGeometry();
			if (locationGeometry == null)
				geometry = GeometryRule.read(location.getGid());
			else
				geometry = locationGeometry;
		}

		if (geometry != null) {
			Geometry multiPolygonGeom = geometry.getShapeGeom();
			feature.setGeometry(GeoOutputRule
					.toFeatureGeometry(multiPolygonGeom));
			if (includeField(fields, KEY_BBOX))
				feature.setBbox(computeBbox(location));
			if (includeField(fields, KEY_REPPOINT))
				feature.setRepPoint(getRepPoint(geometry));
			feature.setId(location.getGid() + "");
		}

		return feature;
	}

	private static FeatureCollection toGeoJSON(Request req,
			List<Location> result) {
		List<String> fields = Arrays.asList(new String[] { KEY_PROPERTIES });
		FeatureCollection geoJSON = toFeatureCollection(result, fields);
		Map<String, Object> properties = new HashMap<>();
		LocationTypeDao locationTypeDao = new LocationTypeDao();
		geoJSON.setProperties(properties);
		properties.put("queryTerm", req.getQueryTerm());
		putAsStringIfNotNull(properties, "locationTypeIds",
				listToString(req.getTypeId()));
		putAsStringIfNotNull(properties, "locationTypeNames",
				listToString(locationTypeDao.getLocationTypeNames(req
						.getTypeId())));
		putAsStringIfNotNull(properties, "start", toStringValue(req.getStart()));
		putAsStringIfNotNull(properties, "end", toStringValue(req.getEnd()));
		putAsStringIfNotNull(properties, "limit", "" + req.getLimit());
		putAsStringIfNotNull(properties, "offset", "" + req.getOffset());
		putAsStringIfNotNull(properties, "ignoreAccent", req.isIgnoreAccent());
		putAsStringIfNotNull(properties, "searchNames", req.isSearchNames());
		putAsStringIfNotNull(properties, "searchOtherNames",
				req.isSearchOtherNames());
		putAsStringIfNotNull(properties, "searchCodes", req.isSearchCodes());
		putAsStringIfNotNull(properties, "resultSize", "" + result.size());
		return geoJSON;
	}

	private static Map<String, Object> toPropertiesOfFeature(Location location,
			boolean includeChildren) {
		Map<String, Object> properties = toProperties(location);
		if (includeChildren) {
			List<Location> children = location.getChildren();
			if (children != null)
				Collections.sort(children);
			putAsLocationObjectsIfNotNull(properties, "children", children);
		}
		putAsLocationObjectsIfNotNull(properties, "lineage",
				LocationProxyRule.getLineage(location.getGid()));
		putAsLocationObjectsIfNotNull(properties, "related",
				location.getRelatedLocations());
		putAsCodeObjectsIfNotNull(properties, "codes", location);
		putAsAltNameObjectsIfNotNull(properties, "otherNames", location);
		putAsStringIfNotNull(properties, "kml", location.getData().getKml());
		return properties;
	}

	public static Location asLocation(FeatureCollection fc) {
		Location location = new Location();
		Data data = new Data();
		LocationType type = findLocationType(fc);
		data.setLocationType(type);
		Date now = getNowDate();
		data.setCodeType(LocationRule.getIsgCodeType());
		data.setGisSource(LocationRule.getAlsGisSource());
		String name = getString(fc, "name");
		data.setName(name);
		data.setDescription(getString(fc, "locationDescription"));
		data.setKml(getString(fc, "kml"));
		Date startDate = newDate(getString(fc, "startDate"));
		data.setStartDate(startDate);
		Date endDate = newDate(getString(fc, "endDate"));
		data.setEndDate(endDate);
		data.setUpdateDate(now);
		String code = getString(fc, "code");
		data.setCode(code);
		location.setData(data);
		String parentGid = getString(fc, "parentGid");
		Location parent = LocationRule.read(Long.parseLong(parentGid));
		if (parent == null) {
			throw new RuntimeException("Cannot find parent gid=" + parentGid);
		}
		location.setParent(parent);
		if (type.getSuperType().getId().equals(2L)) {
			List<Location> locations = wireLocationsIncluded(fc, type);
			location.setLocationsIncluded(locations);
		}
		location.setGeometry(createLocationGeometry(fc, location));
		return location;
	}

	public static FeatureGeometry asFetureGeometry(FeatureCollection fc) {
		Geometry geometry = GeoInputRule.toGeometry(fc);
		return GeoOutputRule.toFeatureGeometry(geometry);
	}
	
	public static Response findByName(String queryTerm, Integer limit,
			Integer offset, Boolean searchOtherNames) {
		Request req = toFindByNameRequest(queryTerm, searchOtherNames, limit, offset);
		List<Location> result = LocationRule.findByTerm(req);
		Response response = new Response();
		response.setGeoJSON(toFeatureCollection(result, DEFAULT_KEYS));
		Map<String, Object> properties = new HashMap<>();
		response.setProperties(properties);
		properties.put("queryTerm", queryTerm);
		putAsStringIfNotNull(properties, "limit", limit);
		putAsStringIfNotNull(properties, "offset", offset);
		putAsStringIfNotNull(properties, "searchOtherNames", searchOtherNames);
		putAsStringIfNotNull(properties, "resultSize", "" + result.size());
		// properties.put("locationTypeName", "Result from a query");
		String descritpion = "Result from the query for '" + queryTerm
				+ "' limit=" + limit + " offset=" + offset
				+ " searchOtherNames=" + searchOtherNames;
		properties.put("locationDescription", descritpion);
		return response;
	}

	public static List<Object> findByTerm(ArrayNode queryArray) {
		List<Object> result = new ArrayList<>();
		FeatureCollection fc;
		for (JsonNode query : queryArray) {
			Request req = toFindByTermRequest(query);
			fc = findByTerm(req);
			result.add(fc);
		}
		return result;
	}
	public static FeatureCollection findByTerm(Request req) {
		List<Location> result = LocationRule.findByTerm(req);
		return toGeoJSON(req, result);
	}

	public static Response findByPoint(double latitude, double longitude) {
		List<Location> result = LocationRule.findByPoint(latitude, longitude);
		Response response = new Response();
		response.setGeoJSON(toFeatureCollection(result, DEFAULT_KEYS));
		Map<String, Object> properties = new HashMap<>();
		response.setProperties(properties);
		putAsStringIfNotNull(properties, "latitude", latitude);
		putAsStringIfNotNull(properties, "longitude", longitude);
		putAsStringIfNotNull(properties, "resultSize", "" + result.size());
		properties.put("locationTypeName", "Result from a query");
		String descritpion = "Result from the query for latitude=" + latitude
				+ " longitude=" + longitude;
		properties.put("locationDescription", descritpion);
		return response;
	}

	private static Request toFindByNameRequest(String name, Boolean searchOtherNames,
			Integer limit, Integer offset) {
		Request req = new Request();
		req.setQueryTerm(name);
		req.setSearchNames(true);
		req.setSearchOtherNames(searchOtherNames);
		req.setLimit(limit);
		req.setOffset(offset);
		return req;
	}

	private static Request toFindByTermRequest(JsonNode node) {
		Request req = new Request();
		Boolean value;
		if (containsKey(node, "queryTerm"))
			req.setQueryTerm(node.get("queryTerm").asText());
		else
			throw new BadRequest("\"" + "queryTerm" + "\" key is requierd!");
		if (containsKey(node, "locationTypeIds"))
			req.setTypeId(toListOfInt((JsonNode) node.get("locationTypeIds")));
		if (containsKey(node, "start"))
			req.setStart(toDate(node.get("start").asText()));
		if (containsKey(node, "end"))
			req.setEnd(toDate(node.get("end").asText()));
		if (containsKey(node, "limit"))
			req.setLimit(node.get("limit").asInt());
		if (containsKey(node, "offset"))
			req.setOffset(node.get("offset").asInt());
		value = returnDefaultIfKeyNotExists(node, "ignoreAccent", true);
		req.setIgnoreAccent(value);
		value = returnDefaultIfKeyNotExists(node, "searchNames", true);
		req.setSearchNames(value);
		value = returnDefaultIfKeyNotExists(node, "searchOtherNames", true);
		req.setSearchOtherNames(value);
		value = returnDefaultIfKeyNotExists(node, "searchCodes", true);
		req.setSearchCodes(value);
		return req;
	}

	private static Boolean returnDefaultIfKeyNotExists(JsonNode node,
			String key, Boolean defaultValue) {
		if (containsKey(node, key))
			return node.get(key).asBoolean();
		else
			return defaultValue;
	}

	/**
	 * @deprecated  replaced by {@link #findByTerm(ArrayNode arrayNode)}
	 */
	@Deprecated	public static List<Object> findBulk(ArrayNode arrayNode) {
		List<Object> result = new ArrayList<>();
		FeatureCollection fc;
		for (JsonNode node : arrayNode) {
			Request req = toFindBulkRequest(node);
			fc = findByTerm(req);
			result.add(fc);
		}
		return result;
	}

	/**
	 * @deprecated  replaced by {@link #toFindByTermRequest(JsonNode node)}
	 */
	@Deprecated	private static Request toFindBulkRequest(JsonNode node) {
		Request req = new Request();
		if (containsKey(node, "name"))
			req.setQueryTerm(node.get("name").asText());
		else
			throw new BadRequest("\"" + "name" + "\" key is requierd!");
		if (containsKey(node, "locationTypeIds"))
			req.setTypeId(toListOfInt((JsonNode) node.get("locationTypeIds")));
		if (containsKey(node, "start"))
			req.setStart(toDate(node.get("start").asText()));
		if (containsKey(node, "end"))
			req.setEnd(toDate(node.get("end").asText()));
		if (containsKey(node, "limit"))
			req.setLimit(node.get("limit").asInt());
		if (containsKey(node, "offset"))
			req.setOffset(node.get("offset").asInt());
		if (containsKey(node, "ignoreAccent"))
			req.setIgnoreAccent(node.get("ignoreAccent").asBoolean());
		if (containsKey(node, "searchNames"))
			req.setSearchNames(node.get("searchNames").asBoolean());
		if (containsKey(node, "searchOtherNames"))
			req.setSearchOtherNames(node.get("searchOtherNames").asBoolean());
		if (containsKey(node, "searchCodes"))
			req.setSearchCodes(node.get("searchCodes").asBoolean());
		return req;
	}
}
