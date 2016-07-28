/*
search-results.js
*/

var SEARCH_RESULTS =
(function(){
	function SearchResults() {
		this.searchURL = CONTEXT + "/api/locations/find-by-term",
		this.pointURL = CONTEXT + "/api/locations-by-coordinate",
		this.browserURL = CONTEXT + "/browser",
		this.typeList = {},
		this.totalCount = 0;

		return;
	}

	SearchResults.prototype.searchByGeoJSON = function(geoJSON) {
		//POST /api/locations-by-geometry
		var httpType = "POST",
			URL = CONTEXT + "/api/locations-by-geometry?superTypeId=3",
			data = geoJSON,
			result = $("#result"),
			thisSearch = this;
		this.totalCount = 0;
		this.typeList = {};
		result.text("Please wait...");

		$.ajax({
			type: httpType,
			url: URL,
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
				SEARCH_RESULTS.updateOutput(thisSearch.typeList, thisSearch.totalCount);
				$("#result-count").append("<strong>user selection</strong>");

				return;
			}
		});

		return;
	}

	SearchResults.prototype.searchPoint = function(latitude, longitude) {
		var url = SEARCH_RESULTS.pointURL + "?lat=" + latitude + "&long=" + longitude,
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
				SEARCH_RESULTS.updateOutput(thisSearch.typeList, thisSearch.totalCount);
				$("#result-count").append("<strong>latitude: " + latitude + ", longitude: " + longitude + "</strong>");

				return;
			}
		});

		return;
	}

	SearchResults.prototype.updateOutput = function(locationList, totalCount) {
		var i,
			feature,
			featureType,
			appendString = "",
			result = $("#result"),
			features = [],
			points,
			target;

		if(totalCount > 0) {
			if(!document.getElementById("results-tbody")) {
				result.text("");
				appendString = "<table class='table table-condensed' style='margin-bottom: 0px;'>";
				appendString += "<caption id='result-count'><span id='result-total'><strong>" + totalCount + "</strong></span> result(s) from searching </caption>";
				appendString += "<thead>";
				appendString += "<th class='location-col'>Location</th>";
				appendString += "<th class='type-col'><select id='location-types'><option selected>[Type]</option></select></th>";
				appendString += "<th class='within-col'>Located within</th>";
				appendString += "</thead>";
				appendString += "<tbody id='results-tbody'></tbody>";
				appendString += "</table>";
				result.append(appendString);
			}

			for(featureType in locationList) {
				if(locationList.hasOwnProperty(featureType)) {
					$("#location-types").append("<option>" + featureType + "</option>");
					
					/*
					for(feature in locationList[featureType]) {
						if(locationList[featureType].hasOwnProperty(feature)) {
							locationList[featureType][feature].relevance = 0;
							
							for(i = 0; i < SEARCH_RESULTS.inputComponent.length; i++) {
								points = ((SEARCH_RESULTS.inputComponent.length - i) << 2);
								target = SEARCH_RESULTS.inputComponent[i].toLowerCase();
								
								locationList[featureType][feature].relevance += SEARCH_RANK.getScore(locationList[featureType][feature], target, points);
							}
							
							//console.log(locationList[featureType][feature]);
							//console.log(locationList[featureType][feature].properties.name + ": " + locationList[featureType][feature].relevance);
						}
					}
					*/
				}
			}
			
			function getFeatures(featureList) {
				var feature;

				for(feature in featureList) {
					if(featureList.hasOwnProperty(feature)) {
						features.push(featureList[feature]);
					}
				}
			}

			$("#location-types").change(function() {
				var type;
				features = [];

				if(this.value === "[Type]") {
					for(type in locationList) {
						if(locationList.hasOwnProperty(type)) {
							getFeatures(locationList[type]);
						}
					}
				}
				else {
					getFeatures(locationList[this.value]);
				}

				features.sort(function determineRelevance(currentFeature, oldFeature) {
					var currentScore = parseFloat(currentFeature.properties.rank),
						oldScore = parseFloat(oldFeature.properties.rank),
						currentName = currentFeature.properties.name.toLowerCase(),
						oldName = oldFeature.properties.name.toLowerCase();

					/*
					if(currentName < oldName) {
						currentScore++;
					}
					else {
						oldScore++;
					}
					*/
					
					return oldScore - currentScore;
				});

				SEARCH_RESULTS.updateTable(features);
			});

			$("#location-types").change();
		}

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
				aliasesString += ", " + properties.otherNames[j].name;
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

		return;
	}

	SearchResults.prototype.runQuery = function(queryInput) {
		var thisQuery = this;
		this.originalInput = queryInput,
		this.inputComponent = this.originalInput.replace(',', '').split(' ');

		function searchQuery() {
			var stillWaiting = thisQuery.inputComponent.length,
				i;

			$("#result").text("Please wait ...");
			SEARCH_MAP.featureLayer.clearLayers();

			function getQueryResults(input) {
				var url = SEARCH_RESULTS.searchURL,
					data = {
						"queryTerm": encodeURIComponent(input),
						"searchNames":true,
						"searchOtherNames":true,
						"searchCodes":true,
						"ignoreAccent":true,
						"limit":0
					};

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
						stillWaiting--;

						if(!stillWaiting) {
							SEARCH_RESULTS.updateOutput(thisQuery.typeList, thisQuery.totalCount);
							$("#result-count").append("<strong>" + $("#input").val() + "</strong>");
						}
					}
				});

				return;
			}

			if(thisQuery.originalInput !== thisQuery.inputComponent[0]) {
				stillWaiting++;
				getQueryResults(thisQuery.originalInput);
			}

			for(i = 0; i < thisQuery.inputComponent.length; i++) {
				getQueryResults(thisQuery.inputComponent[i]);
			}

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
