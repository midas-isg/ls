/*
search-results.js
*/

var searchURL = context + "/api/locations";
var pointURL = context + "/api/locations-by-coordinate";
var browswerURL = context + "/browser";

$(document).ready(function() {
	$("#search-button").click(function() {
			searchClick();
			
			return;
	});
	
	var query = getURLParameterByName("q");
	
	if(query) {
		$("#input").val(query);
		$("#search-button").click();
	}
	
	return;
});

function searchClick() {
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

function searchPoint(latitude, longitude) {
	var url = pointURL + "?lat=" + latitude + "&long=" + longitude;
	var result = $("#result");
	result.text("Please wait ...");
	
	$.ajax({
		url: url,
		type: 'GET',
		success: function(data, status) {
			updateOutput(data, status, result);
			$("#result-count").append("<strong>latitude: " + latitude + ", longitude: " + longitude + "</strong>");
			
			return;
		},
		error: function(data, status) {
			result.text("Error: " + status);
			
			return;
		}
	});
	
	return;
}

var geoJSON;
function updateOutput(data, status, result) {
	geoJSON = data.geoJSON;
	result.text("");
	
	var features = geoJSON.features;
	var size = data.properties.resultSize;
	
	var appendString = "<table class='table table-condensed pre-spaced' style='margin-bottom: 0px;'>";
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
		var p;
		var gid;
		var url;
		var to;
		var i;
		var l;
		for(i = 0, l = features.length; i < l; i++){
			p = features[i].properties;
			gid = p.gid;
			url = browswerURL + "?id=" + gid;
			to = "";
			
			if(p.endDate) {
				to = " to " + p.endDate;
			}
			
			appendString = "<tr><td class='location-col'><a href='"+ url +"'>"+ p.headline  + "</a> from " + p.startDate + to + "</td>";
			appendString = appendString + "<td class='type-col'>" + p.locationTypeName + "</td>";
			
			var root = p.lineage[0];
			if(root) {
				appendString = appendString + "<td class='within-col' id='result_lineage" + i + "'></td></tr>";
				resultBody.append(appendString);
				
				listLineageRefs(p.lineage, "#result_lineage" + i);
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
