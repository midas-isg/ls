package controllers;


import java.sql.Date;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import models.FancyTreeNode;
import dao.entities.Data;
import dao.entities.Location;

public class TreeViewAdapter {
	private static boolean isHideCheckbox = false;

	public static List<FancyTreeNode> toFancyTree(
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

	static FancyTreeNode toNode(Location au, String path) {
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

	private static String makeTitle(Data data) {
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

	public static List<FancyTreeNode> removeUncomposable(List<FancyTreeNode> input){
		if (input == null)
			return null;
		String TYPE_EZ = "Epidemic Zone";
		String TYPE_PUMA = "PUMA";
		Iterator<FancyTreeNode> iterator = input.iterator();
		while (iterator.hasNext()) {
			FancyTreeNode node = iterator.next();
			if (node.type.equalsIgnoreCase(TYPE_EZ)){
				iterator.remove();
			} else 	if (node.type.equalsIgnoreCase(TYPE_PUMA)){
				iterator.remove();
			} else {
				removeUncomposable(node.children);
			}
		}
		return input;
	}
	
	public static List<FancyTreeNode> replaceParentWithOneChildButNotRoot(
			List<FancyTreeNode> input) {
		if (input == null)
			return null;
		for (FancyTreeNode node : input) {
			replaceParentWithOneChild(node.children);
		}
		return input;
	}

	public static List<FancyTreeNode> replaceParentWithOneChild(
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

	static void changeClassToInstanceNode(FancyTreeNode classNode,
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

	public static List<FancyTreeNode> removeJunior(List<FancyTreeNode> input) {
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

	private static FancyTreeNode findJunior(List<FancyTreeNode> children,
			String title) {
		if (children == null)
			return null;
		for (FancyTreeNode node : children) {
			if (node.title.equalsIgnoreCase(title))
				return node;
		}
		return null;
	}

	public static List<FancyTreeNode> removeParentWithNoChild(
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
