/*
search-results.js
*/

var searchURL = context + "/api/locations";
var pointURL = context + "/api/locations-by-coordinate";
var browserURL = context + "/browser";

$(document).ready(function() {
	var query = getURLParameterByName("q");
	
	if(query) {
		$("#input").val(query);
		runQuery();
	}
	
	return;
});

function searchByGeoJSON(geoJSON) {
	//POST /api/locations-by-geometry
	var httpType = "POST",
	URL = context + "/api/locations-by-geometry?superTypeId=3",
	data = geoJSON,
	result = $("#result");
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
			
			data = {geoJSON: data, properties: {resultSize: data.features.length}};
			
			updateOutput(data, status, result);
			$("#result-count").append("<strong>user selection</strong>");
			
			return;
		},
		error: function(data, status) {
			console.log(status);
			console.log(data);
			result.text(status + ": " + data.statusText);
			result.append(data.responseText);
			
			return;
		}
	});
	
	return;
}

function searchPoint(latitude, longitude) {
	var url = pointURL + "?lat=" + latitude + "&long=" + longitude;
	var result = $("#result");
	result.text("Please wait...");
	
	$.ajax({
		url: url,
		type: 'GET',
		success: function(data, status) {
			updateOutput(data, status, result);
			$("#result-count").append("<strong>latitude: " + latitude + ", longitude: " + longitude + "</strong>");
			
			return;
		},
		error: function(data, status) {
			result.text(status + ": " + data.statusText);
			result.append(data.responseText);
			
			return;
		}
	});
	
	return;
}

function runQuery() {
	var originalInput = $("#input").val(),
		inputComponent = originalInput.replace(',', '').split(' ');

	function searchQuery() {
		var typeList = {},
			totalCount = 0,
			stillWaiting = inputComponent.length,
			i;

		$("#result").text("Please wait ...");
		SEARCH_MAP.featureLayer.clearLayers();

		function getQueryResults(input) {
			var url = searchURL + "?limit=0&q=" + encodeURIComponent(input);

			$.ajax({
				url: url,
				type: 'GET',
				success: function(data, status) {
					var properties,
						features = data.geoJSON.features,
						i;

					for(i = 0, length = features.length; i < length; i++) {
						properties = features[i].properties;

						if(!typeList[properties.locationTypeName]) {
							typeList[properties.locationTypeName] = {};
						}

						if(!typeList[properties.locationTypeName][properties.gid]) {
							typeList[properties.locationTypeName][properties.gid] = features[i];
							totalCount++;
						}
					}

					$("#result-count").append("<strong>" + $("#input").val() + "</strong>");

					return;
				},
				error: function(data, status) {
					console.warn(status);

					if(totalCount === 0) {
						$("#result").text("No results found");
					}

					return;
				},
				complete: function(xhr, status) {
					stillWaiting--;

					if(!stillWaiting) {
						updateOutput(typeList, totalCount);
					}
				}
			});

			return;
		}

		if(originalInput !== inputComponent[0]) {
			stillWaiting++;
			getQueryResults(originalInput);
		}

		for(i = 0; i < inputComponent.length; i++) {
			getQueryResults(inputComponent[i]);
		}

		return;
	}

	function updateOutput(locationList, totalCount) {
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

			function getScore(inputFeature, target, points) {
				var scoredPoints = 0,
					inputName = inputFeature.properties.name.toLowerCase(),
					searchIndex = inputName.search(target),
					lineage = inputFeature.properties.lineage,
					j;

				if(searchIndex >= 0) {
					scoredPoints += points;

					if(inputName === target) {
						scoredPoints += (points >> 1);
					}
					else if(searchIndex === 0) {
						scoredPoints += (points >> 2);
					}
				}
				else {
					for(j = 0; j < lineage.length; j++) {
						searchIndex = lineage[j].name.toLowerCase().search(target);
						if(searchIndex >= 0) {
							scoredPoints += (points >> 1);

							if(inputName === target) {
								scoredPoints += (points >> 1);
							}
							else if(searchIndex === 0) {
								scoredPoints += (points >> 2);
							}
						}
					}
				}

				return scoredPoints;
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

					for(i = 0; i < inputComponent.length; i++) {
						points = ((inputComponent.length - i) << 2);
						target = inputComponent[i].toLowerCase();

						currentScore += getScore(currentFeature, target, points);
						oldScore += getScore(oldFeature, target, points);
					}

					/*
					console.log(currentFeature);
					console.log(currentName + ": " + currentScore);
					console.log(oldName + ": " + oldScore);
					console.log("===");
					*/

					return oldScore - currentScore;
				});

				updateTable(features);
			});

			$("#location-types").change();
		}

		return;
	}

	function updateTable(features) {
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
			url = browserURL + "?id=" + gid;
			to = "";

			if(properties.endDate) {
				to = " to " + properties.endDate;
			}

			aliasesString = "";
			for(j = 0; j < properties.otherNames.length; j++) {
				aliasesString += properties.otherNames[j].name;

				if(j < (properties.otherNames.length - 1)) {
					aliasesString += ", ";
				}
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

	return searchQuery();
}
