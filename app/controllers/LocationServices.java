package controllers;

import static interactors.Util.putAsStringIfNotNull;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;

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
import interactors.GeoJsonRule;
import interactors.GeometryRule;
import interactors.KmlRule;
import interactors.LocationProxyRule;
import interactors.LocationRule;
import interactors.RequestRule;
import models.FeatureKey;
import models.Request;
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
import play.mvc.Http.Context;

@Api(value = "/api/locations", description = "Endpoint for locations")
public class LocationServices extends Controller {
	public static final String CURRENT_VERSION = "2";
	public static final String OLD_VERSION = "1";
	public static final String FORMAT_GEOJSON = "geojson";
	public static final String FORMAT_GEOJSONP = "jsonp";
	public static final String FORMAT_APOLLOJSON = "json";
	public static final String FORMAT_APOLLOXML = "xml";
	public static final String FORMAT_KML = "kml";
	public static final String FORMAT_DEFAULT = "geojson";
	private static final String UNIQUE_VIOLATION = "23505";
	private static final String findBulkEx = "find-bulk.json";
	private static final String findBulkExV2 = "find-bulk-v2.json";
	private static final String findBulkExBody = "Only \"queryTerm\" is required. See an example of body for version "
			+ CURRENT_VERSION + " at "
			+ "<a target='_blank' href='assets/examples/api/" + findBulkExV2 + "'>" + findBulkExV2 + "</a></br>"
			+ "Version " + OLD_VERSION + " body example: "
					+ "<a target='_blank' href='assets/examples/api/" + findBulkEx + "'>" + findBulkEx + "</a> ";;
	private static final String findByTermEx = "find-by-term.json";
	private static final String findByTermExV2 = "find-by-term-v2.json";
	private static final String findByTermExBody = "Only \"queryTerm\" is required. See a request example for version "
			+ CURRENT_VERSION + " at "
			+ "<a target='_blank' href='assets/examples/api/" + findByTermExV2 + "'>" + findByTermExV2 + "</a></br>"
			+ "Version " + OLD_VERSION + " request example: "
					+ "<a target='_blank' href='assets/examples/api/" + findByTermEx + "'>" + findByTermEx + "</a> ";
	private static final String findByFilterEx = "find-by-filter.json";
	private static final String findByFilterExBody = "Returns locations matched by filters. </br>"
			+ "See a request example at "
			+ "<a target='_blank' href='assets/examples/api/" + findByFilterEx + "'>" + findByFilterEx + "</a></br>";
	private static final String findbyGeomEx = "AuMaridiTown.geojson";
	private static final String findbyGeomExBody = "See an example of body at "
			+ "<a target='_blank' href='assets/examples/api/" + findbyGeomEx + "'>" + findbyGeomEx + "</a> ";
	private static final String circleExample = "circle.json";
	private static final String relativeLocationsEx = "relative-locations.json";
	private static final String featureFields = "featureFields.txt";
	private static final String superTypeAPI = "/api/super-types";
	private static final String locationTypeAPI = "/api/location-types";

	@Transactional
	@ApiOperation(
			httpMethod = "GET", 
			nickname = "readLocation", 
			value = "Returns a location by ID", 
			notes = "This endpoint returns a location in the requested format (format) by ID (gid). "
			+ "The ID is usually called as GID by Geographic Information System (GIS) "
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
					value = "Comma separated values</br>"
							+ "Includes only the given fields in the response feature</br>"
							+ "See "
							+ "<a target='_blank' href='assets/examples/api/" + featureFields + "'>possible values</a> ", 
					required = false
			) 
			@QueryParam("_onlyFeatureFields")
			String onlyFeatureFields,
			
			@ApiParam(
					value = "Comma separated values</br>"
							+ "Excludes the given fields from the response feature</br>"
							+ "See "
							+ "<a target='_blank' href='assets/examples/api/" + featureFields + "'>possible values</a> ", 
					required = false
			) 
			@QueryParam("_excludedFeatureFields")
			String excludedFeatureFields,
			
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
		
		Request req = RequestRule.toRequestForCustomizedFeatureFields(onlyFeatureFields, excludedFeatureFields);
		
		switch (format.toLowerCase()) {
		case FORMAT_GEOJSON:
			return asGeoJson(location, req);
		case FORMAT_GEOJSONP:
			return asGeoJsonp(location, "jsonpCallback", req);
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
		return asGeoJsonp(location, callback, new Request());
	}

	static Result asGeoJsonp(Location location, String callback, Request req) {
		response().setContentType("text/javascript");
		return ok(Jsonp.jsonp(callback, Json.toJson(Wire.asFeatureCollection(location, req))));
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
			+ "To do pagination, use 'limit' and 'offset'.</br>"
			+ "Note: In vestion 1, response is not a valid geoJSON ('geometry' property is removed from FeatureCollection response).</br>"
			+ "As of version 2, no response field is excluded by default. Use _onlyFeatureFields or _excludedFeatureFields for filtering fields.</br>"
			+ "Some parameters are only available in version 2 and later. See parameter descriptions.",
			response = FeatureCollection.class)
	@ApiResponses(value = {
			@ApiResponse(code = OK, message = "Successfully returned", response = FeatureCollection.class),
			@ApiResponse(code = INTERNAL_SERVER_ERROR, message = "Internal server error"),
			@ApiResponse(code = BAD_REQUEST, message = "Bad request") 
	})
	public Result filterByTerm(
			@ApiParam(
					value = "Search terms delimited by a space character. "
					+ "The search terms are combined together with disjunction by default. ",
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
					value = "Includes only the given fields in feature objects (available as of version 2)</br>"
							+ "Example: _onlyFeatureFields= properties.gid, properties.name", 
					required = false
			) 
			@QueryParam("_onlyFeatureFields")
			String onlyFeatureFields,
			
			@ApiParam(
					value = "Excludes the given fields from feature objects (available as of version 2)</br>"
							+ "Example: _excludedFeatureFields= geometry, properties.children", 
					required = false
			) 
			@QueryParam("_excludedFeatureFields")
			String excludedFeatureFields,
			
			@ApiParam(
					value = "Optional. Maintained for backward compatibility. Will be ignored if \"queryTerm\" provided.", 
					defaultValue = "",
					required = false
			)
			@QueryParam("q")
			String q,
			
			@ApiParam(
					value = "If false, returns only gids</br>"
							+ "NOT supported as of version 2, use _onlyFeatureFields=properties.gid for non-verbose results", 
					defaultValue = "true"
			) 
			@QueryParam("verbose") 
			Boolean verbose,
			
			@ApiParam(
					value = "API version", 
					required = false, defaultValue = OLD_VERSION
				) 
				@QueryParam("_v") 
					String version
		) {
		if (queryTerm == null)
			queryTerm = q;
		
		Object result;
		switch (version) {
		case OLD_VERSION:
			result = v1.interactors.GeoJsonRule.filterByTerm(queryTerm, limit, offset, searchOtherNames, verbose);
			break;
		case CURRENT_VERSION:
			result = GeoJsonRule.filterByTerm(onlyFeatureFields, excludedFeatureFields, queryTerm, limit, offset, searchOtherNames);
			break;
		default:
			return badVersion(version);
		}
		return ok(Json.toJson(result));
	}

	@Transactional
	@ApiOperation(
			httpMethod = "POST", 
			nickname = "Find", 
			value = "Finds locations by name, other-names, or code", 
			notes = "<p>Receives a single query as shown in the example.</p>"
			+ "<b>In version 1:</b> "
			+ "<ul> "
			+ "<li>response is not a valid geoJSON (<i>'geometry'</i> property is removed from FeatureCollection response).</li> "
			+ "<li><i>\"properties.children\"</i> is excluded from features.</li> "
			+ "</ul> "
			+ "<b>As of version 2:</b> "
			+ "<ul> "
			+ "<li>use <i>\"onlyFeatureFields\"</i> or <i>\"excludedFeatureFields\"</i> for filtering fields.</li> See "
				+ "<a target='_blank' href='assets/examples/api/" + featureFields + "'>here</a> for possible values</li>. "
			+ "<li>request filter syntax changed. See <a href='assets/examples/api/" + findByTermExV2 + "'>" + findByTermExV2 + "</a></li> "
			+ "<li>\"verbose\" option is not supported. Use <i>\"_onlyFeatureFields\":\"properties.gid\"</i> for non-verbose results.</li> "
			+ "<li>the default for search logic is <i>\"logic\":\"OR\"</i>. For conjunction between query-terms use <i>\"logic\":\"AND\"</i></li> "
			+ "</ul>"
			+ "<b>Results are sorted by:</b>"
			+ "<ul>"
			+ "<li><i>#unique matches</i> in the location name/code (<i>rank</i>) in descending order</li>"
			+ "<li><i>#unique terms</i> in location name/code in ascending order</li>"
			+ "<li>location <em>area</em> in descending order</li>"
			+ "<li><i>alphabetical order</i> of location name/code ascendingly."
			+ "</ul>",
			response = FeatureCollection.class)
	@ApiResponses(value = {
			@ApiResponse(code = OK, message = "Successfully returned", response = FeatureCollection.class),
			@ApiResponse(code = INTERNAL_SERVER_ERROR, message = "Internal server error"),
			@ApiResponse(code = BAD_REQUEST, message = "Invalid input")
	})
	@ApiImplicitParams({ 
	    	@ApiImplicitParam(
	    			value = findByTermExBody, 
	    			required = true, 
	    			dataType = "models.Request",
	    			paramType = "body"
	    	)
	})
	public Result findByTerm(
			@ApiParam(
				value = "API version", 
				required = false, defaultValue = OLD_VERSION
			) 
			@QueryParam("_v") 
			String version
		) {
		if (!(request().body().asJson() instanceof JsonNode) ||
				request().body().asJson() instanceof ArrayNode)
			return badRequest("Invalid input! JsonNode or ArrayNode expected.");

		ArrayNode arrayNode = toArrayNode((JsonNode)request().body().asJson());
		List<Object> result;
		
		switch (version) {
		case OLD_VERSION:
			result = v1.interactors.GeoJsonRule.findByTerm(arrayNode);
			break;
		case CURRENT_VERSION:
			result = GeoJsonRule.findByTerm(arrayNode);
			break;
		default:
			return badVersion(version);
		}
		
		if(result == null || result.isEmpty())
			return ok(Json.newObject());
		return ok(Json.toJson(result.get(0)));
		
	}
	
	@Transactional
	@ApiOperation(httpMethod = "POST", 
					nickname = "readLocationByFilter", 
					value = "Returns locations based on input filter", 
					notes = "This endpoint returns locations matched by the input filter.",
					response = FeatureCollection.class)
	@ApiResponses(value = {
			@ApiResponse(code = OK, message = "Successful retrieval of location", response = FeatureCollection.class),
			@ApiResponse(code = INTERNAL_SERVER_ERROR, message = "Internal server error"), })
	@ApiImplicitParams({
			@ApiImplicitParam(value = findByFilterExBody,
								required = true, 
								dataType = "models.Request", 
								paramType = "body") })
	public Result findByFilter() {
		JsonNode req = (JsonNode) request().body().asJson();
		Object result = GeoJsonRule.findByFilters(req);
		return ok(Json.toJson(result));
	}

	private Status badVersion(String version) {
		return badRequest("Invalid api version: " + version + ". current_version=" + CURRENT_VERSION);
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
			notes = "This endpoint returns locations match with the requested search terms "
			+ "(queryTerm[required], start, end, locationTypeIds, etc. (see example file)).</br>"
			+ "Note: In version 1, response is not a valid geoJSON ('geometry' property is removed from FeatureCollection response).</br>"
			+ "In version 1, properties.children is excluded from features.</br>"
			+ "As of version 2, no response fields are excluded by default.</br>"
			+ "As of version 2, request filter-key names changed: 'includeOnly' -> 'onlyFeatureFields' & 'exclude' -> 'excludedFeatureFields'.</br>"
			+ "As of version 2, request filter syntax changed. Refer to <a href='assets/examples/api/" + findBulkExV2 + "'>" + findBulkExV2 + "</a>",
			response = FeatureCollection.class)
	@ApiResponses(value = {
			@ApiResponse(code = OK, message = "Successfully returned", response = FeatureCollection.class),
			@ApiResponse(code = INTERNAL_SERVER_ERROR, message = "Internal server error"),
			@ApiResponse(code = BAD_REQUEST, message = "Invalid input")
	})
	 @ApiImplicitParams( { 
	    	@ApiImplicitParam(
	    			value = findBulkExBody, 
	    			required = true, 
	    			dataType = "Array[models.Request]",
	    			paramType = "body"
	    	)
	} )
	public Result findBulkLocations(
			@ApiParam(
				value = "API version", 
				required = false, defaultValue = OLD_VERSION
			) 
			@QueryParam("_v") 
				String version
		) {
		if (!(request().body().asJson() instanceof ArrayNode))
			return badRequest("Invalid input! ArrayNode expected.");
		ArrayNode arrayNode = (ArrayNode)request().body().asJson();
		List<Object> result;
		switch (version) {
		case OLD_VERSION:
			result = v1.interactors.GeoJsonRule.findByTerm(arrayNode);
			break;
		case CURRENT_VERSION:
			result = GeoJsonRule.findByTerm(arrayNode);
			break;
		default:
			return badVersion(version);
		}
		return ok(toJson(result));
	}
	
	/**
	 * @Deprecated replaced by {@link #findBulkLocations()}
	 */
	@Deprecated
	@Transactional
	public Result findBulk() {
		Logger.warn("WARNING! " + Context.current().request().uri() + " is deprecated and may stop being available in the future!");
		
		List<Object> result = v1.interactors.GeoJsonRule.findBulk((ArrayNode)request().body().asJson());
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
			notes = "This endpoint returns unique location-names which match the requested search terms (queryTerm). "
			+ "Use 'limit' to set the maximum number to return (default is 10). ",
			response = List.class,
			produces = "application/json",
			responseContainer = "set"
	)
	@ApiResponses(value = {
			@ApiResponse(code = OK, message = "Successfully returned", response = List.class),
			@ApiResponse(code = INTERNAL_SERVER_ERROR, message = "Internal server error"),
	})
	public Result listUniqueNames(
			@ApiParam(
					value = "Search terms delimited by a space character. "
					+ "The search terms are combined together with a conjunction. ",
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
		Logger.warn("WARNING! " + Context.current().request().uri() + " is deprecated and may stop being available in the future!");
		
		return listUniqueNames(q, limit);
	}

	@Transactional
	@ApiOperation(
			httpMethod = "GET", 
			nickname = "findLocationsByPoint", 
			value = "Returns locations by coordinate search", 
			notes = "This endpoint returns locations whose geometry encompasses the submitting coordinate. "
			+ "The coordinate is defined by longitude (long) and latitude (lat). "
			+ "</br>Note:</br>"
			+ " In version 1, response is not a valid geoJSON ('geometry' property is removed from FeatureCollection response).</br>"
			+ " As of version 2, no response field is excluded by default. Use _onlyFeatureFields or _excludedFeatureFields to include/remove fields.</br>"
			+ " Some parameters are only available in version 2 and later. See parameter descriptions.", 
			response = FeatureCollection.class
	)
	@ApiResponses(value = {
			@ApiResponse(code = OK, message = "Successfully returned", response = FeatureCollection.class),
			@ApiResponse(code = INTERNAL_SERVER_ERROR, message = "Internal server error"),
	})
	public Result findByPoint(
			@ApiParam(value = "Latitude in degree", required = true) @QueryParam("lat") 
			double lat,
			@ApiParam(value = "Longitude in degree", required = true) @QueryParam("long") 
			double lon,
			@ApiParam(value = "If false, returns only gids</br>"
					+ "NOT supported as of version 2, use _onlyFeatureFields=properties.gid for non-verbose results",
					defaultValue = "true")
			@QueryParam("verbose") 
			Boolean verbose,
			
			@ApiParam(
				value = "Includes only the given fields in feature objects (as of version 2)", 
				required = false
			) 
			@QueryParam("_onlyFeatureFields")
			String onlyFeatureFields,
					
			@ApiParam(
				value = "Excludes the given fields from feature objects (as of version 2)", 
				required = false
			) 
			@QueryParam("_excludedFeatureFields")
			String excludedFeatureFields,
			
			@ApiParam(
				value = "API version", 
				required = false, defaultValue = OLD_VERSION
			) 
			@QueryParam("_v") 
			String version
	) {
		Object result;
		switch (version) {
		case OLD_VERSION:
			result = v1.interactors.GeoJsonRule.findByPoint(lat, lon, verbose);
			break;
		case CURRENT_VERSION:
			result = GeoJsonRule.findByPoint(onlyFeatureFields, excludedFeatureFields, lat, lon);
			break;
		default:
			return badVersion(version);
		}
		return ok(Json.toJson(result));
	}

	
	/**
	 * @Deprecated replaced by {@link #findByPoint}
	 */
	@Deprecated
	@Transactional
	public Result findByLocationPoint(double lat, double lon, boolean verbose) {
		Logger.warn("WARNING! " + Context.current().request().uri() + " is deprecated and may stop being available in the future!");
		
		return findByPoint(lat, lon, verbose, null, null, OLD_VERSION);
	}

	@Transactional
	@ApiOperation(
			httpMethod = "POST", 
			nickname = "createLocation", 
			value = "Creates a location", 
			notes = "<p>This endpoint creates a location using submitted GeoJSON FeatureCollection object "
					+ "in the body (with an exception of a 'circle' which is not a GeoJSON) and returns the URI via the 'Location' Header in the response. "
					+ "Currently, no content is returned in the body. </p>"
					+ "<p>Circle is created using ST_Buffer(geography g1, float radius_of_buffer_in_meters) in postGIS 2.1.5.</p>", 
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
    			value = "<p>FeatureCollection</p>"
    					+ "<p>See examples:</p>"
    					+ "<p><a target='_blank' href='assets/examples/api/"
    					+ findbyGeomEx + "'>MuliPolygon</a></p>"
    					+ "<p><a target='_blank' href='assets/examples/api/"
    					+ circleExample + "'>Circle</a></p>", 
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
	Result asGeoJson(Location location, Request req) {
		response().setContentType("application/vnd.geo+json");
		return ok(Json.toJson(Wire.asFeatureCollection(location, req)));
	}

	@Transactional
	@ApiOperation(
			httpMethod = "PUT", 
			nickname = "updateLocation", 
			value = "Updates a location", 
			notes = "This endpoint does full update the given location "
					+ "identified by 'gid' with submitted GeoJSON "
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
			notes = "This endpoint returns locations which intersect the submitted Geometry in the body. "
			+ "Note: response is not a valid geoJSON ('geometry' property is removed from FeatureCollection response). ",
			response = FeatureCollection.class
	)
	@ApiResponses(value = { 
			@ApiResponse(code = OK, message = "Successfully returned", response = FeatureCollection.class),
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
					name = "typeId",
					value = "LocationTypeId. "
							+ "refer to " + locationTypeAPI + " endpoint."
			)
			@QueryParam("typeId")
			Long typeId,
			@ApiParam(
					value = "If false, returns only gids", 
					defaultValue = "true"
			) 
			@QueryParam("verbose") 
			Boolean verbose
			) throws Exception {
		FeatureCollection fc = parseRequestAsFeatureCollection();
		response().setContentType("application/vnd.geo+json");
		return Wire.findByFeatureCollection(fc, superTypeId, typeId, verbose);
	}
	
	/**
	 * @Deprecated replaced by {@link #findByFeatureCollection}
	 */
	@Deprecated
	@Transactional
	public Result findByLocationFeatureCollection(Long superTypeId,	Long typeId, boolean verbose) throws Exception {
		Logger.warn("WARNING! " + Context.current().request().uri() + " is deprecated and may stop being available in the future!");
		
		return findByFeatureCollection(superTypeId, typeId, verbose);
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

		public static FeatureCollection asFeatureCollection(Location location, Request req) {
			FeatureCollection fc = GeoJsonRule.asFeatureCollection(location, req);
			return fc;
		}
		
		public static FeatureCollection asFeatureCollection(Location location) {
			FeatureCollection fc = GeoJsonRule.asFeatureCollection(location, new Request());
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

		public static Result findByFeatureCollection(FeatureCollection fc, Long superTypeId, Long typeId, 
				boolean verbose) {
			FeatureGeometry geometry = GeoJsonRule.asFeatureGeometry(fc);
			String geo = Json.toJson(geometry).toString();
			List<BigInteger> gids = GeometryRule.findGidsByGeometry(geo, superTypeId, typeId);
			Map<String, Object> properties = new HashMap<>();
			putAsStringIfNotNull(properties, "superTypeId", superTypeId);
			putAsStringIfNotNull(properties, "typeId", typeId);
			putAsStringIfNotNull(properties, "verbose", verbose);
			if(verbose){
				List<Location> locations = LocationRule.getLocations(gids);
				Request req = new Request();
				req.setExcludedFeatureFields(Arrays.asList(new String[] { FeatureKey.GEOMETRY }));
				FeatureCollection result = GeoJsonRule.toFeatureCollection(locations, req);
				putAsStringIfNotNull(properties, "resultSize", locations.size());
				result.setProperties(properties);				
				return ok(Json.toJson(result));
			}
			else {
				Map<String, Object> result = new HashMap<>();
				result.put("gids", gids);
				putAsStringIfNotNull(properties, "resultSize", gids.size());
				result.put("properties", properties);
				return ok(Json.toJson(result));
			}
		}
	}

	@Transactional
	@ApiOperation(
		httpMethod = "GET",
		nickname = "findByLocationTypeId",
		value = "Returns locations with the specified type-id",
		notes = "This endpoint returns locations with the requested type-id.</br>"
				+ "Note: In version 1, 'geometry' and 'property.children' are removed from response.</br>"
				+ "Some parameters are only available in version 2 and later. See parameter descriptions.",
		response = FeatureCollection.class
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
		Long typeId,
		
		@ApiParam(
				value = "Comma separated values</br>"
						+ "Includes only the given fields in the response feature <b>(as of version 2)</b></br>"
						+ "See "
						+ "<a target='_blank' href='assets/examples/api/" + featureFields + "'>possible values</a> ", 
				required = false
		) 
		@QueryParam("_onlyFeatureFields")
		String onlyFeatureFields,
		
		@ApiParam(
				value = "Comma separated values</br>"
						+ "Excludes the given fields from the response feature <b>(as of version 2)</b></br>"
						+ "See "
						+ "<a target='_blank' href='assets/examples/api/" + featureFields + "'>possible values</a> ", 
				required = false
		) 
		@QueryParam("_excludedFeatureFields")
		String excludedFeatureFields,
			
		@ApiParam(
			value = "Maximum number of locations to return (as of version 2)", 
			required = true, defaultValue = "10"
		) 
		@QueryParam("limit") 
			Integer limit,
				
		@ApiParam(
			value = "Page offset if number of locations exceeds limit (as of version 2)", 
			required = true, defaultValue = "0"
		) 
		@QueryParam("offset") 
			Integer offset,
			
		@ApiParam(
			value = "API version", 
			required = false, defaultValue = OLD_VERSION
		) 
		@QueryParam("_v") 
			String version
		){
		Object result;
		switch (version) {
		case OLD_VERSION:
			result = v1.interactors.GeoJsonRule.findByTypeId(typeId);
			return ok(Json.toJson(result));
		case CURRENT_VERSION:
			result = GeoJsonRule.findByTypeId(onlyFeatureFields, excludedFeatureFields, typeId, limit, offset);
			return ok(Json.toJson(result));
		default:
			return badVersion(version);
		}
	}

	@Transactional
	public Result updateCache(){
		LocationProxyRule.initializeCache();
		return ok("An update-cache request was scheduled.");
	}
	
	@Transactional
	@ApiOperation(httpMethod = "POST", 
					nickname = "readRelativeLocations", 
					value = "Returns ALCs for ancestors and descendants of given ALC", 
					notes = "<p>This endpoint returns a list of Apollo Location Codes (ALCs) from the locations' hierarchy, which consists of ancestors "
							+ "and descendants of a given ALC.</p>"
							+ "<p>Returned ALCs can be constrained to a list of given ALCs using "
							+ "'onlyALCs' parameter.</p>"
							+ "<a target='_blank' href='assets/examples/api/" + relativeLocationsEx + "'>request body example</a>",
					response = List.class)
	@ApiResponses(value = {
			@ApiResponse(code = OK, message = "Successful retrieval of ALCs", response = List.class),
			@ApiResponse(code = INTERNAL_SERVER_ERROR, message = "Internal server error"),
			@ApiResponse(code = BAD_REQUEST, message = "Format is not supported") })
	@ApiImplicitParams({
		@ApiImplicitParam(value = "<a target='_blank' href='assets/examples/api/" + relativeLocationsEx + "'>request body example</a>",
				required = true,
				paramType = "body") })
	public Result relativeLocations(){
		JsonNode req = request().body().asJson();
		List<Long> result = LocationRule.getRelative(req);
		return ok(Json.toJson(result));
	}
}