var searchURL = context + "/api/locations";
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
	
	$.ajax({
		url: url,
		type: 'GET',
		success: function(data, status) {
			updateOutput(data, status, result);
			
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
	
	$("#result-count").text(size + " result(s)");
	
	if(size > 0) {
		var appendString = "<table class='table table-condensed' style='margin-bottom: 0px;'>";
		appendString += "<thead>";
		appendString += "<th>Location</th>";
		appendString += "<th>Type</th>";
		appendString += "<th>Located within</th>";
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
			
			appendString = "<tr><td><a href='"+ url +"'>"+ p.headline  + "</a> from " + p.startDate + to + "</td><td>" + p.locationTypeName + "</td>";
			
			var root = p.lineage[0];
			if(root) {
				appendString = appendString + "<td id='result_lineage" + i + "'></td></tr>";
				resultBody.append(appendString);
				
				listLineageRefs(p.lineage, "#result_lineage" + i);
			}
			else {
				appendString = appendString + "<td></td></tr>";
				resultBody.append(appendString);
			}
		}
	}
	
	return;
}
