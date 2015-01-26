var searchURL = context + "/api/locations";
var browswerURL = context + "/browser";
$(document).ready(function() {
	$("#search-button").click(function() {
			searchClick();
			
			return;
	});
	
	$("#input").keyup(function(event) {
		switch(event.which)
		{
			case 13:
				$("#search-button").click();
				$("#search-button").focus();
			break;
			
			default:
			break;
		}
		
		return;
	})
	
	var limit = 5;
	
	bindSuggestionBox("#input", context + "/api/unique-location-names?limit=" + limit + "&q=");
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
			updateOutput(data,status,result);
			
			return;
		},
		error: function(data, status) {
			result.text("Error: " + status);
			
			return;
		}
	});
	
}

var geoJSON;
function updateOutput(data, status, result) {
	geoJSON = data.geoJSON;
	result.text("");
	
	var features = geoJSON.features;
	var size = data.properties.resultSize;
	
	$("#result-count").text(size + " result(s)");
	var appendString = "<table class='table table-condensed' style='margin-bottom: 0px;'>";
	appendString += "<thead>";
	appendString += "<th>Location</th>";
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
		
		appendString = "<tr><td><a href='"+ url +"'>"+ p.headline  + "</a> from " + p.startDate + to + "</td>";
		
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
	
	return;
}

function bindSuggestionBox(inputBox, URL) {
	jQuery(inputBox).autocomplete({
		source: function (request, response) {
			jQuery.get(URL + $(inputBox).val(), {
				name: request.name
			}, function (data) {
				var index = 0;
				var locations = [];
				
				locations[0] = "No search results found...";
				
				for(index = 0; index < data.length; index++) {
					locations[index] = data[index]['name'];
				}
				
				response(locations);
			});
		},
		minLength: 3,
		delay: 500
	});
	
	return;
}
