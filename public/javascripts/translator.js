"use strict";

$(document).ready(function() {
	var codeTypesURL = CONTEXT + "/api/code-types",
		countriesURL = CONTEXT + "/api/locations/find-by-type/1?_v=2&_onlyFeatureFields=properties.name%2Cproperties.gid&limit=0",
		gidURL = CONTEXT + "/api/locations/",
		findBulkURL = CONTEXT + "/api/locations/find-bulk?_v=2",
		codeTypes,
		locationTypes,
		countries;
	
	function nameCompare(a, b) {
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
	
	function processResult(properties, maxCodesLength) {
		var name = properties.name,
			codes = properties.codes,
			row = document.createElement("tr"),
			th = document.createElement("th"),
			td,
			i;
		
		th.innerHTML = name;
		th.headers = "results-names";
		row.appendChild(th);
		
		for(i = 0; i < maxCodesLength; i++) {
			td = document.createElement("td");
			
			if(codes[i]) {
				td.innerHTML = codes[i].codeTypeName + ": " + codes[i].code;
			}
			
			td.headers = "results-codes";
			row.appendChild(td);
		}
		
		$("#query-results tbody").append(row);
		
		return;
	}
	
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
	
	$("#location-codes-list").change(function() {
		$("#country").val(-1);
		
		return;
	});
	
	$("#country").change(function() {
		$("#location-codes-list").val("");
		$("#code-type").val(-1);
		
		return;
	});
	
	$("#submit-button").click(function() {
		var i,
			codeType = parseInt($("#code-type")[0].value),
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
			$.ajax({
				url: gidURL + rootALC + "?_v=2&_onlyFeatureFields=properties.codes%2Cproperties.name",
				type: "GET",
				beforeSend: function(xhr) {
					$("#query-results tbody").empty();
					$("#query-results tbody").append("<tr><th>PLEASE</th><th>WAIT...</th></tr>");
					
					return;
				},
				success: function(responseData, status, xhr) {
					console.info(responseData);
					
					$("#query-results tbody").empty();
					processResult(responseData.features[0].properties, responseData.features[0].properties.codes.length);
					
					return;
				},
				error: function(xhr,status,error) {
					console.error(xhr);
					console.error(status);
					console.error(error);
					
					return;
				},
				contentType: "application/json"
			});
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
						"properties.name"
					]
				});
			}
			
			$.ajax({
				url: findBulkURL,
				data: JSON.stringify(postData),
				type: "POST",
				beforeSend: function(xhr) {
					$("#query-results tbody").empty();
					$("#query-results tbody").append("<tr><th>PLEASE</th><th>WAIT...</th></tr>");
					
					return;
				},
				success: function(responseData, status, xhr) {
					var i,
						codesLength,
						maxCodesLength = 0;
					
					console.info(responseData);
					
					$("#query-results tbody").empty();
					
					for(i = 0; i < responseData.length; i++) {
						if(responseData[i].features[0]) {
							codesLength = responseData[i].features[0].properties.codes.length;
						}
						else {
							responseData[i].features.push({
								properties: {
									name: responseData[i].properties.queryTerm,
									codes: [{codeTypeName: "Error", code: "Not Found"}]
								}
							});
							
							codesLength = 1;
						}
						
						if(codesLength > maxCodesLength) {
							maxCodesLength = codesLength;
						}
					}
					
					for(i = 0; i < responseData.length; i++) {
						processResult(responseData[i].features[0].properties, maxCodesLength);
					}
				},
				error: function(xhr,status,error) {
					console.error(xhr);
					console.error(status);
					console.error(error);
					
					return;
				},
				contentType: "application/json"
			});
		}
		
		return;
	});
	
	return;
});
