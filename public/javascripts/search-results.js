/*
search-results.js
*/

var searchURL = context + "/api/locations";
var pointURL = context + "/api/locations-by-coordinate";
var browswerURL = context + "/browser";

$(document).ready(function() {
	var query = getURLParameterByName("q");
	
	if(query) {
		$("#input").val(query);
		searchQuery();
	}
	
	return;
});

function searchQuery() {
	var input = $("#input").val();
	var url = searchURL + "?limit=0&q=" + encodeURIComponent(input);
	var result = $("#result"); 
	result.text("Please wait ...");
	
	SEARCH_MAP.featureLayer.clearLayers();
	
	$.ajax({
		url: url,
		type: 'GET',
		success: function(data, status) {
			updateOutput(data, status, result);
			$("#result-count").append("<strong>" + $("#input").val() + "</strong>");
			
			return;
		},
		error: function(data, status) {
			result.text("Error: " + status);
			
			return;
		}
	});
	
	return;
}

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

function updateOutput(data, status, result) {
	var geoJSON = data.geoJSON;
	result.text("");
	
	var features = geoJSON.features,
	size = data.properties.resultSize,
	appendString = "<table class='table table-condensed pre-spaced' style='margin-bottom: 0px;'>";
	appendString += "<caption id='result-count'>" + size + " result(s) from searching </caption>";
	
	if(size > 0) {
		appendString += "<thead>";
		appendString += "<th class='location-col'>Location</th>";
		appendString += "<th class='type-col'>Type</th>";
		appendString += "<th class='within-col'>Located within</th>";
		appendString += "</thead>";
		appendString += "<tbody id='results-tbody'></tbody>";
		appendString += "</table>";
		
		result.append(appendString);
		var resultBody = $("#results-tbody");
		
		appendString = "";
		var properties,
		gid,
		url,
		to,
		i,
		length,
		root;
		for(i = 0, length = features.length; i < length; i++){
			properties = features[i].properties;
			gid = properties.gid;
			url = browswerURL + "?id=" + gid;
			to = "";
			
			if(properties.endDate) {
				to = " to " + properties.endDate;
			}
			
			appendString = "<tr><td class='location-col'><a href='"+ url +"'>"+ properties.headline  + "</a> from " + properties.startDate + to + "</td>";
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
	}
	else {
		appendString += "</table>";
		result.append(appendString);
	}
	
	return;
}
