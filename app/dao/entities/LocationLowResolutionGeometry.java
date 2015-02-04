package dao.entities;

import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name="location_optimized_geometry")
public class LocationLowResolutionGeometry extends LocationGeometry {

}
