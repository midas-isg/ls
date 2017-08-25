package interactors;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import dao.LocationDao;
import dao.entities.AltName;
import dao.entities.Location;
import gateways.configuration.ConfReader;
import models.FancyTreeNode;
import play.Logger;
import play.db.jpa.JPA;
import play.libs.Akka;
import scala.concurrent.duration.Duration;

public class LocationProxyRule {
	private static Map<Long, Location> gid2location = null;
	private static List<Location> roots = null;
	private static List<String> uniqueSortedLocationNames = null;
	private static List<FancyTreeNode> auTree = null;
	private static Long defaultCacheDelay = 0L;

	public static void initializeCache() {
		scheduleCacheUpdateOrInit(null);
	}

	public static void scheduleCacheUpdateOrInit(Location location) {
		if(isSkipUpdateCache(location))
			return;
		Akka.system().scheduler().scheduleOnce(Duration.create(defaultCacheDelay, TimeUnit.MILLISECONDS),
				new Runnable() {
					@Override
					public void run() {
						JPA.withTransaction(() -> {
							updateOrInitializeCache(location);
						});
					}
				}, Akka.system().dispatcher());
	}
	
	public static void initializeCacheIfConfigured() {
		ConfReader confReader = new ConfReader();
		String updateCache = confReader.readString("update.cache.on.start");
		if(updateCache == null || updateCache.trim().toLowerCase().equals("true")){
			LocationProxyRule.initializeCache();
		}
		else {
			Logger.info("skipped cache-initialization!");
		}
	}

	private static synchronized void updateOrInitializeCache(Location location) {

		if (location != null) {
			// DOES NOT UPDATE AUTREE
			// ONLY EPIDEMIC ZONES ARE ALLOWED TO BE CREATED AT THIS MOMENT
			updateUniqueSortedLocationNames(location);
			Logger.info("added names of location with gid = " + location.getGid()
					+ " to UniqueSortedLocationNames cache ...");
		} else {
			Logger.info("initializing cache for AUtree and UniqueSortedLocationNames ...");
			notifyChange();
			uniqueSortedLocationNames = getUniqueSortedLocationNames();
			roots = getHierarchy();
			auTree = getAuTree();
			Logger.info("cache updated!");
		}
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
				Logger.info("loading uniqueSortedLocationNames ...");
				uniqueSortedLocationNames = new LocationDao().readUniqueNames();
				Collections.sort(uniqueSortedLocationNames, String.CASE_INSENSITIVE_ORDER);
				Logger.info("uniqueSortedLocationNames updated!");
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
		String name;
		String[] tokens;
		String toBeRemoved;
		String usedName = "";
		List<String> remainingNames;
		List<String> tokenMatches = new ArrayList<>();
		boolean matches;
		Map<String, String> map;
		String cleanedInputString = removeNonWordCharacters(prefixName);
		String cleanedCompareString;

		while (!names.isEmpty()) {
			remainingNames = new ArrayList<>();

			for (String originalName : names) {
				name = originalName;
				tokens = name.split(delim);
				toBeRemoved = "";

				for (int i = 0; i < numWord; i++) {
					toBeRemoved += tokens[i] + delim;
				}

				try {
					usedName = name.replaceFirst("\\Q" + toBeRemoved + "\\E", "");
				} catch (Exception exception) {
					Logger.debug("Yes, the regex didn't work right!");
					Logger.debug(exception.toString());
				}

				for (int i = 0; i < tokens.length; i++) {
					cleanedCompareString = removeNonWordCharacters(tokens[i]);
					if (cleanedCompareString.equalsIgnoreCase(cleanedInputString)) {
						tokenMatches.add(originalName);
						break;
					}
				}

				if (usedName.toLowerCase().startsWith(prefixName)) {
					map = new HashMap<>();
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

		numWord = result.size() < limit ? tokenMatches.size() : 0;
		for (int i = 0; i < numWord; i++) {
			map = new HashMap<>();
			map.put("name", tokenMatches.get(i));

			matches = false;
			for (int j = 0; j < result.size(); j++) {
				if (result.get(j).get("name").equals(tokenMatches.get(i))) {
					matches = true;
					break;
				}
			}

			if (!matches) {
				result.add(map);

				if (result.size() == limit) {
					break;
				}
			}
		}

		return result;
	}

	public static String removeNonWordCharacters(String input) {
		Pattern nonWordPattern = Pattern.compile("[^\\p{L}0-9]+", Pattern.UNICODE_CHARACTER_CLASS);
		return input.replaceAll(nonWordPattern.toString(), "");
	}

	private static void updateUniqueSortedLocationNames(Location location) {
		List<String> names = getNames(location);
		if (uniqueSortedLocationNames == null)
			uniqueSortedLocationNames = getUniqueSortedLocationNames();
		else {
			synchronized (uniqueSortedLocationNames) {
				for (String name : names)
					insert(uniqueSortedLocationNames, name);
			}
		}
	}

	private static void insert(List<String> uniqueSortedNames, String name) {
		int insertPoint = Collections.binarySearch(uniqueSortedNames, name, String.CASE_INSENSITIVE_ORDER);
		if (insertPoint < 0)
			uniqueSortedNames.add(-insertPoint - 1, name);
	}

	private static List<String> getNames(Location location) {
		if (location == null)
			return null;

		List<String> names = new ArrayList<>();
		names.add(location.getData().getName());
		List<AltName> altNames = location.getAltNames();
		if (altNames != null)
			for (AltName altName : altNames)
				names.add(altName.getName());
		return names;
	}
	
	private static boolean isSkipUpdateCache(Location location){
		boolean isSkipUpdateCache = false;
		if(location != null){
			ConfReader confReader = new ConfReader();
			String confValue = confReader.readString("update.cache.on.crud");
			if(confValue == null || confValue.trim().toLowerCase().equals("true"))
				isSkipUpdateCache = false;
			else {
				Logger.info("skipped cache-update!");
				isSkipUpdateCache = true;
			}
		}
		return isSkipUpdateCache;
	}
}
