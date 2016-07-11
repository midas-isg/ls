package interactors;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import dao.LocationDao;
import dao.entities.Location;
import models.FancyTreeNode;
import play.Logger;

public class LocationProxyRule {
	private static Map<Long, Location> gid2location = null;
	private static List<Location> roots = null;
	private static List<String> uniqueSortedLocationNames = null;
	private static List<FancyTreeNode> auTree = null;
	
	public static synchronized void updateCache(){
		notifyChange();
		long start = System.nanoTime();
		uniqueSortedLocationNames = getUniqueSortedLocationNames();
		long end = System.nanoTime();
		Logger.info("uniqueSortedLocationNames updated in " + (end-start)*1e-6 + " milliseconds");
		
		start = System.nanoTime();
		roots = getHierarchy();
		end = System.nanoTime();
		Logger.info("roots updated in " + (end-start)*1e-6 + " milliseconds");
		
		start = System.nanoTime();
		//auTree = getAuTree();
		end = System.nanoTime();
		Logger.info("auTree updated in " + (end-start)*1e-6 + " milliseconds");
		
		Logger.info("done! cache updated");
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
			roots = new ArrayList<>();
			synchronized (roots) {
				roots = new LocationDao().readRoots();
			}
		}
		return roots;
	}
	
	public static List<FancyTreeNode> getAuTree() {
		if (auTree == null)
			auTree = new ArrayList<>();
			synchronized (auTree){
				auTree = new FancyTreeRule().getAuTree();
			}
		return auTree;
	}

	public static List<Location> getLineage(long gid) {
		Location l = new LocationDao().read(gid);
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

	private static List<String> getUniqueSortedLocationNames(){
		if (uniqueSortedLocationNames == null){
			uniqueSortedLocationNames = new ArrayList<>();
			synchronized (uniqueSortedLocationNames) {
				uniqueSortedLocationNames = new LocationDao().readUniqueNames();
				Collections.sort(uniqueSortedLocationNames, 
						String.CASE_INSENSITIVE_ORDER);
			}
		}
		return uniqueSortedLocationNames;
	}
	
	public static List<Map<String, String>> listUniqueNames(
			String prefixNames, int limit){
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