package controllers;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

import interactors.TopoJsonRule;
import play.db.jpa.Transactional;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;

public class TopoJsonService extends Controller {

	@Transactional
	public Result topoJson() {
		List<Long> gids = toGids((JsonNode) request().body().asJson());
		String topoJson = new TopoJsonRule().toTopoJson(gids);
		return ok(Json.toJson(topoJson));
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
