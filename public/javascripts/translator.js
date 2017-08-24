"use strict";

(function(HELPERS) {
	$(document).ready(function() {
		var apolloLocationCodeTypeID = 14,
			gidURL = CONTEXT + "/api/locations/",
			findBulkURL = CONTEXT + "/api/locations/find-bulk?_v=2",
			codeTypes,
			locationTypes,
			countries;
		
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
		
		function postLocationQuery(codeType, locationCodes) {
			var i,
				postData = [];
			
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
						j,
						codes,
						codesLength,
						maxCodesLength = 0,
						codeTypes = [],
						tempObject = {},
						temp,
						th;
					
					//console.info(responseData);
					
					$("#query-results tbody").empty();
					
					for(i = 0; i < responseData.length; i++) {
						if(responseData[i].features[0]) {
							codes = responseData[i].features[0].properties.codes;
							
							codesLength = codes.length;
							
							for(j = 0; j < codes.length; j++) {
								tempObject[codes[j].codeTypeName] = j;
							}
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
					
					for(i in tempObject) {
						if(tempObject.hasOwnProperty(i)) {
							codeTypes.push(i);
						}
					}
					
					temp = $("table#query-results thead tr")[0];
					th = $(temp).find("#results-names")[0];
					$(temp).empty();
					temp.appendChild(th);
					
					for(i = 0; i < codeTypes.length; i++) {
						th = document.createElement("th");
						th.innerHTML = codeTypes[i];
						temp.appendChild(th);
					}
					
					for(i = 0; i < responseData.length; i++) {
						displayResultRow(responseData[i].features[0].properties, maxCodesLength, codeTypes);
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
			
			return;
		}
		
		function displayResultRow(properties, maxCodesLength, codeTypes) {
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
					while(codes[i].codeTypeName !== codeTypes[row.children.length - 1]) {
						row.appendChild(td);
						td = document.createElement("td");
					}
					
					td.innerHTML = codes[i].code;
				}
				
				td.headers = "results-codes";
				row.appendChild(td);
			}
			
			while((row.children.length - 1) < codeTypes.length) {
				td = document.createElement("td");
				row.appendChild(td);
			}
			
			$("#query-results tbody").append(row);
			
			return;
		}
		
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
			var codeType = parseInt($("#code-type")[0].value),
				locationCodes = getLocationCodes(),
				rootALC = parseInt($("#country")[0].value);
			
			if(codeType === -1) {
				codeType = null;
			}
			
			if(rootALC === -1) {
				rootALC = null;
			}
			
			if((!codeType) && (!rootALC)) {
				return alert("Please enter either location codes and type or select a country");
			}
			
			if(locationCodes.length < 1) {
				codeType = apolloLocationCodeTypeID;
				locationCodes = [rootALC];
				
				$.ajax({
					url: gidURL + rootALC + "?_v=2&_onlyFeatureFields=properties.children",
					type: "GET",
					beforeSend: function(xhr) {
						$("#query-results tbody").empty();
						$("#query-results tbody").append("<tr><th>PLEASE</th><th>WAIT...</th></tr>");
						
						return;
					},
					success: function(responseData, status, xhr) {
						var i,
							children = responseData.features[0].properties.children;
						
						//console.info(responseData);
						
						for(i = 0; i < children.length; i++) {
							locationCodes.push(parseInt(children[i].gid));
						}
						
						postLocationQuery(codeType, locationCodes);
						
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
				postLocationQuery(codeType, locationCodes);
			}
			
			return;
		});
		
		return;
	});
	
	return;
})(HELPERS);
