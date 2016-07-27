package controllers;


import java.util.List;

import interactors.LocationProxyRule;
import models.FancyTreeNode;
import play.db.jpa.Transactional;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;

public class TreeView extends Controller {

	private static Status auTree = null;

	@Transactional
	public synchronized Result tree() {
		if (auTree == null){
			List<FancyTreeNode> tree = LocationProxyRule.getAuTree();
			auTree = okJson(tree);
		}
		return auTree;
	}
	
	Status okJson(Object resultObject) {
		return ok(Json.toJson(resultObject));
	}
}
