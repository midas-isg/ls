package dao;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import dao.entities.Location;
import dao.entities.LocationType;
import gateways.database.jpa.JpaAdaptor;
import play.db.jpa.JPA;

public class LocationTypeDao extends DataAccessObject<LocationType>{
	
	public LocationTypeDao(EntityManager em) {
		this(new JpaAdaptor(em));
	}

	private LocationTypeDao(JpaAdaptor jpaAdaptor) {
		super(LocationType.class, jpaAdaptor);
	}
	
	public static List<Location> findByType(long typeId, int limit, int offset){
		EntityManager em = JPA.em();
		Query query = em.createQuery("from Location where location_type_id = :typeId "
				+ " ORDER BY gid ");
		query.setParameter("typeId", typeId);
		query.setMaxResults(limit);
		query.setFirstResult(offset);
		@SuppressWarnings("unchecked")
		List<Location> result = query.getResultList();
		return result;
	}
	
	public LocationType findByName(String name) {
		EntityManager em = JPA.em();
		String q = "from LocationType where name='" + name + "'";
		Query query = em.createQuery(q);
		LocationType result = (LocationType)query.getSingleResult();
		return result;
	}

	public List<LocationType> findAllBySuperTypeId(Long superTypeId) {
		EntityManager em = JPA.em();
		String q = "from LocationType "
				+ (superTypeId == null ? "" : "where superType.id = '" + superTypeId + "' ") 
				+" order by name";
		Query query = em.createQuery(q);
		@SuppressWarnings("unchecked")
		List<LocationType> result = (List<LocationType>)query.getResultList();
		return result;
	}
	
	public List<String> getLocationTypeNames(
			List<Long> locationTypeIds) {
		if (locationTypeIds == null)
			return null;
		List<String> locationTypeNames = new ArrayList<>();
		LocationType locationType;
		for (Long id : locationTypeIds) {
			locationType = null;
			try {
				locationType = read(id);
			} catch (Exception e) {
				e.printStackTrace();
			}
			if(locationType != null)
				locationTypeNames.add(locationType.getName());
		}
		return locationTypeNames;
	}
	
	public String getLocationTypeName(LocationType locationType) {
		return (locationType == null) ? null : locationType.getName();
	}
	
}
