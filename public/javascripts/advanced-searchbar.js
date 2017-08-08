$(document).ready(function() {
	var locationTypesURL = CONTEXT + "/api/location-types",
		codeTypesURL = CONTEXT + "/api/code-types",
		disabled = false,
		jsonQuery = {
			ignoreAccent: document.getElementById("ignore-accent").checked,
			searchNames: document.getElementById("search-names").checked,
			searchOtherNames: document.getElementById("search-other-names").checked,
			searchCodes: document.getElementById("search-codes").checked,
			fuzzyMatch: document.getElementById("fuzzy-match").checked,
			fuzzyMatchThreshold: document.getElementById("fuzzy-match-threshold").value,
			limit: document.getElementById("limit").value,
			logic: document.getElementById("logic").value,
			startDate: document.getElementById("start-date").value,
			endDate: document.getElementById("end-date").value,
			rootALC: document.getElementById("root-alc").value,
			locationTypeIds: [],
			codeTypeIds: [],
			onlyFeatureFields: document.getElementById("only-feature-fields").value,
			excludedFeatureFields: document.getElementById("excluded-feature-fields").value
		};
	
	updateQueryDisplay();
	
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
		
		if(!HELPERS.findIsolatedNumberString(locationTypeSelector.value, locationTypesQuery)) {
			if(locationTypesQuery.length > 0) {
				locationTypesQuery += ", ";
			}
			
			locationTypesQuery += locationTypeSelector.value;
			locationTypesInput.value = locationTypesQuery;
			$(locationTypesInput).change();
		}
		
		return;
	});
	
	$("#code-type-adder").click(function() {
		var codeTypesInput = document.getElementById("code-type-ids"),
			codeTypesQuery = codeTypesInput.value,
			codeTypeSelector = document.getElementById("code-type-selector");
		
		if(!HELPERS.findIsolatedNumberString(codeTypeSelector.value, codeTypesQuery)) {
			if(codeTypesQuery.length > 0) {
				codeTypesQuery += ", ";
			}
			
			codeTypesQuery += codeTypeSelector.value;
			codeTypesInput.value = codeTypesQuery;
			$(codeTypesInput).change();
		}
		
		return;
	});
	
	$("#ignore-accent").change(function() {
		jsonQuery.ignoreAccent = document.getElementById("ignore-accent").checked;
		updateQueryDisplay();
		
		return;
	});
	
	$("#search-names").change(function() {
		jsonQuery.searchNames = document.getElementById("search-names").checked;
		updateQueryDisplay();
		
		return;
	});
	
	$("#search-other-names").change(function() {
		jsonQuery.searchOtherNames = document.getElementById("search-other-names").checked;
		updateQueryDisplay();
		
		return;
	});
	
	$("#search-codes").change(function() {
		jsonQuery.searchCodes = document.getElementById("search-codes").checked;
		updateQueryDisplay();
		
		return;
	});
	
	$("#fuzzy-match").change(function() {
		if(this.checked) {
			$("#fuzzy-match-option").show();
		}
		else {
			$("#fuzzy-match-option").hide();
		}
		
		jsonQuery.fuzzyMatch = document.getElementById("fuzzy-match").checked;
		updateQueryDisplay();
		
		return;
	});
	
	
	
	$("#fuzzy-match-threshold").change(function() {
		jsonQuery.fuzzyMatchThreshold = document.getElementById("fuzzy-match-threshold").value;
		updateQueryDisplay();
		
		return;
	});
	
	$("#limit").change(function() {
		jsonQuery.limit = document.getElementById("limit").value;
		updateQueryDisplay();
		
		return;
	});
	
	$("#logic").change(function() {
		jsonQuery.logic = document.getElementById("logic").value;
		updateQueryDisplay();
		
		return;
	});
	
	$("#start-date").change(function() {
		jsonQuery.startDate = document.getElementById("start-date").value;
		updateQueryDisplay();
		
		return;
	});
	
	$("#end-date").change(function() {
		jsonQuery.endDate = document.getElementById("end-date").value;
		updateQueryDisplay();
		
		return;
	});
	
	$("#root-alc").change(function() {
		jsonQuery.rootALC = document.getElementById("root-alc").value;
		updateQueryDisplay();
		
		return;
	});
	
	$("#location-type-ids").change(function() {
		jsonQuery.locationTypeIds = document.getElementById("location-type-ids").value.replace(/ /g, "").split(",");
		updateQueryDisplay();
		
		return;
	});
	
	$("#code-type-ids").change(function() {
		jsonQuery.codeTypeIds = document.getElementById("code-type-ids").value.replace(/ /g, "").split(",");
		updateQueryDisplay();
		
		return;
	});
	
	$("#only-feature-fields").change(function() {
		jsonQuery.onlyFeatureFields = document.getElementById("only-feature-fields").value;
		updateQueryDisplay();
		
		return;
	});
	
	$("#excluded-feature-fields").change(function() {
		jsonQuery.excludedFeatureFields = document.getElementById("excluded-feature-fields").value;
		updateQueryDisplay();
		
		return;
	});
	
	function updateQueryDisplay() {
		var queryDisplay = document.getElementById("json-query");
		
		queryDisplay.value = JSON.stringify(jsonQuery).replace(/,/g, ",\n\t").replace(/{/g, "{\n\t").replace(/}/g, "\n}\n");
		queryDisplay.rows = queryDisplay.value.match(/\n/g).length + 1;
		
		return;
	}
	
	$("#advanced-search-button").click(function() {
		localStorage.setItem("ignoreAccent", jsonQuery.ignoreAccent);
		localStorage.setItem("searchNames", jsonQuery.searchNames);
		localStorage.setItem("searchOtherNames", jsonQuery.searchOtherNames);
		localStorage.setItem("searchCodes", jsonQuery.searchCodes);
		localStorage.setItem("fuzzyMatch", jsonQuery.fuzzyMatch);
		
		localStorage.setItem("fuzzyMatchThreshold", jsonQuery.fuzzyMatchThreshold);
		localStorage.setItem("limit", jsonQuery.limit);
		localStorage.setItem("logic", jsonQuery.logic);
		localStorage.setItem("startDate", jsonQuery.startDate);
		localStorage.setItem("endDate", jsonQuery.endDate);
		//localStorage.setItem("latitude", document.getElementById("latitude").value);
		//localStorage.setItem("longitude", document.getElementById("longitude").value);
		
		localStorage.setItem("rootALC", jsonQuery.rootALC);
		localStorage.setItem("locationTypeIds", jsonQuery.locationTypeIds.toString());
		localStorage.setItem("codeTypeIds", jsonQuery.codeTypeIds.toString());
		localStorage.setItem("onlyFeatureFields", jsonQuery.onlyFeatureFields);
		localStorage.setItem("excludedFeatureFields", jsonQuery.excludedFeatureFields);
		
		if(document.getElementById("input").value === "") {
			alert("Please enter location name!");
			$("#input").focus();
			
			return;
		}
		
		return $("#search-button").click();
	});
	
	return;
});
