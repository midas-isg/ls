var searchURL = context + "/api/locations";
var browswerURL = context + "/browser";
$(document).ready(function() {
	$("#search-button").click(function() {
			searchClick();
	});
	
	$("#input").keyup(function(event) {
		switch(event.which)
		{
			case 13:
				$("#search-button").click();
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
	
	var features = geoJSON.features;
	
	var size = data.properties.resultSize;
	result.text("");
	result.append("# results="+ size + "<br>");
	result.append("<table style='width:100%'>");
	result.append("<thead>")
	//result.append("<th>ID</th>")
	result.append("<th>Location</th>");
	result.append("<th>Located within</th>");
	//result.append("</thead>");
	
	for(var i = 0, l = features.length; i < l; i++){
		var p = features[i].properties;
		var gid = p.gid;
		var url = browswerURL + "?id=" + gid;
		result.append("<tr>");
		//result.append("<td><a href='"+ url +"'>"+ gid  + "</a></td>")
		var to = "";
		if (p.endDate){
			to = " to " + p.endDate;
		}
		result.append("<td><a href='"+ url +"'>"+ p.headline  + "</a> from " + p.startDate + to + "</td>")
		var root = p.lineage[0]
		if (root){
			//result.append("<td>"+ root.name  + "</td>")
			result.append("<td id='result_lineage" + i + "'></td>");
			listLineageRefs(p.lineage, "#result_lineage" + i);
		}
		//result.append("</tr>")
	}
	//result.append("</table>");
	
	return;
}