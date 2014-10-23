package dao;

import java.util.List;

import models.Borough;
import models.Neighborhood;


public interface NeighborhoodDAO {

	// http://workshops.opengeo.org/postgis-intro/simple_sql.html

	public List<Borough> boroughList();
	public List<Neighborhood> findByBorough(String borough);

	// http://workshops.opengeo.org/postgis-intro/simple_sql_exercises.html
	public List<Neighborhood> neighborhoodList();

}
