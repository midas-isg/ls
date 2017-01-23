package interactors;

import static interactors.GeoJsonHelperRule.*;
import static interactors.RequestRule.isRequestedFeatureField;
import static interactors.RequestRule.isRequestedFeatureProperties;
import static interactors.Util.*;

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

import dao.LocationTypeDao;
import dao.entities.AltName;
import dao.entities.Code;
import dao.entities.Data;
import dao.entities.Location;
import dao.entities.LocationGeometry;
import dao.entities.LocationType;
import models.FeatureKey;
import models.Request;
import models.exceptions.BadRequest;
import models.geo.Feature;
import models.geo.FeatureCollection;
import models.geo.FeatureGeometry;
import play.Logger;

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
		putAsStringIfNotNull(properties, "onlyFeatureFields",
				listToString(req.getOnlyFeatureFields()));
		putAsStringIfNotNull(properties, "excludedFeatureFields",
				listToString(req.getExcludedFeatureFields()));
		putAsStringIfNotNull(properties, "resultSize", resultSize);
		putAsStringIfNotNull(properties, "fuzzyMatch", req.isFuzzyMatch());
		if (isTrue(req.isFuzzyMatch()))
			putAsStringIfNotNull(properties, "fuzzyMatchThreshold", req.getFuzzyMatchThreshold());
		putAsStringIfNotNull(properties, "latitude", req.getLatitude());
		putAsStringIfNotNull(properties, "longitude", req.getLongitude());
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

	public static Object filterByTerm(String onlyFeatureFields, String excludedFeatureFields,
			String queryTerm, Integer limit, Integer offset, Boolean searchOtherNames) {
		if (queryTerm == null)
			throwNoQueryTermExp();
		Request req = RequestRule.toFilterByTermRequest(onlyFeatureFields, excludedFeatureFields, queryTerm, limit, offset, searchOtherNames);
		return findByTerm(req);
	}

	public static List<Object> findByTerm(ArrayNode queryArray) {
		List<Object> result = new ArrayList<>();
		Object obj;
		Request req;
		for (JsonNode query : queryArray) {
			req = RequestRule.toFindByTermRequest(query);
			if(req.getQueryTerm() == null)
				throwNoQueryTermExp();
			obj = findByTerm(req);
			result.add(obj);
		}
		return result;
	}

	public static Object findByTerm(Request req) {
		List<Location> result = LocationRule.findByTerm(req);
		return toFeatureCollection(req, result);
	}
	
	public static Object findByTypeId(String onlyFeatureFields, String excludedFeatureFields, Long typeId,
			Integer limit, Integer offset) {
		if(typeId == null)
			throw new BadRequest("\"" + "typeId" + "\" is requierd!");
		Request req = RequestRule.toFindByTypeRequest(onlyFeatureFields, excludedFeatureFields, typeId, limit, offset);
		List<Location> locations = LocationRule.findByTypeId(typeId, limit, offset);
		FeatureCollection featureCollection = toFeatureCollection(locations, req);
		Map<String, Object> properties = toProperties(req, locations.size());
		featureCollection.setProperties(properties);
		return featureCollection;
	}

	public static Object findByPoint(String onlyFeatureFields, String excludedFeatureFields, double latitude, double longitude) {
		
		Request req = RequestRule.toFindByPointRequest(onlyFeatureFields, excludedFeatureFields, latitude, longitude);
		List<Location> result = LocationRule.findByPoint(latitude, longitude);
		FeatureCollection response = toFeatureCollection(req, result);
		response.setProperties(toProperties(req, result.size()));
		return response;
	}

	private static void throwNoQueryTermExp() {
		throw new BadRequest("\"" + "queryTerm" + "\" key is requierd!");
	}
}
