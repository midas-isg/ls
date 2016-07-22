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
import java.util.regex.Pattern;

import play.Logger;
import dao.LocationDao;
import dao.entities.AltName;
import dao.entities.Location;

public class LocationProxyRule {
	private static Map<Long, Location> gid2location = null;
	private static List<Location> roots = null;
	private static List<String> uniqueSortedLocationNames = null;
	
	public static void updateCache(){
		notifyChange();
		gid2location = getGid2location();
		roots = getHierarchy();
		uniqueSortedLocationNames = getUniqueSortedLocationNames();
	}
	
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

	static List<Location> getLineage(Location l) {
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
				set.addAll(getAsStringList(l.getAltNames()));
			}
			uniqueSortedLocationNames = new ArrayList<>();
			synchronized (uniqueSortedLocationNames) 
			{
				uniqueSortedLocationNames.addAll(set);
				Collections.sort(uniqueSortedLocationNames, 
						String.CASE_INSENSITIVE_ORDER);
			}
		}
		return uniqueSortedLocationNames;
	}

	public static List<Map<String, String>> listUniqueNames(String prefixNames, int limit) {
		List<Map<String, String>> result = new ArrayList<>();
		
		if(prefixNames == null || prefixNames.trim().isEmpty())
			return result;
		
		int numWord = 0;
		List<String> names = getUniqueSortedLocationNames();
		String prefixName = prefixNames.trim().toLowerCase();
		String delim = " +";
		String name;
		String[] tokens;
		String toBeRemoved;
		String usedName = "";
		List<String> remainingNames;
		List<String> tokenMatches = new ArrayList();
		boolean matches;
		Map<String, String> map;
		Pattern nonUnicodePattern = Pattern.compile("[^\\w]+", Pattern.UNICODE_CHARACTER_CLASS);
		String cleanedInputString = prefixName.replaceAll(nonUnicodePattern.toString(), "");
		String cleanedCompareString;
		
		while (!names.isEmpty()) {
			remainingNames = new ArrayList<>();
			
			for(String originalName : names) {
				name = originalName;
				tokens = name.split(delim);
				toBeRemoved = "";
				
				for(int i = 0; i < numWord; i++) {
					toBeRemoved += tokens[i] + delim;
				}
				
				try {
					usedName = name.replaceFirst("\\Q" + toBeRemoved + "\\E", "");
				}
				catch(Exception exception) {
					Logger.debug("Yes, the regex didn't work right!");
					Logger.debug(exception.toString());
				}
				
				for(int i = 0; i < tokens.length; i++) {
					cleanedCompareString = tokens[i].replaceAll(nonUnicodePattern.toString(), "");
					if(cleanedCompareString.equalsIgnoreCase(cleanedInputString)) {
						tokenMatches.add(originalName);
						break;
					}
				}
				
				if(usedName.toLowerCase().startsWith(prefixName)) {
					map = new HashMap<>();
					map.put("name", originalName);
					result.add(map);
					
					if(result.size() == limit) {
						remainingNames = Collections.emptyList();
						break;
					}
				}
				else if(tokens.length > numWord + 1) {
					remainingNames.add(originalName);
				}
			}
			
			numWord++;
			names = remainingNames;
		}
		
		numWord = result.size() < limit ? tokenMatches.size() : 0;
		for(int i = 0; i < numWord; i++) {
			map = new HashMap<>();
			map.put("name", tokenMatches.get(i));
			
			matches = false;
			for(int j = 0; j < result.size(); j++) {
				if(result.get(j).get("name").equals(tokenMatches.get(i))) {
					matches = true;
					break;
				}
			}
			
			if(!matches) {
				result.add(map);
				
				if(result.size() == limit) {
					break;
				}
			}
		}
		
		return result;
	}
	
	private static List<String> getAsStringList(
			List<AltName> altNames) {
		List<String> names = new ArrayList<>();
		if(altNames == null)
			return names;
		for(AltName n : altNames)
			names.add(n.getName());
		return names;
	}
}
