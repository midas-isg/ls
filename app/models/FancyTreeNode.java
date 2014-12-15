package models;

import java.util.List;

public class FancyTreeNode implements Comparable<FancyTreeNode> {
	public String title;
	public String key;
	public Boolean icon;
	public Boolean hideCheckbox;
	public Boolean folder;
	public Boolean unselectable;
	public Boolean expanded = true;
	public List<FancyTreeNode> children;
	public String tooltip;

	public String type;
	public String path;

	@Override
	public int compareTo(FancyTreeNode other) {
		return title.toLowerCase().compareTo(other.title.toLowerCase());
	}

}