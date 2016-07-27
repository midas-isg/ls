/*
Common Functions
*/

function getIDFromURI(URI) {
	var components = URI.split('/');
	var id = components[components.length - 1];
	
	return id;
}

function multiPolygonsToPolygons(geoJSON) {
	if(geoJSON) {
		var features = geoJSON.features;
		var count = features.length;
		
		var i;
		var j;
		var properties = null;
		var addedFeature = null;
		for(i = 0; i < count; i++) {
			if(features[i].geometry.type == "MultiPolygon") {
				properties = features[i].properties;
				
				for(j = 0; j < features[i].geometry.coordinates.length; j++) {
					features.push({"type": "Feature", "geometry": {"type": "Polygon", "coordinates": null}, "properties": properties});
					addedFeature = features[features.length - 1];
					addedFeature.geometry.coordinates = features[i].geometry.coordinates[j];
				}
				
				features.splice(i, 1);
				i--;
				count = features.length;
			}
		}
		
		try {
			console.log(geoJSON);
			var JSONString = JSON.stringify(geoJSON);
			JSON.parse(JSONString);
		}
		catch(error) {
			alert(error);
		}
	}
	
	return geoJSON;
}

function validDate(dateString) {
	var date = new Date(dateString);
	
	if(date.valueOf()) {
		var tokens;
		
		if(dateString.search("-") != -1) {
			tokens = dateString.split("-");
		}
		else {
			tokens = dateString.split("/");
		}
		
		return tokens.length;
	}
	
	return 0;
}

function toServerDate(inputDate, fields) {
	var serverDate = "";
	
	serverDate = serverDate.concat(inputDate.getUTCFullYear());
	
	if(fields > 1) {
		serverDate = serverDate.concat("-");
		
		if(inputDate.getUTCMonth() < 9) {
			serverDate = serverDate.concat("0");
		}
		
		serverDate = serverDate.concat((inputDate.getUTCMonth() + 1));
		
		if(fields > 2) {
			serverDate = serverDate.concat("-");
			
			if(inputDate.getUTCDate() < 10) {
				serverDate = serverDate.concat("0");
			}
			
			serverDate = serverDate.concat(inputDate.getUTCDate());
		}
	}
	
	return serverDate;
}

function setTextValue(selector, input) {
	$(selector).text(input);
	$(selector).val(input);
	
	return;
}

function getValueText(selector) {
	var value = $(selector).val();
	
	if(value == "") {
		return $(selector).text();
	}
	
	return value;
}

function getURLParameterByName(name) {
	name = name.replace(/[\[]/, "\\[").replace(/[\]]/, "\\]");
	var regex = new RegExp("[\\?&]" + name + "=([^&#]*)"),
		results = regex.exec(location.search);
	
	return results == null ? "" : decodeURIComponent(results[1].replace(/\+/g, " "));
}

function listLineageRefs(lineage, sectionID) {
	var i,
		auName,
		auGID,
		show;
	
	show = false;
	if(lineage && (lineage.length > 0)) {
		$(sectionID).show();

		i = (lineage.length - 1);
		auName = lineage[i].name;
		auGID = lineage[i].gid;
		$(sectionID).append("<a href='" + context + "/browser?id=" + auGID + "' class='' style='text-decoration: underline;' title='ID: "+ auGID +"'>" + auName + "</a>");
		for(i--; i >= 0; i--) {
			auName = lineage[i].name;
			auGID = lineage[i].gid;

			$(sectionID).append(", ");
			$(sectionID).append("<a href='" + context + "/browser?id=" + auGID + "' class='pre-spaced' style='text-decoration: underline;' title='ID: "+ auGID +"'>" + auName + "</a>");
		}
	}
	
	return;
}

function getFirstAlphaOnly(input) {
	var output = "";
	
	for(var i = 0; i < input.length; i++) {
		if((input.charAt(i) == ' ') || ((input.charAt(i) >= 'a') && (input.charAt(i) <= 'z')) || ((input.charAt(i) >= 'A') && (input.charAt(i) <= 'Z'))) {
			output += input.charAt(i);
		}
		else if(input.charAt(i) != ' ') {
			output = output.trim();
			
			break;
		}
	}
	
	return output;
}

function backslashParentheses(input) {
	output = input.replace("(", "\\(");
	return output.replace(")", "\\)");
}
