package v1.models;

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
		String key = (title != null) ? title.toLowerCase() : "";
		String key2 = (other.title != null) ? other.title.toLowerCase() : "";
		return key.compareTo(key2);
	}

}