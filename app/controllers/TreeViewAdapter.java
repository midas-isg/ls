package controllers;


import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import models.FancyTreeNode;
import dao.entities.AdministrativeUnit;
import dao.entities.Data;

public class TreeViewAdapter {
	private static boolean isHideCheckbox = false;

	public static List<FancyTreeNode> toFancyTree(
			List<AdministrativeUnit> roots) {
		List<FancyTreeNode> newTree = new ArrayList<>();
		for (AdministrativeUnit root : roots) {
			FancyTreeNode node = toNode(root);
			newTree.add(node);
			node.expanded = false;
		}
		Collections.sort(newTree);
		return newTree;
	}

	static FancyTreeNode toNode(AdministrativeUnit au) {
		FancyTreeNode node = new FancyTreeNode();
		Data data = au.getData();
		node.title = data.getName();
		node.path = data.getCodePath();
		node.tooltip = node.path;
		node.key = au.getGid() + "";
		node.icon = false;
		node.type = data.getCodePath().split("\\.")[0];
		node.hideCheckbox = isHideCheckbox;
		node.unselectable = false;
		node.children = new ArrayList<>();
		List<AdministrativeUnit> children = au.getChildren();
		if (children != null){
			for (AdministrativeUnit child : children) {
				node.folder = true;
				node.children.add(toNode(child));
			}
		}
		Collections.sort(node.children);
		return node;
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
