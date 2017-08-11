package controllers;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiImplicitParam;
import com.wordnik.swagger.annotations.ApiImplicitParams;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;

import interactors.TopoJsonRule;
import play.db.jpa.Transactional;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;

@Api(value = "/api/locations", description = "Endpoint for locations")
public class TopoJsonService extends Controller {

	@Transactional
	@ApiOperation(
			httpMethod = "POST", 
			nickname = "TopoJson", 
			value = "generates a TopoJson from input gids", 
			notes = "This endpoint returns a "
					+ "<a target='_blank' href='https://github.com/topojson/topojson/wiki'> TopoJson </a> "
					+ "created from geometries of the input location-ids."
			)
	@ApiResponses(value = {
			@ApiResponse(code = OK, message = "Successfully returned"),
			@ApiResponse(code = INTERNAL_SERVER_ERROR, message = "Internal server error"),
			@ApiResponse(code = BAD_REQUEST, message = "Invalid input")
	})
	@ApiImplicitParams({ 
	    	@ApiImplicitParam(
	    			value = "{\"gids\":[...]}", 
	    			required = true, 
	    			paramType = "body"
	    	)
	})
	public Result topoJson() {
		List<Long> gids = toGids((JsonNode) request().body().asJson());
		String topoJson = new TopoJsonRule().toTopoJson(gids);
		Result result = ok(Json.parse(topoJson));
		return result; 
	}

	private List<Long> toGids(JsonNode node) {
		if (node == null)
			return null;
		JsonNode gids = node.findPath("gids");
		return toList(gids);
	}

	private List<Long> toList(JsonNode gids) {
		if (gids == null)
			return null;
		List<Long> list = new ArrayList<>();
		Iterator<JsonNode> elements = gids.elements();
		while (elements.hasNext()) {
			list.add(elements.next().asLong());
		}
		return list;
	}

}
