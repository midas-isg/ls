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
		for(i = 0; i < count; i++) {
			if(features[i].geometry.type == "MultiPolygon") {
				var properties = features[i].properties;
				properties.description = properties.name;
				
				for(j = 0; j < features[i].geometry.coordinates.length; j++) {
					features.push({"type": "Feature", "geometry": {"type": "Polygon", "coordinates": null}, "properties": properties});
					var addedFeature = features[features.length - 1];
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
	var i;
	var auName;
	var auGID;
	var show;
	
	show = false;
	if(lineage && (lineage.length > 0)) {
		$(sectionID).show();
		
		for(i = (lineage.length - 1); i >= 0; i--) {
			auName = lineage[i].name;
			auGID = lineage[i].gid;
			
			$(sectionID).append("<a href='" + context + "/browser?id=" + auGID + "' class='pre-spaced' style='text-decoration: underline;' title='ID: "+ auGID +"'>" + auName + "</a>");
			
			if(i > 0) {
				$(sectionID).append(", ");
			}
		}
	}
	
	return;
}
