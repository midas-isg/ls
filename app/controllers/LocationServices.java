package controllers;

import interactors.GeoJsonRule;
import interactors.GeometryRule;
import interactors.KmlRule;
import interactors.LocationProxyRule;
import interactors.LocationRule;

import java.math.BigInteger;
import java.util.List;

import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;

import models.Response;
import models.exceptions.PostgreSQLException;
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
import com.sun.javafx.collections.ListListenerHelper;
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
	private static final String findBulkEx = "find-bulk.json";
	private static final String findBulkExBody = "Only \"queryTerm\" is required. See an example of body at "
			+ "<a href='assets/examples/api/" + findBulkEx + "'>" + findBulkEx + "</a> ";
	private static final String findEx = "find-by-term.json";
	private static final String findExBody = "Only \"queryTerm\" is required. See an example of body at "
			+ "<a href='assets/examples/api/" + findEx + "'>" + findEx + "</a> ";
	private static final String findbyGeomEx = "AuMaridiTown.geojson";
	private static final String findbyGeomExBody = "See an example of body at "
			+ "<a href='assets/examples/api/" + findbyGeomEx + "'>" + findbyGeomEx + "</a> ";
	private static final String superTypeAPI = "/api/super-types";
	private static final String locationTypeAPI = "/api/location-types";

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
			@ApiResponse(code = OK, message = "Successfully returned", 
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
		if (location == null)
			return notFound("Location not found");
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
			nickname = "findLocationsByTerm", 
			value = "Returns locations by term search", 
			notes = "This endpoint returns locations whose name matches the requested search term (queryTerm or q). "
			+ "To do pagination, use 'limit' and 'offset'. "
			+ "Note: The schema of the 'geoJSON' field in the response is GeoJSON FeatureCollection. ", 
			response = Response.class)
	@ApiResponses(value = {
			@ApiResponse(code = OK, message = "Successfully returned", response = Response.class),
			@ApiResponse(code = INTERNAL_SERVER_ERROR, message = "Internal server error"),
			@ApiResponse(code = BAD_REQUEST, message = "Bad request") 
	})
	public Result filterByTerm(
			@ApiParam(
					value = "Search terms delimited by a space charactor. "
					+ "The search terms are combined together with conjunction. ",
					required = true, defaultValue = ""
			) 
			@QueryParam("queryTerm") 
			String queryTerm,
			
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
			Integer offset,
			
			@ApiParam(
					value = "Whether to search in location alternative-names. ", 
					defaultValue = "true"
			) 
			@QueryParam("searchOtherNames") 
			Boolean searchOtherNames,
			@ApiParam(
					value = "Optional. Maintained for backward compatibility. Will be ignored if \"queryTerm\" provided.", 
					defaultValue = "",
					required = false
			)
			@QueryParam("q")
			String q
	) {
		if (queryTerm == null)
			queryTerm = q;
		Object result = GeoJsonRule.filterByTerm(queryTerm, limit, offset, searchOtherNames);
		return ok(Json.toJson(result));
	}
	
	/**
	 * @deprecated replaced by {@link #filterByTerm(String queryTerm, Integer limit,
			Integer offset, Boolean searchOtherNames)}
	 */
	@Deprecated
	@Transactional
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
	Integer offset,
	
	@ApiParam(
			value = "Whether to search in location alternative-names. ", 
			defaultValue = "true"
	) 
	@QueryParam("searchOtherNames") 
	Boolean searchOtherNames
			) {
		Object result = GeoJsonRule.filterByTerm(q, limit, offset, searchOtherNames);
		return ok(Json.toJson(result));
	}
	
	@Transactional
	@ApiOperation(
			httpMethod = "POST", 
			nickname = "Find", 
			value = "Finds locations by name, other-names, or code", 
			notes = "", 
			response = Response.class)
	@ApiResponses(value = {
			@ApiResponse(code = OK, message = "Successfully returned", response = Response.class),
			@ApiResponse(code = INTERNAL_SERVER_ERROR, message = "Internal server error"),
			@ApiResponse(code = BAD_REQUEST, message = "Invalid input")
	})
	@ApiImplicitParams({ 
	    	@ApiImplicitParam(
	    			value = findExBody, 
	    			required = true, 
	    			dataType = "[model.Request]",
	    			paramType = "body"
	    	)
	})
	public Result findByTerm() {
		if (!(request().body().asJson() instanceof JsonNode) ||
				request().body().asJson() instanceof ArrayNode)
			return badRequest("Invalid input");

		ArrayNode arrayNode = toArrayNode((JsonNode)request().body().asJson());
		List<Object> result = GeoJsonRule.findByTerm(arrayNode);
		if(result == null || result.isEmpty())
			return ok(Json.newObject());
		return ok(Json.toJson(result.get(0)));
		
	}

	private ArrayNode toArrayNode(JsonNode asJson) {
		ArrayNode arrayNode = Json.newArray();
		arrayNode.add(asJson);
		return arrayNode;
	}

	@Transactional
	@ApiOperation(
			httpMethod = "POST", 
			nickname = "findBulkLocations", 
			value = "Returns locations requested in bulk", 
			notes = "This endpoint returns locations match with the requested search terms (queryTerm[required], start, end, locationTypeIds, etc. (see example file)). "
			+ "Note: The schema of the 'geoJSON' field in the response is a GeoJSON, but 'geometry' and 'children' properties are excluded. ", 
			response = Response.class)
	@ApiResponses(value = {
			@ApiResponse(code = OK, message = "Successfully returned", response = Response.class),
			@ApiResponse(code = INTERNAL_SERVER_ERROR, message = "Internal server error"),
			@ApiResponse(code = BAD_REQUEST, message = "Invalid input")
	})
	 @ApiImplicitParams( { 
	    	@ApiImplicitParam(
	    			value = findBulkExBody, 
	    			required = true, 
	    			dataType = "List[JsonNode]",
	    			paramType = "body"
	    	)
	} )
	public Result findBulkLocations() {
		if (!(request().body().asJson() instanceof ArrayNode))
			return badRequest("Invalid input");
		List<Object> result = GeoJsonRule.findByTerm((ArrayNode)request().body().asJson());
		return ok(toJson(result));
	}
	
	/**
	 * @Deprecated replaced by {@link #findBulkLocations()}
	 */
	@Deprecated
	@Transactional
	public Result findBulk() {
		List<Object> result = GeoJsonRule.findBulk((ArrayNode)request().body().asJson());
		return ok(toJson(result));
	}
	
	private ArrayNode toJson(List<Object> list) {
		ArrayNode result = Json.newArray();
		for(Object item : list){
			result.add(Json.toJson(item));
		}
	    return result;
	}

	@Transactional
	@ApiOperation(
			httpMethod = "GET", 
			nickname = "findUniqueLocationNames", 
			value = "Returns unique location-names", 
			notes = "This endpoint returns uniqure location-names which match the requested search terms (queryTerm). "
			+ "Use 'limit' to set the maximum number to return (default is 10). ",
			response = String.class,
			produces = "application/json",
			responseContainer = "set"
	)
	@ApiResponses(value = {
			@ApiResponse(code = OK, message = "Successfully returned", response = Response.class),
			@ApiResponse(code = INTERNAL_SERVER_ERROR, message = "Internal server error"),
	})
	public Result listUniqueNames(
			@ApiParam(
					value = "Search terms delimited by a space charactor. "
					+ "The search terms are combined together with conjunction. ",
					required = true
			) 
			@QueryParam("queryTerm") 
			String queryTerm,
			@ApiParam(
					value = "Maximum number of names to return. ", 
					required = true, defaultValue = "10"
			) 
			@QueryParam("limit")
			Integer limit
	){
		Object result = LocationProxyRule.listUniqueNames(queryTerm, limit);
		return ok(Json.toJson(result));
	}
	
	/**
	 * @Deprecated Replaced by {@link #listUniqueNames}
	 */
	@Deprecated
	@Transactional
	public Result findLocationNames(String q, Integer limit){
		return listUniqueNames(q, limit);
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
			@ApiResponse(code = OK, message = "Successfully returned", response = Response.class),
			@ApiResponse(code = INTERNAL_SERVER_ERROR, message = "Internal server error"),
	})
	public Result findByPoint(
			@ApiParam(value = "Latitude in degree", required = true) @QueryParam("lat") 
			double lat,
			@ApiParam(value = "Longitude in degree", required = true) @QueryParam("long") 
			double lon
	) {
		Object result = GeoJsonRule.findByPoint(lat, lon);
		return ok(Json.toJson(result));
	}

	
	/**
	 * @Deprecated replaced by {@link #findByPoint}
	 */
	@Deprecated
	@Transactional
	public Result findByLocationPoint(double lat, double lon) {
		return findByPoint(lat,lon);
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
	@ApiOperation(
			httpMethod = "POST", 
			nickname = "findLocationByFeatureCollection", 
			value = "Returns locations by featureCollection", 
			notes = "This endpoint returns locations which intersect the submitted Geometry in body. ",
			response = Response.class
	)
	@ApiResponses(value = { 
			@ApiResponse(code = OK, message = "Successfully returned", response = Response.class),
			@ApiResponse(code = INTERNAL_SERVER_ERROR, message = "Internal server error")
	})
    @ApiImplicitParams( { 
    	@ApiImplicitParam(
    			value = "GeoJSON FeatureCollection. "
    					+ "Only geometry is required. "
    					+ findbyGeomExBody, 
    			required = true, 
    			dataType = "models.geo.FeatureCollection", 
    			paramType = "body"
    	) 
    } )
	public Result findByFeatureCollection(
			@ApiParam(
					name = "superTypeId",
					value = "superTypeId (location-type category). "
							+ "refer to " + superTypeAPI + " endpoint."
			)
			@QueryParam("superTypeId")
			Long superTypeId,
			@ApiParam(
					name = "LocationTypeId",
					value = "LocationTypeId. "
							+ "refer to " + locationTypeAPI + " endpoint."
			)
			@QueryParam("LocationTypeId")
			Long typeId) throws Exception {
		FeatureCollection fc = parseRequestAsFeatureCollection();
		response().setContentType("application/vnd.geo+json");
		return Wire.findByFeatureCollection(fc, superTypeId, typeId);
	}
	
	/**
	 * @Deprecated replaced by {@link #findByFeatureCollection}
	 */
	@Deprecated
	@Transactional
	public Result findByLocationFeatureCollection(Long superTypeId,	Long typeId) throws Exception {
		return findByFeatureCollection(superTypeId, typeId);
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

	@Transactional
	@ApiOperation(
		httpMethod = "GET",
		nickname = "findByLocationTypeId",
		value = "Returns locations gid and name with the specified type",
		notes = "This endpoint returns locations whose type matches the requested type id."
	)
	@ApiResponses(value = {
		@ApiResponse(code = OK, message = "Successfully returned"),
		@ApiResponse(code = INTERNAL_SERVER_ERROR, message = "Internal server error")
	})
	public Result findByTypeId(
		@ApiParam(
			value = "Location-type-id",
			required = true, defaultValue = "1"
		)
		@PathParam("id")
		Long typeId
		){
		List<Object> result = LocationRule.findByTypeId(typeId);
		return ok(Json.toJson(result));
	}

	@Transactional
	public Result updateCache(){
		LocationProxyRule.updateCache();
		return ok("Location-name cache updated!");
	}
}
