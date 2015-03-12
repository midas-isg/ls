package models.geo;

import java.util.List;

public class Polygon extends FeatureGeometry {
	/** list of lists because polygons can have 'holes' **/
	private List<List<double[]>> coordinates;
	
	public Polygon() {
		setType(Polygon.class.getSimpleName());
	}
	
	public List<List<double[]>> getCoordinates() {
		return coordinates;
	}
	
	public void setCoordinates(List<List<double[]>> coordinates) {
		this.coordinates = coordinates;
	}
	
	@Override
	public String toString() {
		String output = "Polygon; coordinates=\n";
		
		for(int i = 0; i < coordinates.size(); i++) {
			List<double[]> pointBody = coordinates.get(i);
			
			for(int j = 0; j < pointBody.size(); j++) {
				output += "[";
				for(int k = 0; k < pointBody.get(j).length; k++) {
					output += String.valueOf(pointBody.get(j)[k]);
					if(k < (pointBody.get(j).length - 1)) {
						output += ", ";
					}
				}
				output += "] ";
			}
			output += "\n";
		}
		
		return output;
	}
	
	public String getFirstTenAsString() {
		String output = "Polygon; coordinates=\n";
		
		List<double[]> pointBody = coordinates.get(0);
		
		for(int j = 0; j < 10; j++) {
			output += "[";
			for(int k = 0; k < pointBody.get(j).length; k++) {
				output += String.valueOf(pointBody.get(j)[k]);
				if(k < (pointBody.get(j).length - 1)) {
					output += ", ";
				}
			}
			output += "] ";
		}
		
		return output;
	}
	
	public String toPrettyString() {
		String output = "Polygon; coordinates=\n";
		
		output += "{\n";
		for(int i = 0; i < coordinates.size(); i++) {
			output += "   [\n";
			List<double[]> pointBody = coordinates.get(i);
			
			for(int j = 0; j < pointBody.size(); j++) {
				output += "\n      [\n";
				
				for(int k = 0; k < pointBody.get(j).length; k++){
					output += "         " + String.valueOf(pointBody.get(j)[k]) + "\n";
				}
				output += "      ]\n";
			}
			output += "   ]\n";
		}
		output += "}\n";
		
		return output;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((coordinates == null) ? 0 : coordinates.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Polygon other = (Polygon) obj;
		if (coordinates == null) {
			if (other.coordinates != null)
				return false;
		} else if (!coordinates.equals(other.coordinates))
			return false;
		return true;
	}
}
