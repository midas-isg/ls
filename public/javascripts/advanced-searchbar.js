$(document).ready(function() {
	var disabled = false;
	
	ADVANCED_OPTIONS_TOGGLE_EFFECTS = function() {
		disabled = !disabled;
		document.getElementById("search-button").disabled = disabled;
		
		return;
	};
	
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
