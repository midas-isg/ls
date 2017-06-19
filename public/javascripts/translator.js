"use strict";

$(document).ready(function() {
	var codeTypesURL = CONTEXT + "/api/code-types",
		locationTypesURL = CONTEXT + "/api/location-types",
		countriesURL = CONTEXT + "/api/locations/find-by-type/1",
		locationsFilterURL = CONTEXT + "/api/locations/find-by-filter",
		findBulkURL = CONTEXT + "/api/locations/find-bulk?_v=2",
		codeTypes,
		locationTypes,
		countries,
		nameCompare;
	
	nameCompare = function(a, b) {
			var aName = a.name.toLowerCase(),
				bName = b.name.toLowerCase(),
				i,
				length = (aName.length - bName.length > 0) ? bName.length : aName.length;
			
			for(i = 0; i < length; i++) {
				if(aName.charAt(i) !== bName.charAt(i)) {
					return (aName.charCodeAt(i) - bName.charCodeAt(i));
				}
			}
	};
	
	$.get(codeTypesURL, function(data, status, xhr) {
		var codeTypesInput = document.getElementById("code-type"),
			codeTypeOption,
			i;
		
		data.sort(nameCompare);
		codeTypes = data;
		
		for(i = 0; i < codeTypes.length; i++) {
			codeTypeOption = document.createElement("option");
			codeTypeOption.value = codeTypes[i].id;
			codeTypeOption.innerHTML = codeTypes[i].name;
			codeTypesInput.appendChild(codeTypeOption);
		}
		
		return;
	});
	
	$.get(locationTypesURL, function(data, status, xhr) {
		var locationTypesInput = document.getElementById("location-types"),
			locationTypeOption,
			i;
		
		locationTypes = data;
		
		for(i = 0; i < locationTypes.length; i++) {
			locationTypeOption = document.createElement("option");
			locationTypeOption.value = locationTypes[i].id;
			locationTypeOption.innerHTML = locationTypes[i].name;
			locationTypesInput.appendChild(locationTypeOption);
		}
		
		return;
	});
	
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
		
		tempArray.sort(nameCompare);
		
		countries = tempArray;
		
		for(i = 0; i < countries.length; i++) {
			countryOption = document.createElement("option");
			countryOption.value = countries[i].gid;
			countryOption.innerHTML = countries[i].name;
			countryInput.appendChild(countryOption);
		}
		
		return;
	});
	
	function getLocationTypes() {
		var i,
			selectedLocationTypes = [];
		
		for(i = 0; i < $("#location-types")[0].selectedOptions.length; i++) {
			selectedLocationTypes.push(parseInt($("#location-types")[0].selectedOptions[i].value));
		}
		
		return selectedLocationTypes;
	}
	
	function getLocationCodes() {
		var i,
			locationCodes = [],
			inputArray = $("#location-codes-list")[0].value.split(","),
			location;
		
		for(i = 0; i < inputArray.length; i++) {
			location = inputArray[i].trim();
			
			if(location.length > 0) {
				locationCodes.push(location);
			}
		}
		
		return locationCodes;
	}
	
	$("#submit-button").click(function() {
		var i,
			postURL = findBulkURL,
			codeType = parseInt($("#code-type")[0].value),
			selectedLocationTypes = getLocationTypes(),
			locationCodes = getLocationCodes(),
			postData = [],
			rootALC = parseInt($("#country")[0].value);
		
		if(rootALC === -1) {
			rootALC = null;
		}
		
		if(codeType === -1) {
			codeType = null;
		}
		
		if((!codeType) && (!rootALC)) {
			return alert("Please enter either location codes and type or select a country");
		}
		
		if(locationCodes.length < 1) {
			postURL = locationsFilterURL;
			
			postData = {
				rootALC: rootALC,
				"onlyFeatureFields": [
					"properties.codes",
					"properties.gid"
				]
			};
		}
		else {
			for(i = 0; i < locationCodes.length; i++) {
				postData.push({
					"queryTerm": locationCodes[i],
					"ignoreAccent": true,
					"searchNames": false,
					"searchOtherNames": false,
					"searchCodes": true,
					"codeTypeIds": [parseInt(codeType)],
					"logic": "AND",
					"onlyFeatureFields": [
						"properties.codes",
						"properties.gid"
					]
				});
				
				if(rootALC) {
					postData[i].rootALC = rootALC;
				}
			}
		}
		
		$.ajax({
			url: postURL,
			data: JSON.stringify(postData),
			type: "POST",
			success: function(responseData, status, xhr) {
				console.log(responseData);
			},
			contentType: "application/json"
		});
		
		return;
	});
	
	return;
});
