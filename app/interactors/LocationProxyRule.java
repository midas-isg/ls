package interactors;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import dao.LocationDao;
import dao.entities.AltName;
import dao.entities.Location;
import models.FancyTreeNode;
import play.Logger;
import play.db.jpa.JPA;
import play.libs.Akka;
import scala.concurrent.duration.Duration;
import akka.util.*;
import akka.actor.ActorSystem;

public class LocationProxyRule {
	private static Map<Long, Location> gid2location = null;
	private static List<Location> roots = null;
	private static List<String> uniqueSortedLocationNames = null;
	private static List<FancyTreeNode> auTree = null;

	public static void scheduleCacheUpdate() {
		scheduleCacheUpdate(null);
	}
	public static void scheduleCacheUpdate(Location location) {

		Akka.system().scheduler().scheduleOnce(Duration.create(0, TimeUnit.MILLISECONDS), new Runnable() {
			@Override
			public void run() {
				Logger.info("building cache ...");
				JPA.withTransaction(() -> {
					LocationProxyRule.updateCache(location);
				});
			}
		}, Akka.system().dispatcher());
	}

	private static synchronized void updateCache(Location location) {

		if (location != null) {
			updateUniqueSortedLocationNames(location);
			return;
		}

		notifyChange();
		// long start = System.nanoTime();
		uniqueSortedLocationNames = getUniqueSortedLocationNames();
		// long end = System.nanoTime();
		// Logger.info("uniqueSortedLocationNames updated in " +
		// (end-start)*1e-6 + " milliseconds");
		roots = getHierarchy();
		auTree = getAuTree();
		Logger.info("cache built finished.");
	}

	private static void notifyChange() {
		if (gid2location != null) {
			synchronized (gid2location) {
				gid2location = null;
			}
		}

		if (roots != null) {
			synchronized (roots) {
				roots = null;
			}
		}

		if (uniqueSortedLocationNames != null) {
			synchronized (uniqueSortedLocationNames) {
				uniqueSortedLocationNames = null;
			}
		}
	}

	public static List<Location> getHierarchy() {
		if (roots == null) {
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
		synchronized (auTree) {
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
		while (parent != null) {
			lineage.addFirst(parent);
			parent = parent.getParent();
		}
		return lineage;
	}

	private static List<String> getUniqueSortedLocationNames() {
		if (uniqueSortedLocationNames == null) {
			uniqueSortedLocationNames = new ArrayList<>();
			synchronized (uniqueSortedLocationNames) {
				uniqueSortedLocationNames = new LocationDao().readUniqueNames();
				Collections.sort(uniqueSortedLocationNames, String.CASE_INSENSITIVE_ORDER);
			}
		}
		return uniqueSortedLocationNames;
	}

	public static List<Map<String, String>> listUniqueNames(String prefixNames, int limit) {
		List<Map<String, String>> result = new ArrayList<>();
		if (prefixNames == null || prefixNames.trim().isEmpty())
			return result;

		int numWord = 0;

		List<String> names = getUniqueSortedLocationNames();

		String prefixName = prefixNames.trim().toLowerCase();

		String delim = " +";
		while (!names.isEmpty()) {
			List<String> remainingNames = new ArrayList<>();
			for (String originalName : names) {
				String name = originalName.replaceAll("[()]", "");
				String[] tokens = name.split(delim);
				String toBeRemoved = "";
				for (int i = 0; i < numWord; i++) {
					toBeRemoved += tokens[i] + delim;
				}
				String usedName = name.replaceFirst(toBeRemoved, "");

				if (usedName.toLowerCase().startsWith(prefixName)) {
					Map<String, String> map = new HashMap<>();
					map.put("name", originalName);
					result.add(map);
					if (result.size() == limit) {
						remainingNames = Collections.emptyList();
						break;
					}
				} else if (tokens.length > numWord + 1) {
					remainingNames.add(originalName);
				}
			}
			numWord++;
			names = remainingNames;
		}
		return result;
	}

	private static void updateUniqueSortedLocationNames(Location location) {
		if (uniqueSortedLocationNames == null)
			uniqueSortedLocationNames = new ArrayList<>();
		List<String> names = getNames(location);
		insertIntoUniqueSortedLocationNames(names);
	}

	private static void insertIntoUniqueSortedLocationNames(List<String> names) {
		if (uniqueSortedLocationNames == null || names == null)
			return;
		int insertPoint;
		for (String name : names) {
			insertPoint = Collections.binarySearch(uniqueSortedLocationNames, name, String.CASE_INSENSITIVE_ORDER);
			if (insertPoint < 0){
				uniqueSortedLocationNames.add(-insertPoint - 1, name);
				//Logger.debug(name + " inserted into uniqueNames at position: " + (-insertPoint - 1));
			}
		}
	}

	private static List<String> getNames(Location location) {
		if (location == null)
			return null;
		List<String> names = new ArrayList<>();
		names.add(location.getData().getName());
		for (AltName altName : location.getAltNames())
			names.add(altName.getName());
		return names;
	}
}