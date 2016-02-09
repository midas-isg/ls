package controllers;

import interactors.GeoJsonRule;
import interactors.GeometryRule;
import interactors.KmlRule;
import interactors.LocationProxyRule;
import interactors.LocationRule;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;

import models.Response;
import models.exceptions.PostgreSQLException;
import models.exceptions.BadRequest;
//import models.filters.LocationFilter;
import models.geo.FeatureCollection;
import models.geo.FeatureGeometry;
import play.Logger;
import play.Play;
import play.db.jpa.Transactional;
import play.libs.Json;
import play.libs.Jsonp;
import play.mvc.Controller;
import play.mvc.Result;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiImplicitParam;
import com.wordnik.swagger.annotations.ApiImplicitParams;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;

import dao.entities.Location;

@Api(value = "/api/locations", description = "Endpoint for locations")
public class LocationServices extends Controller {
	public static final String FORMAT_GEOJSON = "geojson";
	public static final String FORMAT_GEOJSONP = "jsonp";
	public static final String FORMAT_APOLLOJSON = "json";
	public static final String FORMAT_APOLLOXML = "xml";
	public static final String FORMAT_KML = "kml";
	public static final String FORMAT_DEFAULT = "geojson";
	private static final String UNIQUE_VIOLATION = "23505";

	@Transactional
	@ApiOperation(
			httpMethod = "GET", 
			nickname = "readLocation", 
			value = "Returns a location by ID", 
			notes = "This endpoint returns a location in the requested format (format) by ID (gid). "
			+ "The ID is usaully called as GID by Geographic Information System (GIS) "
			+ "and we refer to as 'Apollo Location Code'. "
			+ "For some locations, "
			+ "their geometry information is too big to use in other services. "
			+ "To simplify geometry information, "
			+ "request the maximum number of exterior rings (maxExteriorRings) "
			+ "to a number that other services can handle", 
			response = FeatureCollection.class
	)
	@ApiResponses(value = {
			@ApiResponse(code = OK, message = "Successful retrieval of location", 
					response = FeatureCollection.class),
			@ApiResponse(code = NOT_FOUND, message = "Location not found"),
			@ApiResponse(code = INTERNAL_SERVER_ERROR, message = "Internal server error"),
			@ApiResponse(code = BAD_REQUEST, message = "Format is not supported") 
	})
	public Result locations(
			@ApiParam(value = "ID of the location (Apollo Location Code)", required = true) 
			@PathParam("gid") 
			Long gid,
			
			@ApiParam(
					value = "Requested response format: GeoJSON (geojson), "
					+ "JSON-P of GeoJSON (jsonp), "
					+ "Apollo Location JSON (json), Apollo Location XML (xml), "
					+ "or Keyhole Markup Language (kml). ", 
					required = false, 
					allowableValues = "[geojson, jsonp, json, xml, kml]", 
					defaultValue = "geojson"
			) 
			@QueryParam("format")
			String format,
			
			@ApiParam(
					value = "Maximum number of exterior rings in the response. ", 
					required = false
			) 
			@QueryParam("maxExteriorRings") 
			Integer maxExteriorRings
	) {

		if (gid == null)
			return notFound("gid is required but got " + gid);

		if (IsFalsified(format))
			format = FORMAT_DEFAULT;

		Location location = Wire.simplifyToMaxExteriorRings(gid, maxExteriorRings);
		switch (format.toLowerCase()) {
		case FORMAT_GEOJSON:
			return asGeoJson(location);
		case FORMAT_GEOJSONP:
			return asGeoJsonp(location, "jsonpCallback");
		case FORMAT_APOLLOJSON:
			return ApolloLocationServices.asJson(location);
		case FORMAT_APOLLOXML:
			return ApolloLocationServices.asXml(location);
		case FORMAT_KML:
			return asKml(location);
		default:
			return badRequest(format + " is not supported.");
		}
	}

	@Transactional
	public Result jsonp(
			Long gid,
			String callback,
			Integer maxExteriorRings
	) {

		if (gid == null)
			return notFound("gid is required but got " + gid);

		Location location = Wire.simplifyToMaxExteriorRings(gid, maxExteriorRings);
		return asGeoJsonp(location, callback);
	}

	static Result asGeoJsonp(Location location, String callback) {
		response().setContentType("text/javascript");
		return ok(Jsonp.jsonp(callback, Json.toJson(Wire.asFeatureCollection(location))));
	}


	@Transactional
	public Result getGeometryMetadata(long gid, Double tolerance) {
		Object object = LocationRule.getSimplifiedGeometryMetadata(gid, tolerance);
		return ok(Json.toJson(object));
	}

	private boolean IsFalsified(String text) {
		return text == null || text.isEmpty();
	}

	@Transactional
	@ApiOperation(
			httpMethod = "GET", 
			nickname = "findLocationsByName", 
			value = "Returns locations by name search", 
			notes = "This endpoint returns locations whose name matches the requested search terms (q). "
			+ "To do pagination, use 'limit' and 'offset'. "
			+ "Note: The schema of the 'geoJSON' field in the response is GeoJSON FeatureCollection. ", 
			response = Response.class)
	@ApiResponses(value = {
			@ApiResponse(code = OK, message = "Successful retrieval of location", response = Response.class),
			//@ApiResponse(code = 404, message = "Location not found"),
			@ApiResponse(code = INTERNAL_SERVER_ERROR, message = "Internal server error"),
			//@ApiResponse(code = 400, message = "Format is not supported") 
	})
	public Result findLocations(
			@ApiParam(
					value = "Search terms delimited by a space charactor. "
					+ "The search terms are combined together with conjunction. ",
					required = true
			) 
			@QueryParam("q") 
			String q,
			
			@ApiParam(
					value = "Maximum number of locations to return. ", 
					required = true, defaultValue = "10"
			) 
			@QueryParam("limit") 
			Integer limit,
			
			@ApiParam(
					value = "Page offset if number of locations exceeds limit. ", 
					required = true, defaultValue = "0"
			) 
			@QueryParam("offset") 
			Integer offset
	) {
		Object result = GeoJsonRule.findByName(q, limit, offset);
		return ok(Json.toJson(result));
	}

	@Transactional
	@ApiOperation(
			httpMethod = "POST", 
			nickname = "findBulkLocationsByNameAndDate", 
			value = "Returns locations by name and start and end dates", 
			notes = "This endpoint returns locations whose name matches the requested search terms (name, start date, end date). "
			+ "Note: The schema of the 'geoJSON' field in the response is GeoJSON FeatureCollection. ", 
			response = Response.class)
	@ApiResponses(value = {
			@ApiResponse(code = OK, message = "Successful retrieval of location", response = Response.class),
			//@ApiResponse(code = 404, message = "Location not found"),
			@ApiResponse(code = INTERNAL_SERVER_ERROR, message = "Internal server error"),
			//@ApiResponse(code = 400, message = "Format is not supported") 
	})
	
	public Result findBulkLocations() {
		ArrayList<Map<String, Object>> params = toParams((ArrayNode)request().body().asJson());
		List<Object> result = GeoJsonRule.findBulkLocations(params);
		return ok(toJson(result));
	}
	
	private ArrayNode toJson(List<Object> list) {
		ArrayNode result = Json.newArray();
		for(Object item : list){
			result.add(Json.toJson(item));
		}
	    return result;
	}

	private static ArrayList<Map<String, Object>> toParams(ArrayNode array) {
		ArrayList<Map<String,Object>> params = new ArrayList<>();
		for(JsonNode node: array){
			Map<String,Object> map = new HashMap<>();
			putAsRequired(node, map, "name");
			putAsNullIfNull(node, map, "locationTypeIds");
			putAsTextIfNotNull(node, map, "start");
			putAsTextIfNotNull(node, map, "end");
			
			params.add(map);
		}
		return params;
	}

	private static void putAsRequired(JsonNode node, Map<String, Object> map,
			String string) {
		if(getKeyList(node).contains(string))
			map.put("name",node.findValue(string).asText());
		else
			throw new BadRequest("\"" + string + "\" key is requierd!");
	}

	private static void putAsNullIfNull(JsonNode node, Map<String, Object> map, String key) {
		if(getKeyList(node).contains(key))
			map.put(key, node.get(key));
		else
			map.put(key, null);
	}
	
	private static void putAsTextIfNotNull(JsonNode node, Map<String, Object> map, String key) {
		if(getKeyList(node).contains(key))
			map.put(key, node.findValue(key).asText());
		else
			map.put(key, null);
	}
	
	private static List<String> getKeyList(JsonNode node) {
		List<String> keys = new ArrayList<>();
		Iterator<String> l = node.fieldNames();
		while(l.hasNext()){
			keys.add(l.next());
		}
		return keys;
	}
	
	@Transactional
	public Result findLocationNames(String q, Integer limit) {
		Object result = LocationProxyRule.findLocationNames(q, limit);
		return ok(Json.toJson(result));
	}

	@Transactional
	@ApiOperation(
			httpMethod = "GET", 
			nickname = "findLocationsByPoint", 
			value = "Returns locations by coordinate search", 
			notes = "This endpoint returns locations whose geometry encompasses the submitting coordinate. "
			+ "The coordinate is defined by latitude (lat) and longtitude (long). "
			+ "Note: The schema of the 'geoJSON' field in the response is GeoJSON FeatureCollection. ", 
			response = Response.class
	)
	@ApiResponses(value = {
			@ApiResponse(code = OK, message = "Successful retrieval of location", response = Response.class),
			//@ApiResponse(code = NOT_FOUND, message = "Location not found"),
			@ApiResponse(code = INTERNAL_SERVER_ERROR, message = "Internal server error"),
			//@ApiResponse(code = BAD_REQUEST, message = "Format is not supported") 
	})
	public Result findLocationsByPoint(
			@ApiParam(value = "Latitude in degree", required = true) @QueryParam("lat") 
			double lat,
			@ApiParam(value = "Longitude in degree", required = true) @QueryParam("long") 
			double lon
	) {
		Object result = GeoJsonRule.findByPoint(lat, lon);
		return ok(Json.toJson(result));
	}

	@Transactional
	@ApiOperation(
			httpMethod = "POST", 
			nickname = "createLocation", 
			value = "Creates a location", 
			notes = "This endpoint creates a location using submitted GeoJSON FeatureCollection object "
					+ "in body and return the URI via the 'Location' Header in the response. "
					+ "Currently, no content returns in the body. ", 
			response = Void.class
	)
	@ApiResponses(value = { 
			@ApiResponse(code = OK, message = "(Not used yet)"),
			@ApiResponse(code = CREATED, message = "Successful location creation", response = Void.class),
			@ApiResponse(code = FORBIDDEN, message = "Failed location creation due to duplication"),
			@ApiResponse(code = INTERNAL_SERVER_ERROR, message = "Internal server error"),
			@ApiResponse(code = BAD_REQUEST, message = "Invalid input") 
	})
    @ApiImplicitParams( { 
    	@ApiImplicitParam(
    			value = "GeoJSON FeatureCollection", 
    			required = true, 
    			dataType = "models.geo.FeatureCollection", 
    			paramType = "body"
    	) 
    } )
	public Result create() {
		try {
			FeatureCollection parsed = parseRequestAsFeatureCollection();
			Long id = Wire.create(parsed);
			setResponseLocation(id);

			return created();
		} catch(PostgreSQLException e){
			String message = e.getMessage();
			if(e.getSQLState()!= null && e.getSQLState().equals(UNIQUE_VIOLATION))
				return forbidden(message);
			
			return badRequest(message);
		} catch (RuntimeException e) {
			String message = e.getMessage();
			Logger.error(message, e);

			return badRequest(message);
		} catch (Exception e) {
			String message = e.getMessage();
			Logger.error(message, e);

			return forbidden(message);
		}
	}

	private void setResponseLocation(Long id) {
		AdministrativeUnitServices.setResponseLocation(id);
	}

	private FeatureCollection parseRequestAsFeatureCollection() throws Exception {
		return AdministrativeUnitServices.parseRequestAsFeatureCollection();
	}

	@Transactional
	Result asGeoJson(Location location) {
		response().setContentType("application/vnd.geo+json");
		return ok(Json.toJson(Wire.asFeatureCollection(location)));
	}

	@Transactional
	@ApiOperation(
			httpMethod = "PUT", 
			nickname = "updateLocation", 
			value = "Updates a location", 
			notes = "This endpoint does full update the given location "
					+ "idientified by 'gid' with submitted GeoJSON "
					+ "FeatureCollection object in body "
					+ "and returns the URI via the 'Location' Header in the response. "
					+ "Currently, no content in the body. ",
			response = Void.class
	)
	@ApiResponses(value = { 
			@ApiResponse(code = OK, message = "(Not used yet)"),
			@ApiResponse(code = NO_CONTENT, message = "Location updated", response = Void.class),
			@ApiResponse(code = FORBIDDEN, message = "Failed location update due to duplication"),
			//@ApiResponse(code = NOT_FOUND, message = "Location not found"),
			@ApiResponse(code = INTERNAL_SERVER_ERROR, message = "Internal server error"),
			@ApiResponse(code = BAD_REQUEST, message = "Format is not supported") 
	})
    @ApiImplicitParams( { 
    	@ApiImplicitParam(
    			value = "GeoJSON FeatureCollection", 
    			required = true, 
    			dataType = "models.geo.FeatureCollection", 
    			paramType = "body"
    	) 
    } )
	public Result update(
			@ApiParam(value = "ID of the location (Apollo Location Code)", required = true) 
			@PathParam("gid") 
			Long gid
	) {
		try {
			FeatureCollection parsed = parseRequestAsFeatureCollection();
			Wire.update(gid, parsed);
			setResponseLocation(null);
			return noContent();
		} catch (RuntimeException e) {
			String message = e.getMessage();
			Logger.error(message, e);
			return badRequest(message);
		} catch (Exception e) {
			String message = e.getMessage();
			Logger.error(message, e);
			return forbidden(message);
		}
	}

	@Transactional
	@ApiOperation(
			httpMethod = "DELETE",
			nickname = "deleteLocation", 
			value = "Deletes a location", 
			notes = "This endpoint deletes the given location idientified by 'gid' "
					+ "and returns the URI via the 'Location' Header in the response. "
					+ "Currently, no content in the body. "
					+ "Note: This enpoint is restricted based on application configuration. ", 
			response = Void.class
	)
	@ApiResponses(value = { 
			@ApiResponse(code = OK, message = "(Not used yet)"),
            @ApiResponse(code = UNAUTHORIZED , message = "Unauthorized"),
			@ApiResponse(code = NO_CONTENT , message = "Location deleted", response = Void.class),
			@ApiResponse(code = INTERNAL_SERVER_ERROR, message = "Internal server error"),
			//@ApiResponse(code = 400, message = "Format is not supported")
			@ApiResponse(code = NOT_FOUND, message = "Location not found")
	})
	public Result delete(
			@ApiParam(value = "ID of the location (Apollo Location Code)", required = true) 
			@PathParam("gid") 
			Long gid
	) {
		String remote = request().remoteAddress();
		if (Play.application().configuration().getString("permission.delete", "127.0.0.1").indexOf(remote) == -1) {

            Logger.warn("Anauthorized attempt to delete by " + remote);
			return unauthorized();

		}

		Long id = Wire.delete(gid);
		setResponseLocation(null);
		if (id == null) {
			return notFound();
		}
		return noContent();
	}

	@Transactional
	public Result asKml(Location location) {
		String result = KmlRule.asKml(location);
		response().setContentType("application/vnd.google-earth.kml+xml");
		return ok(result);
	}

	@Transactional
	public Result findByFeatureCollection(Long superTypeId, Long typeId) throws Exception {
		FeatureCollection fc = parseRequestAsFeatureCollection();
		response().setContentType("application/vnd.geo+json");
		return Wire.findByFeatureCollection(fc, superTypeId, typeId);
	}

	public static class Wire {
		public static Long create(FeatureCollection fc) {
			Location location = GeoJsonRule.asLocation(fc);
			Long id = LocationRule.create(location);
			return id;
		}

		public static Location read(Long gid) {
			return simplifyToMaxExteriorRings(gid, null);
		}

		public static Location simplifyToMaxExteriorRings(Long gid, Integer maxExteriorRings) {
			Location location = LocationRule.simplifyToMaxExteriorRings(gid, maxExteriorRings);
			return location;
		}

		public static FeatureCollection asFeatureCollection(Location location) {
			FeatureCollection fc = GeoJsonRule.asFeatureCollection(location);
			return fc;
		}

		public static Long update(long gid, FeatureCollection fc) {
			Location location = GeoJsonRule.asLocation(fc);
			return LocationRule.update(gid, location);
		}

		public static Long delete(long gid) {
			Long id = LocationRule.deleteTogetherWithAllGeometries(gid);
			return id;
		}

		public static Result findByFeatureCollection(FeatureCollection fc, Long superTypeId, Long typeId) {
			FeatureGeometry geometry = GeoJsonRule.asFetureGeometry(fc);
			String geo = Json.toJson(geometry).toString();
			List<BigInteger> gids = GeometryRule.findGidsByGeometry(geo, superTypeId, typeId);
			List<Location> locations = LocationProxyRule.getLocations(gids);
			return ok(Json.toJson(GeoJsonRule.toFeatureCollection(locations, GeoJsonRule.DEFAULT_KEYS)));
		}
	}
}
