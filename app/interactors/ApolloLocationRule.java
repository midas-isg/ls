package interactors;

import java.util.List;

import models.apollo.Apollo;
import models.apollo.ApolloLocation;
import models.apollo.ApolloNamedMultiGeometry;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;

import dao.entities.Data;
import dao.entities.Location;
import edu.pitt.apollo.types.v3_0_0.LocationDefinition;
import edu.pitt.apollo.types.v3_0_0.LocationPolygon;
import edu.pitt.apollo.types.v3_0_0.MultiGeometry;
import edu.pitt.apollo.types.v3_0_0.NamedMultiGeometry;

public class ApolloLocationRule {
	public static Apollo toApolloLocation(Location location) {
		Long auTypeId = location.getData().getLocationType().getId();
		
		if (auTypeId.longValue() == LocationRule.EPIDEMIC_ZONE_ID)
			return toEpidemicZones(location);

		ApolloLocation al = new ApolloLocation();
		al.setTextualDescription(toText(location));
		al.setApolloLocationCode("" + location.getGid());
		
		
		if (location.getGeometry().getMultiPolygonGeom() != null){
			NamedMultiGeometry nmg = toNamedMultiGeometry(location);
			al.setNamedMultiGeometry(nmg);
		} else if (! location.getLocationsIncluded().isEmpty()) {
			LocationDefinition ld = toLocationDefinition(location);
			al.setLocationDefinition(ld);
		}
		return al;
	}

	private static ApolloNamedMultiGeometry toEpidemicZones(Location location){
		ApolloNamedMultiGeometry nmg = new ApolloNamedMultiGeometry();
		populateNamedMuttiGeometry(location, nmg);
		return nmg;
	}
		
	private static String toText(Location location) {
		Data data = location.getData();
		String text = data.getName() 
				+ " [" + data.getLocationType().getName() + "]"; 
		String description = data.getDescription();
		if (description != null && !description.isEmpty())
			text += ": " + description;
		return text;
	}
	
	private static NamedMultiGeometry toNamedMultiGeometry(Location location) {
		NamedMultiGeometry nmg = new NamedMultiGeometry();
		populateNamedMuttiGeometry(location, nmg);
		return nmg;
	}

	private static void populateNamedMuttiGeometry(Location location,
			NamedMultiGeometry nmg) {
		nmg.setApolloLocationCode("" + location.getGid());
		nmg.setTextualDescription(toText(location));
		List<LocationPolygon> polygons = nmg.getPolygons();
		populatePoygons(polygons, location);
	}

	private static LocationDefinition toLocationDefinition(Location location){
		LocationDefinition ld = new LocationDefinition();
		List<MultiGeometry> geos = ld.getMultiGeometries();
		List<Location> locations = location.getLocationsIncluded();
		if (locations != null && ! locations.isEmpty()){
			List<String> locationsIncluded = ld.getLocationsIncluded();
			for (Location l : locations){
				locationsIncluded.add("" + l.getGid());
				MultiGeometry mg = toMutiGeometry(l);
				geos.add(mg);
			}
		} else {
			geos.add(toMutiGeometry(location));
		}
		return ld;
	}

	private static MultiGeometry toMutiGeometry(Location l) {
		MultiGeometry mg = new MultiGeometry();
		List<LocationPolygon> polygons = mg.getPolygons();
		populatePoygons(polygons, l);
		return mg;
	}

	private static void populatePoygons(List<LocationPolygon> polygons,
			Location l) {
		Geometry mpg = l.getGeometry().getMultiPolygonGeom();
		if (mpg == null){
			return;
		}
		int n = mpg.getNumGeometries();
		for (int i = 0; i < n; i++){
			Geometry p = mpg.getGeometryN(i);
			LocationPolygon polygon = toPolygon(p);
			polygons.add(polygon);
		}
	}

	private static LocationPolygon toPolygon(Geometry p) {
		LocationPolygon polygon = new LocationPolygon();
		String linearRing = toLinearRing(p); 
		polygon.setLinearRing(linearRing);
		return polygon;
	}
	
	private static String toLinearRing(Geometry polygon) {
		String linearRing = "";
		if (polygon == null)
			return linearRing;
		String space = "";
		Coordinate[] coordinates = polygon.getCoordinates();
		for (Coordinate c : coordinates){
			linearRing += space + c.x + "," + c.y + ",0";
			space = " ";
		}
		return linearRing;
	}
}
