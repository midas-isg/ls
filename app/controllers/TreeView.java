package controllers;


import interactors.LocationProxyRule;

import java.sql.Date;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import models.FancyTreeNode;
import play.db.jpa.Transactional;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import dao.entities.Data;
import dao.entities.Location;

public class TreeView extends Controller {
	private static final long superTypeId_au = 3L;
	private static boolean isHideCheckbox = false;

	private static Status auTree = null;
	private static Set<String> auTypes = null;

	@Transactional
	public synchronized Result tree() {
		if (auTree == null){
			List<FancyTreeNode> tree = toFancyTree(LocationProxyRule.getHierarchy());
			auTypes = null;
			auTree = okJson(removeNonAu(tree));
			auTypes = null;
		}
		return auTree;
	}
	
	Status okJson(Object resultObject) {
		return ok(Json.toJson(resultObject));
	}

	private List<FancyTreeNode> toFancyTree(
			List<Location> roots) {
		List<FancyTreeNode> newTree = new ArrayList<>();
		for (Location root : roots) {
			FancyTreeNode node = toNode(root, "");
			newTree.add(node);
			node.expanded = false;
		}
		Collections.sort(newTree);
		return newTree;
	}

	FancyTreeNode toNode(Location au, String path) {
		FancyTreeNode node = new FancyTreeNode();
		Data data = au.getData();
		node.title = makeTitle(data);
		node.path = path + data.getName();
		node.tooltip = node.path;
		node.key = au.getGid() + "";
		node.icon = false;
		node.type = data.getLocationType().getName();
		node.hideCheckbox = isHideCheckbox;
		node.unselectable = false;
		node.children = new ArrayList<>();
		List<Location> children = au.getChildren();
		if (children != null){
			for (Location child : children) {
				node.folder = true;
				node.expanded = false;
				node.children.add(toNode(child, node.path +  "."));
			}
		}
		Collections.sort(node.children);
		return node;
	}

	private String makeTitle(Data data) {
		Date startDate = data.getStartDate();
		String fromStartDateText = "";
		if (startDate != null){
			String startDateText = startDate.toString();
			if (! startDateText.startsWith("0"))
				fromStartDateText =  startDateText + " ";
		}
		Date endDate = data.getEndDate();
		String toEndDateText = "";
		if (endDate == null) {
			if (! fromStartDateText.isEmpty()){
				toEndDateText += "to Present";
			}
		} else {
			toEndDateText = "to " + endDate;
		}
		String variant = "";
		if (fromStartDateText.length() + toEndDateText.length() > 0)		
			variant = " [" + fromStartDateText  + toEndDateText + "]";
		return data.getName() + variant; 
	}

	public List<FancyTreeNode> removeNonAu(List<FancyTreeNode> input){
		if (input == null)
			return null;
		if (auTypes == null){
			auTypes = new HashSet<>();
			auTypes.addAll(ListServices.Wire.findLocationTypeNamesBySuperTypeId(superTypeId_au));
		}
		
		Iterator<FancyTreeNode> iterator = input.iterator();
		while (iterator.hasNext()) {
			FancyTreeNode node = iterator.next();
			if (! auTypes.contains(node.type)){
				iterator.remove();
			} else {
				removeNonAu(node.children);
			}
		}
		return input;
	}
	
	public List<FancyTreeNode> replaceParentWithOneChildButNotRoot(
			List<FancyTreeNode> input) {
		if (input == null)
			return null;
		for (FancyTreeNode node : input) {
			replaceParentWithOneChild(node.children);
		}
		return input;
	}

	public List<FancyTreeNode> replaceParentWithOneChild(
			List<FancyTreeNode> input) {
		if (input == null)
			return null;
		for (FancyTreeNode node : input) {
			replaceParentWithOneChild(node.children);
			if (node.children != null && node.children.size() == 1) {
				FancyTreeNode child = node.children.get(0);
				if (child.folder != null && child.folder)
					return input;
				changeClassToInstanceNode(node, child);
				node.children.remove(child);
			}
		}
		Collections.sort(input);
		return input;
	}

	void changeClassToInstanceNode(FancyTreeNode classNode,
			FancyTreeNode instanceNode) {
		classNode.title = instanceNode.title;
		classNode.path = instanceNode.path;
		classNode.tooltip = instanceNode.tooltip;
		classNode.key = instanceNode.key;
		classNode.icon = instanceNode.icon;
		classNode.folder = instanceNode.folder;
		classNode.type = instanceNode.type;
		classNode.unselectable = instanceNode.unselectable;
		classNode.hideCheckbox = instanceNode.hideCheckbox;
	}

	public List<FancyTreeNode> removeJunior(List<FancyTreeNode> input) {
		if (input == null)
			return null;
		for (FancyTreeNode node : input) {
			removeJunior(node.children);
			FancyTreeNode junior = findJunior(node.children, node.title);
			if (junior != null) {
				changeClassToInstanceNode(node, junior);
				node.children.remove(junior);
			}
		}
		return input;
	}

	private FancyTreeNode findJunior(List<FancyTreeNode> children,
			String title) {
		if (children == null)
			return null;
		for (FancyTreeNode node : children) {
			if (node.title.equalsIgnoreCase(title))
				return node;
		}
		return null;
	}

	public List<FancyTreeNode> removeParentWithNoChild(
			List<FancyTreeNode> input) {
		if (input == null)
			return null;
		Iterator<FancyTreeNode> iterator = input.iterator();
		while (iterator.hasNext()) {
			FancyTreeNode node = iterator.next();
			List<FancyTreeNode> children = node.children;
			removeParentWithNoChild(children);
			if (children != null && children.isEmpty())
				iterator.remove();
		}
		return input;
	}
}
