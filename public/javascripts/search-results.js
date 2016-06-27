/*
search-results.js
*/

var SEARCH_RESULTS = new searchResults();

function searchResults() {
	this.searchURL = context + "/api/locations/find-by-term",
	this.pointURL = context + "/api/locations-by-coordinate",
	this.browserURL = context + "/browser",
	this.typeList = {},
	this.totalCount = 0;

	return;
}

$(document).ready(function() {
	var query = getURLParameterByName("q");
	
	if(query) {
		$("#input").val(query);
		SEARCH_RESULTS.runQuery();
	}
	
	return;
});



searchResults.prototype.searchByGeoJSON = function(geoJSON) {
	//POST /api/locations-by-geometry
	var httpType = "POST",
		URL = context + "/api/locations-by-geometry?superTypeId=3",
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

			thisSearch.processResults(data);

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

searchResults.prototype.searchPoint = function(latitude, longitude) {
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
			thisSearch.processResults(data.geoJSON);

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

searchResults.prototype.updateOutput = function(locationList, totalCount) {
	var i,
		appendString = "",
		result = $("#result"),
		features;

	if(totalCount > 0) {
		if(!document.getElementById("results-tbody")) {
			result.text("");
			appendString = "<table class='table table-condensed pre-spaced' style='margin-bottom: 0px;'>";
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

		for(i in locationList) {
			if(locationList.hasOwnProperty(i)) {
				$("#location-types").append("<option>" + i + "</option>");
			}
		}

		function getFeatures(locationList) {
			var feature;

			for(feature in locationList) {
				if(locationList.hasOwnProperty(feature)) {
					features.push(locationList[feature]);
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
				var currentScore = 0,
					oldScore = 0,
					points,
					target,
					currentName = currentFeature.properties.name.toLowerCase(),
					oldName = oldFeature.properties.name.toLowerCase(),
					i;

				if(currentName < oldName) {
					currentScore++;
				}
				else {
					oldScore++;
				}

				for(i = 0; i < SEARCH_RESULTS.inputComponent.length; i++) {
					points = ((SEARCH_RESULTS.inputComponent.length - i) << 2);
					target = SEARCH_RESULTS.inputComponent[i].toLowerCase();

					currentScore += SEARCH_RANK.getScore(currentFeature, target, points);
					oldScore += SEARCH_RANK.getScore(oldFeature, target, points);
				}

				/*
				 console.log(currentFeature);
				 console.log(currentName + ": " + currentScore);
				 console.log(oldName + ": " + oldScore);
				 console.log("===");
				 */

				return oldScore - currentScore;
			});

			SEARCH_RESULTS.updateTable(features);
		});

		$("#location-types").change();
	}

	return;
}

searchResults.prototype.updateTable = function(features) {
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
		appendString = appendString + "<td class='type-col'>" + properties.locationTypeName + "</td>";

		root = properties.lineage[0];
		if(root) {
			appendString = appendString + "<td class='within-col' id='result_lineage" + i + "'></td></tr>";
			resultBody.append(appendString);

			listLineageRefs(properties.lineage, "#result_lineage" + i);
		}
		else {
			appendString = appendString + "<td class='within-col'></td></tr>";
			resultBody.append(appendString);
		}
	}

	return;
}

searchResults.prototype.processResults = function(data) {
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

searchResults.prototype.runQuery = function() {
	var thisQuery = this;
	this.originalInput = $("#input").val(),
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
					SEARCH_RESULTS.processResults(data);

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
