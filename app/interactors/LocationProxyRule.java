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

import javax.persistence.EntityManager;
import javax.persistence.Tuple;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.ParameterExpression;
import javax.persistence.criteria.Root;

import play.Logger;
import play.db.jpa.JPA;
import dao.LocationDao;
import dao.entities.AltName;
import dao.entities.Location;

public class LocationProxyRule {
	private static Map<Long, Location> gid2location = null;
	private static List<Location> roots = null;
	private static List<String> uniqueSortedLocationNames = null;
	
	public static void updateCache(){
		notifyChange();
		//gid2location = getGid2location();
		uniqueSortedLocationNames = getUniqueSortedLocationNames();
		Logger.info("uniqueSortedLocationNames updated");
		roots = getHierarchy();
		Logger.info("roots updated");
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
			if (gid2location != null){
				synchronized (roots) 
				{
					for (Location l: gid2location.values()){
						if (l.getParent() == null)
							roots.add(l);
					}
				}
			}
			else {
				roots.addAll(readRootsFromDB());
			}
		}
		return roots;
	}

	public static Map<Long, Location> getGid2location() {
		if (gid2location == null){
			//gid2location = new LocationDao().getGid2location();
		}
		return gid2location;
	}
	
	static Location getLocation(long gid) {
		return getLocationFromCacheOrDB(gid);
	}

	private static Location getLocationFromCacheOrDB(long gid) {
		Map<Long, Location> cache = getGid2location();
		if (cache != null)
			return cache.get(gid);
		else {
			return readLocationFromDB(gid);
		}
	}

	public static List<Location> getLineage(long gid) {
		Location l = getLocationFromCacheOrDB(gid);
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
			Map<Long, Location> cache = getGid2location();
			Set<String> set = new HashSet<>();
			if(cache != null){		
				Collection<Location> locations = cache.values();
				for (Location l : locations){
					set.add(l.getData().getName());
					set.addAll(getAsStringList(l.getAltNames()));
				}
			}
			else {
				set.addAll(readUniqueNamesFromDB());
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

	private static Collection<? extends String> readUniqueNamesFromDB() {
		EntityManager em = JPA.em();
		CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();
		
		CriteriaQuery<String> q = criteriaBuilder.createQuery(String.class);
		Root<Location> root = q.from(Location.class);
		q.select(root.get("data").get("name")).distinct(true);
		List<String> names = em.createQuery(q).getResultList();
		
		q = criteriaBuilder.createQuery(String.class);
		Root<AltName> root2 = q.from(AltName.class);
		q.select(root2.get("name")).distinct(true);
		List<String> altames = em.createQuery(q).getResultList();
		
		Set<String> allNames = new HashSet<>();
		allNames.addAll(names);
		allNames.addAll(altames);
		
		return allNames;
	}
	
	private static Collection<? extends Location> readRootsFromDB() {
		EntityManager em = JPA.em();
		CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();
		CriteriaQuery<Location> q = criteriaBuilder.createQuery(Location.class);
		Root<Location> root = q.from(Location.class);
		q.select(root);
		//ParameterExpression<Location> p = criteriaBuilder.parameter(Location.class);
		q.where(criteriaBuilder.equal(root.get("data").get("locationType").get("id"), 1));
		List<Location> result = em.createQuery(q).getResultList();
		return result;
	}
	
	private static Location readLocationFromDB(long gid) {
		EntityManager em = JPA.em();
		Location l = em.find(Location.class, gid);
		return l;
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
