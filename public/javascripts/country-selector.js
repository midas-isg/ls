"use strict";

(function(HELPERS) {
	$(document).ready(function() {
		var countriesURL = CONTEXT + "/api/locations/find-by-type/1?_v=2&_onlyFeatureFields=properties.name%2Cproperties.gid&limit=0",
			countries = [];
		
		$.get(countriesURL, function(data, status, xhr) {
			var countryInput = document.getElementById("country"),
				countryOption,
				i;
			
			for(i = 0; i < data.features.length; i++) {
				countries.push({
					name: data.features[i].properties.name,
					gid: data.features[i].properties.gid
				});
			}
			
			countries.sort(HELPERS.nameCompare);
			
			for(i = 0; i < countries.length; i++) {
				countryOption = document.createElement("option");
				countryOption.value = countries[i].gid;
				countryOption.innerHTML = countries[i].name;
				countryInput.appendChild(countryOption);
			}
			
			return;
		});
		
		$("#get-all-countries").click(function() {
			var countriesIDs = countries[0].gid,
				i;
			
			for(i = 1; i < countries.length; i++) {
				countriesIDs += ", " + countries[i].gid;
			}
			
			$("#location-codes-list").text(countriesIDs);
			$("#code-type").val(14);
			alert("This query may take a few minutes! Click Submit when ready.");
			
			return;
		});
		
		return;
	});
	
	return;
})(HELPERS);
