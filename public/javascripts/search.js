var searchURL = context + "/api/locations";
var browswerURL = context + "/browser";
$( document ).ready(function() {
	$("#search-button").click(function() {
			searchClick();
	});
});

function searchClick(){
	var input = $("#input").val();
	var url = searchURL + "?limit=0&q=" + encodeURIComponent(input); 
	var result = $("#result"); 
	result.text("Please wait ...");
	$.get(url, function(data, status){
		updateOutput(data,status,result);
	});
	
}

var geoJSON;
function updateOutput(data, status, result){
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