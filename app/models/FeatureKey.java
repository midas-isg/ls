package models;

public class FeatureKey {

	public final static String PROPERTIES = "properties";
	public final static String HEADLINE = "headline";
	public final static String RANK = "rank";
	public final static String NAME = "name";
	public final static String START_DATE = "startDate";
	public final static String END_DATE = "endDate";
	public final static String LOCATION_TYPE_NAME = "locationTypeName";
	public final static String PARENT_GID = "parentGid";
	public final static String LOCATION_TYPE_ID = "locationTypeId";
	public final static String MATCHED_TERM = "matchedTerm";
	public final static String GID = "gid";
	public final static String CODE = "code";
	public final static String CODE_TYPE_NAME = "codeTypeName";
	public final static String LOCATION_DESCRIPTION = "locationDescription";
	public final static String CHILDREN = "children";
	public final static String GEOMETRY = "geometry";
	public final static String BBOX = "bbox";
	public final static String REPPOINT = "repPoint";
	public final static String LINEAGE = "lineage";
	public final static String RELATED = "related";
	public final static String CODES = "codes";
	public final static String OTHER_NAMES = "otherNames";
	public final static String KML = "kml";
	public final static String SYNTHETIC_POPULATION = "syntheticPopulation";
	
	public static String asFullPath(String key){
		return PROPERTIES + "." + key;
	}

}
