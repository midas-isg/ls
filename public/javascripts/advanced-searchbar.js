$(document).ready(function() {
	var locationTypesURL = CONTEXT + "/api/location-types",
		codeTypesURL = CONTEXT + "/api/code-types",
		disabled = false;
	
	$.ajax( {
		url: locationTypesURL,
		type: "GET",
		success: function(result, status, xhr) {
			var i,
				selector = document.getElementById("location-type-selector"),
				option;
			
//console.info(result);
			
			for(i = 0; i < result.length; i++) {
				option = document.createElement("option");
				option.innerHTML = result[i].name + " (" + result[i].id +")";
				option.value = result[i].id;
				selector.appendChild(option);
			}
			
			return;
		}
	});
	
	$.ajax( {
		url: codeTypesURL,
		type: "GET",
		success: function(result, status, xhr) {
			var i,
				selector = document.getElementById("code-type-selector"),
				option;
			
//console.info(result);
			
			for(i = 0; i < result.length; i++) {
				option = document.createElement("option");
				option.innerHTML = result[i].name + " (" + result[i].id +")";
				option.value = result[i].id;
				selector.appendChild(option);
			}
			
			return;
		}
	});
	
	ADVANCED_OPTIONS_TOGGLE_EFFECTS = function() {
		disabled = !disabled;
		document.getElementById("search-button").disabled = disabled;
		
		return;
	};
	
	$("#location-type-adder").click(function() {
		var locationTypesInput = document.getElementById("location-type-ids"),
			locationTypesQuery = locationTypesInput.value,
			locationTypeSelector = document.getElementById("location-type-selector");
		
		if((locationTypesQuery.search(locationTypeSelector.value) !== 0) && (locationTypesQuery.search(" " + locationTypeSelector.value) === -1)) {
			if(locationTypesQuery.length > 0) {
				locationTypesQuery += ", ";
			}
			
			locationTypesQuery += locationTypeSelector.value;
			locationTypesInput.value = locationTypesQuery;
		}
		
		return;
	});
	
	$("#code-type-adder").click(function() {
		var codeTypesInput = document.getElementById("code-type-ids"),
			codeTypesQuery = codeTypesInput.value,
			codeTypeSelector = document.getElementById("code-type-selector");
		
		if((codeTypesQuery.search(codeTypeSelector.value) !== 0) && (codeTypesQuery.search(" " + codeTypeSelector.value) === -1)) {
			if(codeTypesQuery.length > 0) {
				codeTypesQuery += ", ";
			}
			
			codeTypesQuery += codeTypeSelector.value;
			codeTypesInput.value = codeTypesQuery;
		}
		
		return;
	});
	
	$("#fuzzy-match").change(function() {
		if(this.checked) {
			$("#fuzzy-match-option").show();
		}
		else {
			$("#fuzzy-match-option").hide();
		}
		
		return;
	});
	
	$("#advanced-search-button").click(function() {
		localStorage.setItem("ignoreAccent", document.getElementById("ignore-accent").checked);
		localStorage.setItem("searchNames", document.getElementById("search-names").checked);
		localStorage.setItem("searchOtherNames", document.getElementById("search-other-names").checked);
		localStorage.setItem("searchCodes", document.getElementById("search-codes").checked);
		localStorage.setItem("fuzzyMatch", document.getElementById("fuzzy-match").checked);
		
		localStorage.setItem("fuzzyMatchThreshold", document.getElementById("fuzzy-match-threshold").value);
		localStorage.setItem("limit", document.getElementById("limit").value);
		localStorage.setItem("logic", document.getElementById("logic").value);
		localStorage.setItem("startDate", document.getElementById("start-date").value);
		localStorage.setItem("endDate", document.getElementById("end-date").value);
		//localStorage.setItem("latitude", document.getElementById("latitude").value);
		//localStorage.setItem("longitude", document.getElementById("longitude").value);
		
		localStorage.setItem("rootALC", document.getElementById("root-alc").value);
		
		localStorage.setItem("locationTypeIds", document.getElementById("location-type-ids").value);
		localStorage.setItem("codeTypeIds", document.getElementById("code-type-ids").value);
		localStorage.setItem("onlyFeatureFields", document.getElementById("only-feature-fields").value);
		localStorage.setItem("excludedFeatureFields", document.getElementById("excluded-feature-fields").value);
		
		if(document.getElementById("input").value === "") {
			alert("Please enter location name!");
			$("#input").focus();
			
			return;
		}
		
		return $("#search-button").click();
	});
	
	return;
});
