package interactors;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import play.Logger;
import dao.LocationDao;
import dao.entities.Location;

public class LocationProxyRule {
	private static Map<Long, Location> gid2location = null;
	private static List<Location> roots = null;
	private static List<String> uniqueSortedLocationNames = null;
	
	public static void notifyChange(){
		if (gid2location != null){
			synchronized (gid2location){
				gid2location = null;
			}
		}
		
		if (roots != null){
			synchronized (roots){
				roots = null;
			}
		}
		
		if (uniqueSortedLocationNames != null){
			synchronized (uniqueSortedLocationNames){
				uniqueSortedLocationNames = null;
			}
		}
	}
	
	public static List<Location> getHierarchy() {
		if (roots == null){
			Map<Long, Location> gid2location = getGid2location();
			roots = new ArrayList<>();
			synchronized (roots) 
			{
				for (Location l: gid2location.values()){
					if (l.getParent() == null)
						roots.add(l);
				}
			}
		}
		return roots;
	}

	public static Map<Long, Location> getGid2location() {
		if (gid2location == null){
			gid2location = new LocationDao().getGid2location();
		}
		return gid2location;
	}
	
	static Location getLocation(long gid) {
		return getGid2location().get(gid);
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

	public static List<Location> getLocations(List<BigInteger> ids) {
		List<Location> result = new ArrayList<>();
		for (BigInteger id : ids){
			long gid = id.longValue();
			Location location = getLocation(gid);
			if (location == null){
				Logger.warn(gid + " not found!");
			} else {
				location.setHeadline(location.getData().getName());
				result.add(location);
			}
		}
		return result;
	}
	
	private static List<String> getUniqueSortedLocationNames(){
		if (uniqueSortedLocationNames == null){
			Map<Long, Location> map = getGid2location();
			Set<String> set = new HashSet<>();
			Collection<Location> locations = map.values();
			for (Location l : locations){
				set.add(l.getData().getName());
			}
			uniqueSortedLocationNames = new ArrayList<>();
			synchronized (uniqueSortedLocationNames) 
			{
				uniqueSortedLocationNames.addAll(set);
				Collections.sort(uniqueSortedLocationNames, String.CASE_INSENSITIVE_ORDER);
			}
		}
		return uniqueSortedLocationNames;
	}
	
	public static List<Map<String, String>> findLocationNames(String prefixNames, int limit){
		List<Map<String, String>> result = new ArrayList<>();
		if (prefixNames == null || prefixNames.trim().isEmpty())
			return result;
		
		int numWord = 0;

		List<String> names = getUniqueSortedLocationNames();
		
		String prefixName = prefixNames.trim().toLowerCase();
		
		String delim = " +";
		while (!names.isEmpty()){
			List<String> remainingNames = new ArrayList<>();
			for (String originalName : names){
				String name = originalName.replaceAll("[()]", "");
				String[] tokens = name.split(delim);
				String toBeRemoved = "";
				for (int i = 0; i < numWord; i++){
					toBeRemoved += tokens[i] + delim;
				}
				String usedName = name.replaceFirst(toBeRemoved, "");

				if (usedName.toLowerCase().startsWith(prefixName)){
					Map<String, String> map = new HashMap<>();
					map.put("name", originalName);
					result.add(map);
					if (result.size() == limit){
						remainingNames = Collections.emptyList();
						break;
					}
				} else if (tokens.length > numWord + 1){
					remainingNames.add(originalName);
				}

				
			}
			numWord++;
			names = remainingNames;
		}
		return result;
	}

}
