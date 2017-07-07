var CRUD_PATH = CONTEXT + '/api/locations';

function MapDriver() {
	var id = '';//'12';
	
	this.title = "";
	this.mapID = id;

	if(this.mapID) {
		this.geoJSONURL = CRUD_PATH + "/" + id;
	}
	
	this.accessToken = 'pk.eyJ1IjoidHBzMjMiLCJhIjoiVHEzc0tVWSJ9.0oYZqcggp29zNZlCcb2esA';
	this.featureLayer = null;
	this.map = null;
	this.kml = null;
	this.parent = null;
	this.auComponents = [];
	this.suggestionIDs = [];
	
	this.initialize();
	
	return;
}

MapDriver.prototype.initialize = function() {
	L.mapbox.accessToken = this.accessToken;
	
	var southWest = L.latLng(-90, -180),
		northEast = L.latLng(90, 180),
		mapBounds = L.latLngBounds(southWest, northEast);
	
	this.map = L.mapbox.map('map-one', 'examples.map-i86l3621', {
		//maxBounds: mapBounds,
		worldCopyJump: true,
		bounceAtZoomLimits: false,
		zoom: 1.6,
		minZoom: 1.6
	});
	this.map.legendControl.addLegend(this.title);
	
	this.drawControl = null;
	
	this.loadFeatureLayer();
	
	return;
}

MapDriver.prototype.loadFeatureLayer = function() {
	var thisMapDriver = this,
		noLoad = false;
	
	if(this.geoJSONURL) {
		this.featureLayer = L.mapbox.featureLayer().loadURL(this.geoJSONURL);
	}
	else if(this.mapID) {
		this.featureLayer = L.mapbox.featureLayer().loadID(this.mapID);
	}
	else {
		this.featureLayer = L.mapbox.featureLayer();
		noLoad = true;
	}
	
	this.featureLayer.on('ready', function() {
		var geoJSON = thisMapDriver.featureLayer.getGeoJSON(),
			feature,
			parentGID;

		thisMapDriver.loadJSON(geoJSON);
		
		thisMapDriver.featureLayer.addTo(thisMapDriver.map);
		
		if(geoJSON) {
			feature = geoJSON.features[0];
			
			centerMap(geoJSON, thisMapDriver);
			
			thisMapDriver.mapID = thisMapDriver.featureLayer.getGeoJSON().id;
			thisMapDriver.kml = feature.properties.kml;
			HELPERS.setTextValue("#au-name", feature.properties.name);
			HELPERS.setTextValue("#description", feature.properties.locationDescription);
			HELPERS.setTextValue("#start-date", feature.properties.startDate);
			HELPERS.setTextValue("#end-date", feature.properties.endDate);
			
			parentGID = feature.properties.parentGid;
			console.log(parentGID);

			HELPERS.setTextValue("#gid", feature.properties.gid);
			feature.properties.title = feature.properties.name + " " + feature.properties.locationTypeName + " from " + feature.properties.startDate;
			
			if(feature.properties.endDate) {
				feature.properties.title = feature.properties.title + " to " + feature.properties.endDate;
			}
			else {
				feature.properties.title += " to present";
			}
			
			thisMapDriver.map.legendControl.removeLegend(thisMapDriver.title);
			thisMapDriver.title = "<strong>" + feature.properties.title + "</strong>";
			thisMapDriver.map.legendControl.addLegend(thisMapDriver.title);
		}
		
		if(!thisMapDriver.drawControl) {
			thisMapDriver.drawControl = new L.Control.Draw({
				draw: {
					polyline: false,
					rectangle: false,
					circle: false,
					marker: false
				},
				edit: {
					featureGroup: thisMapDriver.featureLayer
				}
			}).addTo(thisMapDriver.map);
		}
		
		thisMapDriver.map.on('draw:created', function(e) {
			thisMapDriver.featureLayer.addLayer(e.layer);
			console.log(e);
		});
		
		thisMapDriver.map.on('draw:deleted', function(e) {
			var layers = e.layers;

			layers.eachLayer(function(layer) {
				if(thisMapDriver.featureLayer.hasLayer(layer._leaflet_id + 1)) {
					console.log(thisMapDriver.featureLayer.removeLayer(layer._leaflet_id + 1));
				}
			});
		});
	});
	
	this.featureLayer.on('error', function(err) {
		console.log("Error: " + err['error']['statusText']);
		
		if((thisMapDriver.featureLayer.getLayers().length == 0) && thisMapDriver.mapID) {
			console.log("Attempting to load via mapbox ID");
			thisMapDriver.featureLayer = L.mapbox.featureLayer().loadID(thisMapDriver.mapID);
		}
		
		thisMapDriver.featureLayer.on('ready', function() {
			thisMapDriver.featureLayer.addTo(thisMapDriver.map);
			
			if(!thisMapDriver.drawControl) {
				thisMapDriver.drawControl = new L.Control.Draw({
					draw: {
						polyline: false,
						rectangle: false,
						circle: false,
						marker: true
					},
					edit: {
						featureGroup: thisMapDriver.featureLayer
					}
				}).addTo(thisMapDriver.map);
			}
			
			thisMapDriver.map.on('draw:created', function(e) {
				thisMapDriver.featureLayer.addLayer(e.layer);
				console.log(e);
			});
			
			thisMapDriver.map.on('draw:deleted', function(e) {
				var layers = e.layers;

				layers.eachLayer(function(layer) {
					if(thisMapDriver.featureLayer.hasLayer(layer._leaflet_id + 1)) {
						console.log(thisMapDriver.featureLayer.removeLayer(layer._leaflet_id + 1));
					}
				});
			});
		});
	});
	
	if(noLoad) {
		this.featureLayer.fireEvent("ready");
	}
	
	return;
}

MapDriver.prototype.loadJSON = function(jsonData) {
	if(jsonData) {
		var thisMapDriver = this,
			i,
			features = jsonData.features,
			feature,
			properties,
			title;
		
		for(i = 0; i < features.length; i++) {
			features[i].properties.description = features[i].properties.name;
		}
		
		this.featureLayer.setGeoJSON(jsonData);
		centerMap(jsonData, thisMapDriver);
		
		feature = jsonData.features[0];
		properties = feature.properties;
		title = properties.name + " " + properties.locationTypeName + " from " + properties.startDate;
		
		if(properties.endDate) {
			title = title + " to " + properties.endDate;
		}
		else {
			title += " to present";
		}
		
		this.map.legendControl.removeLegend(this.title);
		this.title = "<strong>" + title + "</strong>";
		this.map.legendControl.addLegend(this.title);
	}
	
	return;
}

MapDriver.prototype.saveMap = function() {
	// CREATE //POST /resources/aus
	// CREATE //POST /api/locations
	var httpType = "POST",
		URL = CRUD_PATH,
		data = this.featureLayer.toGeoJSON();

	data.id = this.mapID;
	
	if(!this.formatGeoJSON(data)) {
		return;
	}

/*
console.log("Sending JSON.stringify([" + data.type + "]):");
console.log(JSON.stringify(data));
console.log("Length: " + JSON.stringify(data).length);
*/

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
			HELPERS.setTextValue("#gid", HELPERS.getIDFromURI(response.getResponseHeader("Location")));
			$("#gid").prop("disabled", true);
			$("#new-button").show();
			$("#save-button").hide();
			$("#update-button").show();

			HELPERS.setTextValue("#server-result", "Success, ID: " + $("#gid").val() + " created");
			$("#server-result").css("color", "#008000");
			$("#server-result").show();
			$("#server-result").fadeOut(15000);
		},
		error: function(data, status) {
			console.log(status);
			console.log(data);
			HELPERS.setTextValue("#server-result", status + ": " + data.statusText + " - " + data.responseText);
			$("#server-result").css("color", "#800000");
			$("#server-result").show();
			$("#server-result").fadeOut(15000);
		}
	});
	
	return;
}

MapDriver.prototype.updateMap = function() {
	// UPDATE //PUT /resources/aus
	// UPDATE //PUT /api/locations
	var httpType = "PUT",
		URL = CRUD_PATH,
		data = this.featureLayer.toGeoJSON();

	data.id = this.mapID;
	
	URL = URL + "/" + HELPERS.getValueText("#gid");
	
	if(!this.formatGeoJSON(data)) {
		return;
	}
	
console.log("Sending JSON.stringify([" + data.type + "]):");
console.log(JSON.stringify(data));
console.log("Length: " + JSON.stringify(data).length);
	
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
			HELPERS.setTextValue("#gid", HELPERS.getIDFromURI(response.getResponseHeader("Location")));
			$("#gid").prop("disabled", true);
			$("#new-button").show();

			HELPERS.setTextValue("#server-result", "Success, ID: " + $("#gid").val() + " saved");
			$("#server-result").css("color", "#008000");
			$("#server-result").show();
			$("#server-result").fadeOut(15000);
		},
		error: function(data, status) {
			console.log(status);
			console.log(data);
			HELPERS.setTextValue("#server-result", status + ": " + data.statusText + " - " + data.responseText);
			$("#server-result").css("color", "#800000");
			$("#server-result").show();
			$("#server-result").fadeOut(15000);
		}
	});
	
	return;
}

MapDriver.prototype.deleteLocation = function() {
	// DELETE //DELETE /resources/aus
	// DELETE //DELETE /api/locations
	var httpType = "DELETE",
		URL = CRUD_PATH;
	
	URL = URL + "/" + HELPERS.getValueText("#gid");
	
	$.ajax({
		type: httpType,
		url: URL,
		success: function(data, status, response) {
			console.log(data);
			console.log(status);

			HELPERS.setTextValue("#server-result", "Success, ID: " + $("#gid").val() + " deleted");
			$("#server-result").css("color", "#008080");
			$("#server-result").show();
			$("#server-result").fadeOut(15000);
		},
		error: function(data, status) {
			console.log(status);
			console.log(data);

			HELPERS.setTextValue("#server-result", status + ": " + data.statusText + " - " + data.responseText);
			$("#server-result").css("color", "#800000");
			$("#server-result").show();
			$("#server-result").fadeOut(15000);
		}
	});
	
	return;
}

MapDriver.prototype.download = function() {
	var jsonData = this.featureLayer.toGeoJSON(),
		properties = null,
		i,
		data;
	
	for(i = 0; i < jsonData.features.length; i++) {
		properties = jsonData.features[i].properties;
		properties.name = HELPERS.getValueText("#au-name");
		properties.startDate = HELPERS.getValueText("#start-date");
		properties.endDate = HELPERS.getValueText("#end-date");
		properties.parentGid = this.parent;
		properties.description = HELPERS.getValueText("#description");
	}
	
	if(!jsonData.id) {
		jsonData.id = this.mapID;
	}
	
	data = "text/json;charset=utf-8," + encodeURIComponent(JSON.stringify(jsonData));
	
	//$('<a href="data:' + data + '" download="data.json">download JSON</a>').appendTo('#container');
	
	location.assign("data:'" + data);
	
	return jsonData;
}

MapDriver.prototype.upload = function() {
	var file = $('#file-input').get(0).files[0],
		fileReader = new FileReader(),
		thisMapDriver = this;
	
	fileReader.onload = (function() {
		var kmlData = fileReader['result'],
			kmlDOM = (new DOMParser()).parseFromString(kmlData, 'text/xml'),
			jsonData = toGeoJSON.kml(kmlDOM),
			properties,
			i,
			parentGID;

		thisMapDriver.kml = kmlData;

		if(jsonData.features.length == 0) {
			jsonData = JSON.parse(kmlData);
		}
		
		properties = jsonData.features[0].properties;
		
		if(properties.name) {
			HELPERS.setTextValue("#au-name", properties.name);
		}
	
		if(properties.description) {
			HELPERS.setTextValue("#description", properties.description);
		}
		
		if(properties.startDate) {
			HELPERS.setTextValue("#start-date", properties.startDate);
		}
		
		if(properties.endDate) {
			HELPERS.setTextValue("#end-date", properties.endDate);
		}

		for(i = 0; i < jsonData.features.length; i++) {
			jsonData.features[i].properties.description = jsonData.features[i].properties.name;
		}
		
		parentGID = properties.parentGid;
		console.log("parent GID: " + parentGID);
		
		thisMapDriver.loadJSON(jsonData);
		thisMapDriver.suggestParents();

		return;
	});
	
	var fileString = fileReader.readAsText(file);
	
	return;
}

MapDriver.prototype.suggestParents = function() {
	//POST /api/locations-by-geometry
	var thisMapDriver = this,
		httpType = "POST",
		URL = CONTEXT + "/api/locations-by-geometry?superTypeId=3",
		data = this.featureLayer.toGeoJSON();
	
	$.ajax({
		type: httpType,
		url: URL,
		data: JSON.stringify(data),
		contentType: "application/json; charset=UTF-8",
		//dataType: "json",
		//processData: false,
		success: function(data, status, response) {
			var i;
			console.log(data);
			console.log(status);
			console.log(response);
			
			//filter parent widget to features returned
			for(i = 0; i < data.features.length; i++) {
				thisMapDriver.suggestionIDs.push(data.features[i].properties.gid);
			}
			thisMapDriver.populateSuggestions();
		},
		error: function(data, status) {
			console.log(status);
			console.log(data);
			HELPERS.setTextValue("#server-result", status + ": " + data.statusText + " - " + data.responseText);
			$("#server-result").css("color", "#800000");
			$("#server-result").show();
			$("#server-result").fadeOut(15000);
		}
	});
	
	return;
}

MapDriver.prototype.populateSuggestions = function() {
	var thisMapDriver = this;
	
	if(this.suggestionIDs.length > 0) {
		PARENT_TREE.tree.filterNodes(function(node) {
			var set = new Set(thisMapDriver.suggestionIDs);
			
			return set.has(node.key);
		});
		
		$("#resetParentSearchButton").removeAttr("disabled");
	}
	
	return;
}

MapDriver.prototype.getAUComponents = function() {
	return this.auComponents;
}

MapDriver.prototype.addAUComponent = function(gID) {
	this.auComponents.push(gID)
	console.log(this.auComponents);
	
	return;
}

MapDriver.prototype.removeAUComponent = function(gID) {
	var i;
	for(i = 0; i < this.auComponents.length; i++) {
		if(this.auComponents[i] == gID) {
			this.auComponents.splice(i, 1);
			break;
		}
	}
	
	console.log(this.auComponents);
	
	return;
}

MapDriver.prototype.formatGeoJSON = function(geoJSON) {
	var id = HELPERS.getValueText("#gid"),
		auName = HELPERS.getValueText("#au-name"),
		locationTypeName = HELPERS.getValueText("#au-type"),
		auCode = HELPERS.getValueText("#au-code"),
		auCodeType = HELPERS.getValueText("#au-codetype"),
		startDate = HELPERS.getValueText("#start-date"),
		endDate = HELPERS.getValueText("#end-date"),
		auParentGID = this.parent,
		description = $("#description").val(),
		dateTokens = HELPERS.validDate(startDate),
		useParent,
		properties;

	if(startDate == "today") {
		startDate = new Date().toString();
		dateTokens = 3;
	}

	if(dateTokens > 2) {
		startDate = HELPERS.toServerDate(new Date(startDate), dateTokens);
	}
	else {
		alert("Invalid start date: " + startDate);

		return null;
	}

	dateTokens = HELPERS.validDate(endDate);
	if(endDate == "today") {
		endDate = new Date().toString();
		dateTokens = 3;
	}

	if(dateTokens > 2) {
		endDate = HELPERS.toServerDate(new Date(endDate), dateTokens);

		if((new Date(endDate)).valueOf() < (new Date(startDate)).valueOf()) {
			alert("It is not possible for the end date to occur before the start date");

			return null;
		}
	}
	else if(endDate.length > 0) {
		alert("Invalid end date: " + endDate); //TODO: append month/year OR change back-end to accomodate dates without months & days

		return null;
	}
	else {
		endDate = null;
	}

	if(auName.length == 0) {
		alert("Please enter the name");

		return  null;
	}

	if(locationTypeName.length == 0) {
		alert("Please enter the location type");

		return  null;
	}

	if((auCode.length > 0) && (auCodeType.length < 1)) {
		alert("Please enter the code type");

		return null;
	}
	else if((auCodeType.length > 0) && (auCode.length < 1)) {
		alert("Please enter the code");

		return null;
	}

	if((!auParentGID || (auParentGID.length < 1)) && (locationTypeName != "Country")) {
		alert("Please select an encompassing location");

		return null;
	}

	if(description.length == 0) {
		description = null;
	}

	if(geoJSON.features.length == 0) {
		useParent = confirm("You did not upload a KML file. Press OK if you want to use the selected Parent location as the Epidemic Zone.");

		if(useParent) {
			geoJSON.features = [
				{
					"type": "Feature",
					"geometry": null,
					"id": auParentGID
				}
			];

			geoJSON.id = auParentGID;
			geoJSON.properties = {};
			properties = geoJSON.properties;

			geoJSON.id = Number(id);
			properties["name"] = auName;
			properties["locationTypeName"] = locationTypeName;
			properties["codes"] = [{"code": auCode, "codeTypeName": auCodeType}];
			properties["locationDescription"] = description;
			properties["parentGid"] = auParentGID;
			properties["startDate"] = startDate;
			properties["endDate"] = endDate;

			return geoJSON
		}

		return null;
	}

	geoJSON.properties = {};
	properties = geoJSON.properties;

	geoJSON.id = Number(id);
	properties["name"] = auName;
	properties["locationTypeName"] = locationTypeName;
	properties["codes"] = [{"code": auCode, "codeTypeName": auCodeType}];
	properties["locationDescription"] = description;
	properties["parentGid"] = auParentGID;
	properties["startDate"] = startDate;
	properties["endDate"] = endDate;
	properties["kml"] = this.kml;

	return geoJSON;
}

/* Helper Functions */
function centerMap(geoJSON, thisMapDriver) {
		var geometryCount,
			geometry = geoJSON.features[0].geometry,
			latitude,
			longitude,
			minLat,
			maxLat,
			minLng,
			maxLng,
			a,
			geo,
			i,
			j,
			coordinates,
			coordinate,
			coordinatesCount,
			coordinateGroup,
			coordinateGroupCount,
			vertices,
			coordinatesBody,
			southWest,
			northEast,
			bounds;
	
	for(a = 0; a < geoJSON.features.length; a++) {
		geometry = geoJSON.features[a].geometry;
		
		if((!geometry) ||  (geometry.type == "Point")) {
			return;
		}
		
		if(geometry.geometries) {
			geometryCount = geometry.geometries.length;
			
			if(a == 0) {
				latitude = geometry.geometries[0].coordinates[0][0][1];
				longitude = geometry.geometries[0].coordinates[0][0][0];
				minLat = latitude;
				maxLat = minLat;
				minLng = longitude;
				maxLng = minLng;
			}
			
			for(geo = 0; geo < geometryCount; geo++) {
				coordinates = geometry.geometries[geo].coordinates;
				coordinatesCount = coordinates.length;
				
				for(i = 0; i < coordinatesCount; i++) {
					coordinateGroupCount = coordinates[i].length;
					coordinateGroup = coordinates[i];
					
					coordinate = null;
					for(j = 0; j < coordinateGroupCount; j++) {
						coordinate = coordinateGroup[j];
						
						latitude = coordinate[1];
						longitude = coordinate[0];
						
						if(latitude < minLat) {
							minLat = latitude;
						}
						else if(latitude > maxLat) {
							maxLat = latitude;
						}
						
						if(longitude < minLng) {
							minLng = longitude;
						}
						else if(longitude > maxLng) {
							maxLng = longitude;
						}
					}
				}
			}
		}
		else /*if(geometry.coordinates)*/ {
			geometryCount = geometry.coordinates.length;
			
			if(a == 0) {
				latitude = geometry.coordinates[0][0][1];
				longitude = geometry.coordinates[0][0][0];
				minLat = latitude;
				maxLat = minLat;
				minLng = longitude;
				maxLng = minLng;
			}
			
			vertices;
			coordinatesBody;
			for(i = 0; i < geometryCount; i++) {
				coordinatesBody = geometry.coordinates[i];
				vertices = geometry.coordinates[i].length;
				
				for(j = 0; j < vertices; j++) {
					latitude = geometry.coordinates[i][j][1];
					longitude = geometry.coordinates[i][j][0];
					
					if(latitude < minLat) {
						minLat = latitude;
					}
					else if(latitude > maxLat) {
						maxLat = latitude;
					}
					
					if(longitude < minLng) {
						minLng = longitude;
					}
					else if(longitude > maxLng) {
						maxLng = longitude;
					}
				}
			}
		}
	}
	
	southWest = L.latLng(minLat, minLng);
	northEast = L.latLng(maxLat, maxLng);
	bounds = L.latLngBounds(southWest, northEast);
	
	console.log(bounds);
	
	return thisMapDriver.map.fitBounds(bounds);
}
