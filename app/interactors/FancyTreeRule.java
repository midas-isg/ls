package interactors;

import java.sql.Date;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import dao.entities.Data;
import dao.entities.Location;
import models.FancyTreeNode;

public class FancyTreeRule {

	private boolean isHideCheckbox = false;

	public List<FancyTreeNode> getAuTree() {
		return toFancyTree(LocationProxyRule.getHierarchy());
	}

	private List<FancyTreeNode> toFancyTree(List<Location> roots) {
		List<FancyTreeNode> newTree = new ArrayList<>();
		for (Location root : roots) {
			FancyTreeNode node = toNode(root, "");
			newTree.add(node);
			node.expanded = false;

		}
		Collections.sort(newTree);
		return newTree;
	}

	private FancyTreeNode toNode(Location au, String path) {
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
		if (children != null) {
			for (Location child : children) {
				if (child.getData().getLocationType().getSuperType().getId() != 3)
					continue;
				node.folder = true;
				node.expanded = false;
				node.children.add(toNode(child, node.path + "."));
			}
		}
		Collections.sort(node.children);
		return node;
	}

	private String makeTitle(Data data) {
		Date startDate = data.getStartDate();
		String fromStartDateText = "";
		if (startDate != null) {
			String startDateText = startDate.toString();
			if (!startDateText.startsWith("0"))
				fromStartDateText = startDateText + " ";
		}
		Date endDate = data.getEndDate();
		String toEndDateText = "";
		if (endDate == null) {
			if (!fromStartDateText.isEmpty()) {
				toEndDateText += "to Present";
			}
		} else {
			toEndDateText = "to " + endDate;
		}
		String variant = "";
		if (fromStartDateText.length() + toEndDateText.length() > 0)
			variant = " [" + fromStartDateText + toEndDateText + "]";
		return data.getName() + variant;
	}

	void changeClassToInstanceNode(FancyTreeNode classNode, FancyTreeNode instanceNode) {
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
}
