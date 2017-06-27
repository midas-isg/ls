(function(HELPERS) {
	$(document).ready(function() {
		var countriesURL = CONTEXT + "/api/locations/find-by-type/1?_v=2&_onlyFeatureFields=properties.name%2Cproperties.gid&limit=0";
		
		$.get(countriesURL, function(data, status, xhr) {
			var countryInput = document.getElementById("country"),
				countryOption,
				tempArray = [],
				i;
			
			for(i = 0; i < data.features.length; i++) {
				tempArray.push({
					name: data.features[i].properties.name,
					gid: data.features[i].properties.gid
				});
			}
			
			tempArray.sort(HELPERS.nameCompare);
			
			countries = tempArray;
			
			for(i = 0; i < countries.length; i++) {
				countryOption = document.createElement("option");
				countryOption.value = countries[i].gid;
				countryOption.innerHTML = countries[i].name;
				countryInput.appendChild(countryOption);
			}
			
			return;
		});
		
		return;
	});
	
	return;
})(HELPERS);
