/*
search-results.js
*/

var SEARCH_RESULTS =
(function() {
	function SearchResults() {
		this.geometrySearchURL = CONTEXT + "/api/locations/find-by-geometry"; //?superTypeId=3";
		this.searchURL = CONTEXT + "/api/locations/find-by-term",
		this.pointURL = CONTEXT + "/api/locations/find-by-coordinate",
		this.browserURL = CONTEXT + "/browser",
		this.typeList = {},
		this.totalCount = 0;

		return;
	}

	SearchResults.prototype.searchByGeoJSON = function(geoJSON) {
		var geometrySearchURL = this.geometrySearchURL,
			httpType = "POST",
			data = geoJSON,
			result = $("#result"),
			thisSearch = this;
		
		this.totalCount = 0;
		this.typeList = {};
		result.text("Please wait...");

		$.ajax({
			type: httpType,
			url: geometrySearchURL,
			data: JSON.stringify(data),
			contentType: "application/json; charset=UTF-8",
			//dataType: "json",
			//processData: false,
			success: function(data, status, response) {
				console.log(data);
				console.log(status);
				console.log(response);

				if(data.features.length < 1) {
					$("#result").text("No results found");
				}
				else {
					thisSearch.processTypes(data);
				}

				return;
			},
			error: function(data, status) {
				console.warn(status);

				if(thisSearch.totalCount === 0) {
					$("#result").text("No results found");
				}

				return;
			},
			complete: function(xhr, status) {
				SEARCH_RESULTS.updateOutput(thisSearch.typeList);
				$("#searchInput").html("<strong>user selection</strong>");

				return;
			}
		});

		return;
	}

	SearchResults.prototype.searchPoint = function(latitude, longitude) {
		var url = SEARCH_RESULTS.pointURL + "?_v=2&_onlyFeatureFields=properties&lat=" + latitude + "&long=" + longitude,
			result = $("#result"),
			thisSearch = this;
		this.totalCount = 0;
		this.typeList = {};
		result.text("Please wait...");
		
		$.ajax({
			url: url,
			type: 'GET',
			success: function(data, status) {
				if(data.features.length < 1) {
					$("#result").text("No results found");
				}
				else {
					thisSearch.processTypes(data);
				}

				return;
			},
			error: function(data, status) {
				console.warn(status);

				if(thisSearch.totalCount === 0) {
					$("#result").text("No results found");
				}

				return;
			},
			complete: function(xhr, status) {
				SEARCH_RESULTS.updateOutput(thisSearch.typeList);
				$("#searchInput").html("<strong>latitude: " + latitude + ", longitude: " + longitude + "</strong>");

				return;
			}
		});

		return;
	}

	SearchResults.prototype.updateOutput = function(locationTypeList) {
		var featureType,
			appendString = "",
			result = $("#result"),
			features = [];

		if(!document.getElementById("results-tbody")) {
			result.text("");
			appendString = "<table class='table table-condensed' style='margin-bottom: 0px;'>";
			appendString += "<caption id='result-count'><span id='result-total'><strong>" + this.totalCount + "</strong></span> result(s) from searching <span id='searchInput'></span></caption>";
			appendString += "<thead>";
			appendString += "<th class='location-col'>Location</th>";
			appendString += "<th class='type-col'><select id='location-types'></select></th>";
			appendString += "<th class='within-col'>Located within</th>";
			appendString += "</thead>";
			appendString += "<tbody id='results-tbody'></tbody>";
			appendString += "</table>";
			result.append(appendString);

			function getFeatures(featureList) {
				var feature;

				for(feature in featureList) {
					if(featureList.hasOwnProperty(feature)) {
						features.push(featureList[feature]);
					}
				}

				return;
			}

			$("#location-types").change(function() {
				var type;
				features = [];

				if(this.value === "[Type]") {
					for(type in locationTypeList) {
						if(locationTypeList.hasOwnProperty(type)) {
							getFeatures(locationTypeList[type]);
						}
					}
				}
				else {
					getFeatures(locationTypeList[this.value]);
				}
				
				SEARCH_RESULTS.updateTable(features);
			});
		}

		(function resetTypes() {
			$("#location-types").empty();
			$("#location-types").append("<option selected>[Type]</option>");
			for(featureType in locationTypeList) {
				if(locationTypeList.hasOwnProperty(featureType)) {
					$("#location-types").append("<option>" + featureType + "</option>");
				}
			}

			$("#location-types").change();
		})();

		return;
	}

	SearchResults.prototype.updateTable = function(features) {
		var appendString = "",
			aliasesString = "",
			properties,
			gid,
			url,
			to,
			i,
			j,
			length,
			root,
			resultBody = $("#results-tbody");

		resultBody.empty();

		for(i = 0, length = features.length; i < length; i++) {
			properties = features[i].properties;
			gid = properties.gid;
			url = SEARCH_RESULTS.browserURL + "?id=" + gid;
			to = "";

			if(properties.endDate) {
				to = " to " + properties.endDate;
			}

			aliasesString = "Aliases: " + properties.name;
			for(j = 0; j < properties.otherNames.length; j++) {
				aliasesString += ", " + properties.otherNames[j].name.replace(/'/g, "&#x27;").replace(/"/g, "&#39;");
			}

			appendString = "<tr><td class='location-col'><a href='"+ url +"' title='" + aliasesString + "'>"+ properties.headline  + "</a> from " + properties.startDate + to + "</td>";
			appendString += "<td class='type-col'>" + properties.locationTypeName + "</td>";

			root = properties.lineage[0];
			if(root) {
				appendString = appendString + "<td class='within-col' id='result_lineage" + i + "'></td></tr>";
				resultBody.append(appendString);

				HELPERS.listLineageRefs(properties.lineage, "#result_lineage" + i);
			}
			else {
				appendString = appendString + "<td class='within-col'></td></tr>";
				resultBody.append(appendString);
			}
		}

		return;
	}

	SearchResults.prototype.processTypes = function(data) {
		var properties,
			features = data.features,
			i;

		for(i = 0, length = features.length; i < length; i++) {
			properties = features[i].properties;

			if(!this.typeList[properties.locationTypeName]) {
				this.typeList[properties.locationTypeName] = {};
			}

			if(!this.typeList[properties.locationTypeName][properties.gid]) {
				this.typeList[properties.locationTypeName][properties.gid] = features[i];
				this.totalCount++;
			}
		}

		$("#result-total").html("<strong>" + this.totalCount + "</strong>");

		return;
	}

	SearchResults.prototype.runQuery = function(queryInput) {
		var thisQuery = this;
		
		this.originalInput = queryInput;
		this.inputComponent = this.originalInput.replace(',', '').split(' ');
		
		function searchQuery() {
			var i,
				j,
				latitude,
				longitude,
				geoJSON,
				coordinateArray,
				tempArray;

			$("#result").text("Please wait ...");
			SEARCH_MAP.featureLayer.clearLayers();
			
			function getQueryResults(input) {
				var parameter,
					url = SEARCH_RESULTS.searchURL + "?_v=2",
					data = {
						"queryTerm": input,
						"excludedFeatureFields": ["geometry", "type", "properties.children", "properties.codes", "properties.matchedTerm"],
						"limit": 0
					};
				
				for(parameter in localStorage) {
					if(localStorage.hasOwnProperty(parameter)) {
						if(localStorage[parameter] !== "") {
							switch(parameter) {
								case "ignoreAccent":
								case "searchNames":
								case "searchOtherNames":
								case "searchCodes":
								case "verbose":
								case "fuzzyMatch":
									data[parameter] = (localStorage[parameter] === "true");
								break;
								
								//case "offset":
								case "limit":
								case "rootALC":
								case "fuzzyMatchThreshold":
									data[parameter] = parseInt(localStorage[parameter]);
								break;
								
								case "latitude":
								case "longitude":
									data[parameter] = parseFloat(localStorage[parameter]);
								break;
								
								case "locationTypeIds":
								case "codeTypeIds":
									data[parameter] = [];
									tempArray = localStorage[parameter].split(",");
									
									for(j = 0; j < tempArray.length; j++) {
										data[parameter][j] = parseInt(tempArray[j].trim());
									}
								break;
								case "onlyFeatureFields":
								case "excludedFeatureFields":
									data[parameter] = [];
									tempArray = localStorage[parameter].split(",");
									
									for(j = 0; j < tempArray.length; j++) {
										data[parameter][j] = tempArray[j].trim();
									}
								break;
								
								default:
									data[parameter] = localStorage[parameter];
								break;
							}
						}
						
						localStorage.removeItem(parameter);
					}
				}
				
				
				$.ajax({
					url: url,
					type: 'POST',
					contentType: 'application/json',
					data: JSON.stringify(data),
					success: function(data, status) {
						if(data.features.length < 1) {
							$("#result").text("No results found");
						}
						else{
							SEARCH_RESULTS.processTypes(data);
						}

						return;
					},
					error: function(data, status) {
						console.warn(status);

						if(thisQuery.totalCount === 0) {
							$("#result").text("No results found");
						}

						return;
					},
					complete: function(xhr, status) {
						SEARCH_RESULTS.updateOutput(thisQuery.typeList);
						$("#searchInput").html("<strong>" + $("#input").val() + "</strong>");
					}
				});

				return;
			}

			if(thisQuery.originalInput.charAt(0) === "@") {
				thisQuery.inputComponent = thisQuery.originalInput.substring(1).split(',');
				latitude = thisQuery.inputComponent[0];
				longitude = thisQuery.inputComponent[1];
				
				return SEARCH_RESULTS.searchPoint(latitude, longitude);
			}
			
			if(thisQuery.originalInput.charAt(0) === "[" &&
				thisQuery.originalInput.charAt(1) === "(") {
				coordinateArray = [];
				thisQuery.inputComponent = thisQuery.originalInput.substring(1, thisQuery.originalInput.length - 1)
					.replace(/[\(\)]/g, "").split(",");
				
				for(i = 0; i < thisQuery.inputComponent.length; i += 2) {
					coordinateArray.push([parseFloat(thisQuery.inputComponent[i]), parseFloat(thisQuery.inputComponent[i + 1])]);
				}
				
				geoJSON = {
					type: "FeatureCollection",
					features: [
						{
							type: "Feature",
							geometry: {
								type: "Polygon",
								coordinates: [coordinateArray]
							}
						}
					]
				};
				
				return SEARCH_RESULTS.searchByGeoJSON(geoJSON);
			}
			
			getQueryResults(thisQuery.originalInput);
			
			
			return;
		}

		return searchQuery();
	}

	return new SearchResults();
})();

$(document).ready(function() {
	var query = HELPERS.getURLParameterByName("q");

	if(query) {
		$("#input").val(query);
		SEARCH_RESULTS.runQuery(query);
	}

	return;
});
