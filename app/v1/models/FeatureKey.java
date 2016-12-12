package v1.models;

public enum FeatureKey {
	PROPERTIES("properties"), HEADLINE("headline"), RANK("rank"), NAME("name"), START_DATE("startDate"), END_DATE(
			"endDate"), LOCATION_TYPE_NAME("locationTypeName"), LOCATION_TYPE_ID("locationTypeId"), PARENT_GID(
					"parentGid"), MATCHED_TERM("matchedTerm"), GID("gid"), CODE("code"), CODE_TYPE_NAME(
							"codeTypeName"), LOCATION_DESCRIPTION("locationDescription"), CHILDREN(
									"children"), GEOMETRY("geometry"), BBOX("bbox"), REPPOINT(
											"repPoint"), LINEAGE("lineage"), RELATED("related"), CODES(
													"codes"), OTHER_NAMES("otherNames"), KML(
															"kml"), SYNTHETIC_POPULATION("syntheticPopulation");

	private String value;

	FeatureKey(String value) {
		this.value = value;
	}

	public String valueOf() {
		return value;
	}

}
