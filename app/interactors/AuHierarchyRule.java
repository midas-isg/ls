package interactors;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import dao.AuDao;
import dao.entities.Location;

public class AuHierarchyRule {
	private static Map<Long, Location> gid2location = null;
	private static List<Location> roots = null;
	
	public static void notifyChange(){
		gid2location = null;
		roots = null;
	}
	
	public static List<Location> getHierarchy() {
		//if (roots == null){
			Map<Long, Location> gid2location = getGid2location();
			roots = new ArrayList<>();
			for (Location l: gid2location.values()){
				if (l.getParent() == null)
					roots.add(l);
			}
		//}
		return roots;
	}

	public static Map<Long, Location> getGid2location() {
		//if (gid2location == null){
			gid2location = new AuDao().getGid2location();
		//}
		return gid2location;
	}

	public static List<Location> getLineage(long gid) {
		Location l = getGid2location().get(gid);
		return getLineage(l);
	}

	static LinkedList<Location> getLineage(Location l) {
		if (l == null)
			return null;

		LinkedList<Location> lineage = new LinkedList<>();
		Location parent = l.getParent();
		while (parent != null){
			lineage.addFirst(parent);
			parent = parent.getParent();
		}
		return lineage;
	}
}