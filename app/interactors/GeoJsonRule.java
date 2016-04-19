package interactors;

import static interactors.Util.containsKey;
import static interactors.Util.getNowDate;
import static interactors.Util.includeField;
import static interactors.Util.listToString;
import static interactors.Util.newDate;
import static interactors.Util.putAsStringIfNotNull;
import static interactors.Util.toDate2;
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
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;

import dao.entities.AltName;
import dao.entities.Code;
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
	public static final List<String> DEFAULT_KEYS = Arrays.asList(new String[]{
			KEY_PROPERTIES, KEY_CHILDREN
	});

	public static FeatureCollection asFeatureCollection(Location location) {
		long locationTypeId = location.getData().getLocationType().getId();
		if (locationTypeId == LocationRule.PUMA_TYPE_ID 
				|| locationTypeId == LocationRule.COMPOSITE_LOCATION_ID){
			return toFeatureCollection(location);
		}
		List<Location> list = new ArrayList<>();
		list.add(location);
		return toFeatureCollection(list, null);
	}

	public static FeatureCollection toFeatureCollection(List<Location> locations,
			List<String> fields) {
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
			computeBbox =  computeBbox(features);
		fc.setBbox(computeBbox);
		fc.setId(composite.getGid() + "");
		return fc;
	}

	private static List<Feature> toFeatures(List<Location> locations, 
			List<String> fields) {
		List<Feature> features = new ArrayList<>();
		for (Location location : locations) {
			if (location == null){
				Logger.warn("toFeatures got an element in the list as null.");
				continue;
			}
			features.add(toFeature(location, fields));
		}
		
		return features;
	}

	private static Feature toFeature(Location location, List<String> fields) {
		Feature feature = new Feature();
		if (includeField(fields, KEY_PROPERTIES, KEY_CHILDREN)){
			Map<String, Object> properties = toPropertiesOfFeature(location, true);
			feature.setProperties(properties);
		}
		else if (includeField(fields, KEY_PROPERTIES)){
			Map<String, Object> properties = toPropertiesOfFeature(location, false);
			feature.setProperties(properties);
		}
		LocationGeometry geometry = null;
		if (includeField(fields, KEY_GEOMETRY)){
			final LocationGeometry locationGeometry = location.getGeometry();
			if (locationGeometry == null)
				geometry = GeometryRule.read(location.getGid());
			else
				geometry = locationGeometry;
		}
		
		if (geometry != null){
			Geometry multiPolygonGeom = geometry.getShapeGeom();
			feature.setGeometry(GeoOutputRule.toFeatureGeometry(multiPolygonGeom));
			if (includeField(fields, KEY_BBOX)) 
				feature.setBbox(computeBbox(location));
			if (includeField(fields, KEY_REPPOINT))
				feature.setRepPoint(getRepPoint(geometry));
			feature.setId(location.getGid() + "");
		}
		
		return feature;
	}

	private static double[] getRepPoint(LocationGeometry geometry) {
		if(geometry == null)
			return null;
		Point repPoint = geometry.getRepPoint();
		if(repPoint == null)
			return null;
		return GeoOutputRule.toPoint(repPoint.getCoordinate());
	}

	private static Map<String, Object> toPropertiesOfFeature(Location location, boolean includeChildren) {
		Map<String, Object> properties = toProperties(location);
		if (includeChildren){
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

	private static void putAsAltNameObjectsIfNotNull(
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
		for (AltName name : otherNames){
			anotherName = new HashMap<>();
			anotherName.put(KEY_NAME, name.getName());
			anotherName.put(KEY_LANG, name.getLanguage());
			anotherName.put(KEY_DESC, name.getDescription());
			names.add(anotherName);
		}		
	}
	
	private static double[] computeBbox(Location l) {
		if (l == null)
			return null;
		LocationGeometry geometry = l.getGeometry();
		if (geometry == null)
			return null;
		return computeBbox(geometry.getShapeGeom());
	}
	
	private static double[] computeBbox(List<Feature> features) {
		if (features == null || features.isEmpty())
			return null;
		double[] result = new double[]{ 
				Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY,
				Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY 
				};
		for(Feature f : features){
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
		for (double d : result){
			if (Double.isInfinite(d))
				return null;
		}
		return result;
	}

	private static double[] computeBbox(Geometry geometry) {
		if (geometry == null || geometry.isEmpty())
			return null;
		Geometry envelope = geometry.getEnvelope();
		Coordinate[] coordinates = envelope.getCoordinates();
		Coordinate westSouth = coordinates[0];
		Coordinate eastNorth = coordinates.length >=2 
				? coordinates[2] 
				: westSouth;
		double[] bbox = new double[]{westSouth.x, westSouth.y, 
				eastNorth.x, eastNorth.y};
		return bbox;
	}

	private static void putAsCodeObjectsIfNotNull(Map<String, Object> properties,
			String string, Location location) {
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
		for (Code c : otherCodes){
			Map<String, String> anotherCode = new HashMap<>();
			anotherCode.put(KEY_CODE, c.getCode());
			anotherCode.put(KEY_TYPE, c.getCodeType().getName());
			codes.add(anotherCode);
		}
	}

	private static void putAsLocationObjectsIfNotNull(
			Map<String, Object> properties, String key, 
			List<Location> locations) {
		if (locations == null)
			return;
		Map<String, Object> locationProperties;
		List<Map<String, Object>> list = new ArrayList<>();
		for (Location l : locations){
			locationProperties = toProperties(l);
			locationProperties.remove("headline");
			locationProperties.remove("rank");
			list.add(locationProperties);
		}
		properties.put(key, list);
	}

	private static Map<String, Object> toProperties(Location location) {
		Map<String, Object> properties = new HashMap<>();
		String gid = getGid(location);
		putAsStringIfNotNull(properties, "gid", gid);
		Data data = location.getData();
		if (data == null){
			Logger.warn(gid + " has null data!");
			return properties;
		}
		putAsStringIfNotNull(properties, "name", data.getName());
		putAsStringIfNotNull(properties, "locationDescription", data.getDescription());
		putAsStringIfNotNull(properties, "headline", location.getHeadline());
		putAsStringIfNotNull(properties, "rank", location.getRank());
		putAsStringIfNotNull(properties, "startDate", data.getStartDate());
		putAsStringIfNotNull(properties, "endDate", data.getEndDate());
		String locationTypeName = getLocationTypeName(data.getLocationType());
		putAsStringIfNotNull(properties, "locationTypeName", locationTypeName);
		Location parent = location.getParent();
		putAsStringIfNotNull(properties, "parentGid", getGid(parent));
		return properties;
	}

	private static String getLocationTypeName(LocationType locationType) {
		return (locationType == null) ? null : locationType.getName();
	}

	private static String getGid(Location location) {
		if (location == null)
			return null;
		return String.valueOf(location.getGid());
	}

	public static Location asLocation(FeatureCollection fc){
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
		if (parent == null){
			throw new RuntimeException("Cannot find parent gid=" + parentGid);
		}
		location.setParent(parent);
		if (type.getSuperType().getId().equals(2L)){
			List<Location> locations = wireLocationsIncluded(fc, type);
			location.setLocationsIncluded(locations);
		}		
		location.setGeometry(createLocationGeometry(fc, location));
		return location;
	}

	private static List<Location> wireLocationsIncluded(FeatureCollection fc, LocationType type) {
		LocationType allowType = type.getComposedOf();
		List<Location> locations = new ArrayList<>();
		for (Feature f : fc.getFeatures()){
			String gid = f.getId();
			if (gid != null){
				Location location = LocationRule.read(Long.parseLong(gid));
				if (location == null){
					throw new RuntimeException("Cannot find LocationIncluded gid=" + gid);
				}
				LocationType foundType = location.getData().getLocationType();
				if (allowType != null &&! foundType.equals(allowType))
					throw new RuntimeException(foundType.getName() 
							+ " type of gid=" + gid 
							+ " is not allowed  to compose type " 
							+ type.getName());
						
				locations.add(location);
			}
		}
		return locations;
	}

	private static LocationType findLocationType(FeatureCollection fc) {
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
		} catch (Exception e){
			Logger.info(e.getMessage(), e);
		}
		try {
			if (type == null)
				type = LocationTypeRule.findByName(name);
		} catch (Exception e2){
			throw new RuntimeException("Requested location type not found: "
					+ idKey + "=" + id + ", " + nameKey +"=" + name);
		}
		return type;
	}

	private static LocationGeometry createLocationGeometry(
			FeatureCollection fc, Location l) {
		LocationGeometry lg = new LocationGeometry();
		lg.setShapeGeom(GeoInputRule.toGeometry(fc));
		lg.setLocation(l);
		Date now = l.getData().getUpdateDate();
		lg.setUpdateDate(now);
		return lg;
	}

	private static String getString(FeatureCollection fc, String key) {
		Object object = fc.getProperties().get(key);
		if (object == null)
			return null;
		return object.toString();
	}
	
	private static Request toRequest(String name, Boolean searchOtherNames, Integer limit, Integer offset) {
		Request req = new Request();
		req.setQueryTerm(name);
		req.setSearchOtherNames(searchOtherNames);
		req.setLimit(limit);
		req.setOffset(offset);
		return req;
	}

	public static Response findByName(String q, 
			Integer limit, Integer offset, Boolean searchOtherNames){
		Request req = toRequest(q, searchOtherNames, limit, offset);
		List<Location> result = LocationRule.findLocations(req);
		Response response = new Response();
		response.setGeoJSON(toFeatureCollection(result, DEFAULT_KEYS));
		Map<String, Object> properties = new HashMap<>();
		response.setProperties(properties);
		properties.put("q", q);
		putAsStringIfNotNull(properties, "limit", limit);
		putAsStringIfNotNull(properties, "offset", offset);
		putAsStringIfNotNull(properties, "searchOtherNames", searchOtherNames);
		putAsStringIfNotNull(properties, "resultSize", "" + result.size());
		//properties.put("locationTypeName", "Result from a query");
		String descritpion = "Result from the query for '" + q + "' limit=" 
		+ limit + " offset=" + offset + " searchOtherNames=" + searchOtherNames;
		properties.put("locationDescription", descritpion);
		return response;
	}
	
	public static List<Object> findLocations(ArrayNode arrayNode){
		List<Object> result = new ArrayList<>();
		FeatureCollection fc;
		for (JsonNode node : arrayNode) {
			Request req = toRequest(node);
			fc = findLocations(req);
			result.add(fc);
		}
		return result;
	}

	private static Request toRequest(JsonNode node) {
		Request req = new Request();
		if(containsKey(node, "q"))
			req.setQueryTerm(node.get("q").asText());
		else
			throw new BadRequest("\"" + "q" + "\" key is requierd!");
		if(containsKey(node, "locationTypeIds"))
			req.setTypeId(toListOfInt((JsonNode) node.get("locationTypeIds")));
		if(containsKey(node, "start"))
			req.setStart(toDate2(node.get("start").asText()));
		if(containsKey(node, "end"))
			req.setEnd(toDate2(node.get("end").asText()));
		if(containsKey(node, "limit"))
			req.setLimit(node.get("limit").asInt());
		if(containsKey(node, "offset"))
			req.setOffset(node.get("offset").asInt());
		if(containsKey(node, "ignoreAccent"))
			req.setIgnoreAccent(node.get("ignoreAccent").asBoolean());
		if(containsKey(node, "searchNames"))
			req.setSearchNames(node.get("searchNames").asBoolean());
		if(containsKey(node, "searchOtherNames"))
			req.setSearchOtherNames(node.get("searchOtherNames").asBoolean());
		if(containsKey(node, "searchCodes"))
			req.setSearchCodes(node.get("searchCodes").asBoolean());
		return req;
	}
	
	public static FeatureCollection findLocations(Request req) {
		List<Location> result = LocationRule.findLocations(req);
		return toGeoJSON(req, result);
	}

	private static FeatureCollection toGeoJSON(Request req,
			List<Location> result) {
		List<String> fields = Arrays.asList(new String[] {KEY_PROPERTIES});
		FeatureCollection geoJSON = toFeatureCollection(result, fields);
		Map<String, Object> properties = new HashMap<>();
		geoJSON.setProperties(properties);
		properties.put("q", req.getQueryTerm());
		putAsStringIfNotNull(properties, "locationTypeIds", listToString(req.getTypeId()));
		putAsStringIfNotNull(properties, "start", toStringValue(req.getStart()));
		putAsStringIfNotNull(properties, "end", toStringValue(req.getEnd()));
		putAsStringIfNotNull(properties, "limit", "" + req.getLimit());
		putAsStringIfNotNull(properties, "offset", "" + req.getOffset());
		putAsStringIfNotNull(properties, "ignoreAccent", req.isIgnoreAccent());
		putAsStringIfNotNull(properties, "searchNames", req.isSearchNames());
		putAsStringIfNotNull(properties, "searchOtherNames", req.isSearchOtherNames());
		putAsStringIfNotNull(properties, "searchCodes", req.isSearchCodes());
		putAsStringIfNotNull(properties, "resultSize", "" + result.size());
		return geoJSON;
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

	public static FeatureGeometry asFetureGeometry(FeatureCollection fc) {
		Geometry geometry = GeoInputRule.toGeometry(fc);
		return GeoOutputRule.toFeatureGeometry(geometry);
	}
}
