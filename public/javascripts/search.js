var searchURL = context + "/api/locations";
var browswerURL = context + "/browser";
$( document ).ready(function() {
	$("#search-button").click(function() {
			searchClick();
	});
});

function searchClick(){
	var input = $("#input").val();
	var url = searchURL + "?q=" + input; 
	$.get(url, function(data,status){
	    updateOutput(data,status);
	});
	
}
var geoJSON;
function updateOutput(data,status){
	geoJSON = data.geoJSON;
	
	var features = geoJSON.features;
	
	var result = $("#result"); 
	result.text("");
	result.append("<table style='width:100%'>");
	result.append("<thead>")
	result.append("<th>ID</th>")
	result.append("<th>short description</th>")
	result.append("<th>in</th>")
	result.append("</thead>")
	for (var i = 0, l = features.length; i < l; i++){
		var p = features[i].properties;
		result.append("<tr>")
		var gid = p.gid
		var url = browswerURL + "?id=" + gid;
		result.append("<td><a href='"+ url +"'>"+ gid  + "</a></td>")
		result.append("<td><b>"+  p.name + "</b> from " + p.startDate + " to " + p.endDate + "</td>")
		var root = p.lineage[0]
		if (root){
			result.append("<td>"+ root.name  + "</td>")
		}
		result.append("</tr>")
	}
	result.append("</table>");
    
}