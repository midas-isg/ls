package dao;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import models.Borough;
import models.Neighborhood;
import dao.entities.NycNeighborhood;
import play.Logger;
import play.db.jpa.JPA;


public class NeighboorhoodPostGIS implements NeighborhoodDAO {

	@Override
	public List<Neighborhood> findByBorough(String borough) {
		
		List<Neighborhood> neighborhoods = new ArrayList<Neighborhood>();
		
		EntityManager em = JPA.em();
		Query q = em.createQuery ("FROM NycNeighborhood WHERE boroname = ?1");
		q.setParameter(1, borough);
		
		@SuppressWarnings("unchecked")
		List<NycNeighborhood> results = q.getResultList();
		
		for(NycNeighborhood row : results) {
			Neighborhood neighborhood = new Neighborhood();
			neighborhood.name = row.getName();
			neighborhood.boroughName = row.getBoroname();
			neighborhood.geom = row.getGeom();
			neighborhoods.add(neighborhood);
		}
		
		return neighborhoods;
	}

	@Override
	public List<Borough> boroughList() {
		
		List<Borough> boroughs = new ArrayList<Borough>();
		
		EntityManager em = JPA.em();
		Query q = em.createQuery ("SELECT DISTINCT n.boroname FROM NycNeighborhood n");

		@SuppressWarnings("unchecked")
		List<String> boroughNames = q.getResultList();	
		
		for(String name : boroughNames) {
			Borough borough = new Borough();
			borough.name = name;
			borough.population = 0;
			borough.neighborhoods = findByBorough(name);
			boroughs.add(borough);
		}

		return boroughs;
	}

	@Override
	public List<Neighborhood> neighborhoodList() {
		
		List<Neighborhood> neighborhoods = new ArrayList<Neighborhood>();
		
		EntityManager em = JPA.em();
		Query q = em.createQuery ("FROM NycNeighborhood");

		@SuppressWarnings("unchecked")
		List<NycNeighborhood> results = q.getResultList();	
		
		for(NycNeighborhood row : results) {
			Neighborhood neighborhood = new Neighborhood();
			neighborhood.name = row.getName();
			neighborhood.boroughName = row.getBoroname();
			neighborhood.geom = row.getGeom();
			neighborhoods.add(neighborhood);
		}
		
		return neighborhoods;
	}

}
