package v1.interactors;

import static v1.interactors.GeoJsonHelperRule.*;
import static v1.interactors.RequestRule.isRequestedFeatureField;
import static v1.interactors.RequestRule.isRequestedFeatureProperties;
import static v1.interactors.Util.*;

import java.math.BigInteger;
import java.sql.Date;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.vividsolutions.jts.geom.Geometry;

import play.Logger;
import v1.dao.LocationTypeDao;
import v1.dao.entities.AltName;
import v1.dao.entities.Code;
import v1.dao.entities.Data;
import v1.dao.entities.Location;
import v1.dao.entities.LocationGeometry;
import v1.dao.entities.LocationType;
import v1.models.FeatureKey;
import v1.models.Request;
import v1.models.exceptions.BadRequest;
import v1.models.geo.Feature;
import v1.models.geo.FeatureCollection;
import v1.models.geo.FeatureGeometry;

public class GeoJsonRule {

	public static FeatureCollection asFeatureCollection(Location location, Request req) {
		long locationTypeId = location.getData().getLocationType().getId();
		if (locationTypeId == LocationRule.PUMA_TYPE_ID
				|| locationTypeId == LocationRule.COMPOSITE_LOCATION_ID) {
			return toFeatureCollection(location, req);
		}
		List<Location> list = new ArrayList<>();
		list.add(location);
		return toFeatureCollection(list, req);
	}

	public static FeatureCollection toFeatureCollection(List<Location> locations, Request req) {
		FeatureCollection fc = new FeatureCollection();
		List<Feature> features = toFeatures(locations, req);
		fc.setFeatures(features);
		fc.setType("FeatureCollection");
		fc.setBbox(computeBbox(features));
		return fc;
	}

	private static FeatureCollection toFeatureCollection(Location composite, Request req) {
		FeatureCollection fc = new FeatureCollection();
		List<Location> locationsIncluded = composite.getLocationsIncluded();
		List<Feature> features = toFeatures(locationsIncluded, req);
		fc.setFeatures(features);
		fc.setType("FeatureCollection");
		if(req == null)
		 req = new Request();
		if(req.getExcludedFeatureFields() == null)
			req.setExcludedFeatureFields(Arrays.asList(new String[] { toPropertiesPath(FeatureKey.CHILDREN.valueOf()) }));
		if(isRequestedFeatureField(req, FeatureKey.PROPERTIES.valueOf()))
			fc.setProperties(toPropertiesOfFeature(composite, req));
		
		double[] computeBbox = computeBbox(composite);
		if (computeBbox == null)
			computeBbox = computeBbox(features);
		fc.setBbox(computeBbox);
		fc.setId(composite.getGid() + "");
		
		return fc;
	}

	private static List<Feature> toFeatures(List<Location> locations, Request req) {
		List<Feature> features = new ArrayList<>();
		for (Location location : locations) {
			if (location == null) {
				Logger.warn("toFeatures got an element in the list as null.");
				continue;
			}
			features.add(toFeature(location, req));
		}

		return features;
	}

	private static Feature toFeature(Location location, Request req) {
		Feature feature = new Feature();
		Map<String, Object> properties = new HashMap<>();
		if(RequestRule.isPropertiesRequested(req)){
			properties = toPropertiesOfFeature(location, req);
			feature.setProperties(properties);
		}
		LocationGeometry geometry = null;
		if(isRequestedFeatureField(req, FeatureKey.GEOMETRY.valueOf())) {
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
			feature.setId(location.getGid() + "");
		}
		if(isRequestedFeatureField(req, toPropertiesPath(FeatureKey.BBOX.valueOf()))) {
			geometry = readGeometryIfNullOrEmpty(location, geometry); //TODO: read only bbox instead of whole record
			feature.setBbox(GeometryRule.computeBbox(geometry.getShapeGeom()));
		}
		
		if(isRequestedFeatureField(req, toPropertiesPath(FeatureKey.REPPOINT.valueOf()))) {
			geometry = readGeometryIfNullOrEmpty(location, geometry); //TODO: read only repPoint instead of whole record
			feature.setRepPoint(getRepPoint(geometry));
		}
		
		return feature;
	}

	private static LocationGeometry readGeometryIfNullOrEmpty(Location location, LocationGeometry geometry) {
		if (geometry == null || geometry.getShapeGeom() == null)
			geometry = GeometryRule.read(location.getGid());
		return geometry;
	}

	private static FeatureCollection toFeatureCollection(Request req, List<Location> result) {
		FeatureCollection geoJSON = toFeatureCollection(result, req);
		Map<String, Object> properties = toProperties(req, result.size());
		geoJSON.setProperties(properties);
		return geoJSON;
	}

	private static Map<String, Object> toProperties(Request req, int resultSize) {
		Map<String, Object> properties = new HashMap<>();
		LocationTypeDao locationTypeDao = new LocationTypeDao();
		putAsStringIfNotNull(properties, "queryTerm", req.getQueryTerm());
		putAsStringIfNotNull(properties, "locationTypeIds",
				listToString(req.getLocationTypeIds()));
		putAsStringIfNotNull(properties, "locationTypeNames",
				listToString(locationTypeDao.getLocationTypeNames(req
						.getLocationTypeIds())));
		putAsStringIfNotNull(properties, "startDate",
				toStringValue(req.getStartDate()));
		putAsStringIfNotNull(properties, "endDate",
				toStringValue(req.getEndDate()));
		putAsStringIfNotNull(properties, "limit", req.getLimit());
		putAsStringIfNotNull(properties, "offset", req.getOffset());
		putAsStringIfNotNull(properties, "ignoreAccent", req.isIgnoreAccent());
		putAsStringIfNotNull(properties, "searchNames", req.isSearchNames());
		putAsStringIfNotNull(properties, "searchOtherNames",
				req.isSearchOtherNames());
		putAsStringIfNotNull(properties, "searchCodes", req.isSearchCodes());
		putAsStringIfNotNull(properties, "rootALC", req.getRootALC());
		putAsStringIfNotNull(properties, "includeOnly",
				listToString(req.getOnlyFeatureFields()));
		putAsStringIfNotNull(properties, "resultSize", resultSize);
		putAsStringIfNotNull(properties, "fuzzyMatch", req.isFuzzyMatch());
		if (isTrue(req.isFuzzyMatch()))
			putAsStringIfNotNull(properties, "fuzzyMatchThreshold", req.getFuzzyMatchThreshold());
		return properties;
	}

	private static Map<String, Object> toPropertiesOfFeature(Location location, Request req) {
		Map<String, Object> properties = GeoJsonHelperRule.toProperties(location, req);
		if(isRequestedFeatureProperties(req, toPropertiesPath(FeatureKey.CHILDREN.valueOf()))) {
				List<Location> children = location.getChildren();
				if (children != null)
					Collections.sort(children);
				putAsLocationObjectsIfNotNull(properties, FeatureKey.CHILDREN.valueOf(), children);
		}
		
		if(isRequestedFeatureProperties(req, toPropertiesPath(FeatureKey.LINEAGE.valueOf())))
			putAsLocationObjectsIfNotNull(properties, FeatureKey.LINEAGE.valueOf(),
					LocationProxyRule.getLineage(location.getGid()));
		
		if(isRequestedFeatureProperties(req, toPropertiesPath(FeatureKey.RELATED.valueOf())))
			putAsLocationObjectsIfNotNull(properties, FeatureKey.RELATED.valueOf(),
					location.getRelatedLocations());
		
		if(isRequestedFeatureProperties(req, toPropertiesPath(FeatureKey.CODES.valueOf())))
			putAsCodeObjectsIfNotNull(properties, FeatureKey.CODES.valueOf(), location);
		
		if(isRequestedFeatureProperties(req, toPropertiesPath(FeatureKey.OTHER_NAMES.valueOf())))
			putAsAltNameObjectsIfNotNull(properties, FeatureKey.OTHER_NAMES.valueOf(), location);
		
		if(isRequestedFeatureProperties(req, toPropertiesPath(FeatureKey.KML.valueOf())))
			putAsStringIfNotNull(properties, FeatureKey.KML.valueOf(), location.getData().getKml());
		
		if(isRequestedFeatureProperties(req, toPropertiesPath(FeatureKey.SYNTHETIC_POPULATION.valueOf())))
			putAsSpewLinkObjectsIfNotNull(properties, FeatureKey.SYNTHETIC_POPULATION.valueOf(), location);
		
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
		setOtherNames(fc, location);
		setOtherCodes(fc, location);
		String parentGid = getString(fc, "parentGid");
		if (parentGid != null) {
			Location parent = LocationRule.read(Long.parseLong(parentGid));
			if (parent == null) {
				throw new RuntimeException("Cannot find parent gid="
						+ parentGid);
			}
			location.setParent(parent);
		}
		if (type.getSuperType().getId().equals(2L)) {
			List<Location> locations = wireLocationsIncluded(fc, type);
			location.setLocationsIncluded(locations);
		}
		location.setGeometry(createLocationGeometry(fc, location));
		return location;
	}

	private static void setOtherCodes(FeatureCollection fc, Location location) {
		@SuppressWarnings("unchecked")
		List<Code> codes = (List<Code>) fc.getProperties().get("otherCodes");
		if (codes == null)
			return;
		for (Code c : codes) {
			c.setLocation(location);
			if (c.getCodeType() == null)
				c.setCodeType(LocationRule.getIsgCodeType());
		}
		location.setOtherCodes(codes);
	}

	private static void setOtherNames(FeatureCollection fc, Location location) {
		@SuppressWarnings("unchecked")
		List<AltName> altNames = (List<AltName>) fc.getProperties().get(
				"otherNames");
		if (altNames == null)
			return;
		for (AltName n : altNames) {
			n.setLocation(location);
			if (n.getGisSource() == null)
				n.setGisSource(LocationRule.getAlsGisSource());
		}
		location.setAltNames(altNames);
	}

	public static FeatureGeometry asFetureGeometry(FeatureCollection fc) {
		Geometry geometry = GeoInputRule.toGeometry(fc);
		return GeoOutputRule.toFeatureGeometry(geometry);
	}

	public static Object filterByTerm(String queryTerm, Integer limit,
			Integer offset, Boolean searchOtherNames, Boolean verbose) {
		Request req = toFindByNameRequest(queryTerm, searchOtherNames, limit,
				offset, verbose);
		if (req.getVerbose()){
			List<Location> result = LocationRule.findByTerm(req);
			req.setExcludedFeatureFields(Arrays.asList(new String[] { FeatureKey.GEOMETRY.valueOf() }));
			return toFeatureCollection(req, result);
		}
		else {
			List<Long> gids = findGids(req);
			return toNonVerboseResponse(req, gids);
		}
			
	}

	public static List<Object> findByTerm(ArrayNode queryArray) {
		List<Object> result = new ArrayList<>();
		Object obj;
		Request req;
		for (JsonNode query : queryArray) {
			req = toFindByTermRequest(query);
			obj = findByTerm(req);
			result.add(obj);
		}
		return result;
	}

	public static Object findByTerm(Request req) {
		if(req.getVerbose()){
			List<Location> result = LocationRule.findByTerm(req);
			req.setExcludedFeatureFields(Arrays.asList(new String[] { toPropertiesPath(FeatureKey.CHILDREN.valueOf()),
					FeatureKey.GEOMETRY.valueOf() }));
			return toFeatureCollection(req, result);
		}
		else {
			List<Long> gids = findGids(req);
			return toNonVerboseResponse(req, gids);
		}
		
	}
	
	public static Object findByTypeId(long typeId) {
		List<Location> locations = LocationRule.findByTypeId(typeId);
		Request req = new Request();
		req.setExcludedFeatureFields(Arrays.asList(new String[] { toPropertiesPath(FeatureKey.CHILDREN.valueOf()),
				FeatureKey.GEOMETRY.valueOf() }));
		FeatureCollection featureCollection = toFeatureCollection(locations, req);
		req.setLocationTypeIds(Arrays.asList(new Integer[] {(int) typeId}));
		Map<String, Object> properties = toProperties(req, locations.size());
		featureCollection.setProperties(properties);
		return featureCollection;
	}

	private static Map<String, Object> toNonVerboseResponse(Request req, List<Long> gids) {
		Map<String, Object> result = new HashMap<>();
		result.put("gids", gids);
		result.put(FeatureKey.PROPERTIES.valueOf(), toProperties(req, gids.size()));
		return result;
	}

	private static List<Long> findGids(Request req) {
		List<BigInteger> list = LocationRule.findGids(req);
		List<Long> result = toListOfLong(list);
		return result;
	}

	public static Object findByPoint(double latitude, double longitude, Boolean verbose) {
		if(verbose){
			List<Location> result = LocationRule.findByPoint(latitude, longitude);
			Request req = new Request();
			req.setExcludedFeatureFields(Arrays.asList(new String[] { toPropertiesPath(FeatureKey.CHILDREN.valueOf()),
					FeatureKey.GEOMETRY.valueOf() }));
			FeatureCollection response = toFeatureCollection(req, result);
			putAsStringIfNotNull(response.getProperties(), "latitude", latitude);
			putAsStringIfNotNull(response.getProperties(), "longitude", longitude);
			putAsStringIfNotNull(response.getProperties(), "verbose", verbose);

			return response;
		}
		else{
			List<Long> gids = toListOfLong(GeometryRule.findByPoint(latitude, longitude));
			Map<String, Object> response = new HashMap<>();
			response.put("gids", gids);
			Map<String, Object> properties = new HashMap<>();
			putAsStringIfNotNull(properties, "resultSize", gids.size());
			putAsStringIfNotNull(properties, "latitude", latitude);
			putAsStringIfNotNull(properties, "longitude", longitude);
			putAsStringIfNotNull(properties, "verbose", verbose);
			response.put(FeatureKey.PROPERTIES.valueOf(), properties);
			return response;			
		}
	}

	private static Request toFindByNameRequest(String queryTerm,
			Boolean searchOtherNames, Integer limit, Integer offset, Boolean verbose) {
		Request req = new Request();
		if (queryTerm == null)
			throw new BadRequest("\"" + "queryTerm" + "\" key is requierd!");
		req.setQueryTerm(queryTerm);
		req.setSearchNames(true);
		req.setSearchOtherNames(searchOtherNames);
		req.setLimit(limit);
		req.setOffset(offset);
		req.setVerbose(verbose);
		return req;
	}

	private static Request toFindByTermRequest(JsonNode node) {
		Request req = new Request();
		if (containsKey(node, "queryTerm"))
			req.setQueryTerm(node.get("queryTerm").asText());
		else
			throw new BadRequest("\"" + "queryTerm" + "\" key is requierd!");
		setStartDate(node, req, "startDate");
		setEndDate(node, req, "endDate");
		setOtherParams(node, req);
		return req;
	}

	/**
	 * @deprecated replaced by {@link #findByTerm(ArrayNode arrayNode)}
	 */
	@Deprecated
	public static List<Object> findBulk(ArrayNode arrayNode) {
		List<Object> result = new ArrayList<>();
		Object fc;
		for (JsonNode node : arrayNode) {
			Request req = toFindBulkRequest(node);
			fc = findByTerm(req);
			result.add(fc);
		}
		return result;
	}

	/**
	 * @deprecated replaced by {@link #toFindByTermRequest(JsonNode node)}
	 */
	@Deprecated
	private static Request toFindBulkRequest(JsonNode node) {
		Request req = new Request();
		if (containsKey(node, "name"))
			req.setQueryTerm(node.get("name").asText());
		else
			throw new BadRequest("\"" + "name" + "\" key is requierd!");
		setStartDate(node, req, "start");
		setEndDate(node, req, "end");
		setOtherParams(node, req);
		return req;
	}

	private static void setOtherParams(JsonNode node, Request req) {
		Boolean value;
		if (containsKey(node, "locationTypeIds"))
			req.setLocationTypeIds(toListOfInt((JsonNode) node
					.get("locationTypeIds")));
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
		value = returnDefaultIfKeyNotExists(node, "verbose", true);
		req.setVerbose(value);
		JsonNode rootALC = node.get("rootALC");
		if(rootALC != null)
			req.setRootALC(rootALC.asLong());
		JsonNode includeOnly = node.get("includeOnly");
		if(includeOnly != null)
			req.setOnlyFeatureFields(v1.interactors.Util.toListOfString(includeOnly));
		value = returnDefaultIfKeyNotExists(node, "fuzzyMatch", false);
		req.setFuzzyMatch(value);
		if (containsKey(node, "fuzzyMatchThreshold"))
			req.setFuzzyMatchThreshold((float) node.get("fuzzyMatchThreshold").asDouble());
	}

	private static void setEndDate(JsonNode node, Request req, String endDate) {
		if (containsKey(node, endDate))
			req.setEndDate(toDate(node.get(endDate).asText()));
		if (req.getEndDate() == null)
			req.setEndDate(getNowDate());
	}

	private static void setStartDate(JsonNode node, Request req,
			String startDate) {
		if (containsKey(node, startDate))
			req.setStartDate(toDate(node.get(startDate).asText()));
		if (req.getStartDate() == null)
			req.setStartDate(toDate("0001-01-01"));
	}

	private static Boolean returnDefaultIfKeyNotExists(JsonNode node,
			String key, Boolean defaultValue) {
		if (containsKey(node, key))
			return node.get(key).asBoolean();
		else
			return defaultValue;
	}
	
	private static List<Long> toListOfLong(List<BigInteger> list) {
		List<Long> result = new ArrayList<>();
		for (BigInteger gid : list)
			result.add(gid.longValue());
		return result;
	}
}
