var crudPath = context + '/api/locations';

function MapDriver() {
	var id = '';//'12';
	this.title = "";//"<strong>Sierra Leone</strong> 0001-01-01 to now";
	this.mapID = id;//'tps23.k1765f0g';
	if(this.mapID) {
		this.geoJSONURL = crudPath + "/" + id;
		//"http://tps23-nb.univ.pitt.edu/test.json";
	}
	
	this.accessToken = 'pk.eyJ1IjoidHBzMjMiLCJhIjoiVHEzc0tVWSJ9.0oYZqcggp29zNZlCcb2esA';
	this.featureLayer = null;
	this.map = null;
	this.kml = null;
	this.parent = null;
	this.auComponents = [];
	
	this.initialize();
	
	return;
}

MapDriver.prototype.initialize = function() {
	L.mapbox.accessToken = this.accessToken;
	
	var southWest = L.latLng(-90, -180);
	var northEast = L.latLng(90, 180);
	var mapBounds = L.latLngBounds(southWest, northEast);
	
	this.map = L.mapbox.map('map-one', 'examples.map-i86l3621', { worldCopyJump: true, bounceAtZoomLimits: false, zoom: 1, minZoom: 1, maxBounds: mapBounds /*crs: L.CRS.EPSG385*/});
	this.map.legendControl.addLegend(this.title);
	
	this.drawControl = null;
	
	this.loadFeatureLayer();
	
	return;
}

MapDriver.prototype.loadFeatureLayer = function() {
	var thisMapDriver = this;
	var noLoad = false;
	
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
		var geoJSON = thisMapDriver.featureLayer.getGeoJSON();
		thisMapDriver.loadJSON(geoJSON);
		
		thisMapDriver.featureLayer.addTo(thisMapDriver.map);
		
		if(geoJSON) {
			var feature = geoJSON.features[0];
			
			centerMap(geoJSON, thisMapDriver);
			
			thisMapDriver.mapID = thisMapDriver.featureLayer.getGeoJSON().id;
			thisMapDriver.kml = feature.properties.kml;
			setTextValue("#au-name", feature.properties.name);
			setTextValue("#description", feature.properties.locationDescription);
			setTextValue("#start-date", feature.properties.startDate);
			setTextValue("#end-date", feature.properties.endDate);
			
			var parentGID = feature.properties.parentGid;
			console.log(parentGID);
			
			setTextValue("#gid", feature.properties.gid);
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
		var thisMapDriver = this;
		
		//multiPolygonsToPolygons(jsonData);
		var i;
		var features = jsonData.features;
		var properties;
		
		for(i = 0; i < features.length; i++) {
			features[i].properties.description = features[i].properties.name;
		}
		
		this.featureLayer.setGeoJSON(jsonData);
		centerMap(jsonData, thisMapDriver);
		
		var feature = jsonData.features[0];
		properties = feature.properties;
		var title = properties.name + " " + properties.locationTypeName + " from " + properties.startDate;
		
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
	var httpType = "POST";
	var URL = crudPath;
	
	var data = this.featureLayer.toGeoJSON();
	data.id = this.mapID;
	
	if(!formatGeoJSON(data, this)) {
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
			setTextValue("#gid", getIDFromURI(response.getResponseHeader("Location")));
			$("#gid").prop("disabled", true);
			$("#new-button").show();
			$("#save-button").hide();
			$("#update-button").show();
			
			setTextValue("#server-result", "Success, ID: " + $("#gid").val() + " created");
			$("#server-result").css("color", "#008000");
			$("#server-result").show();
			$("#server-result").fadeOut(15000);
		},
		error: function(data, status) {
			console.log(status);
			console.log(data);
			setTextValue("#server-result", status + ": " + data.statusText + " - " + data.responseText);
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
	var httpType = "PUT";
	var URL = crudPath;
	
	var data = this.featureLayer.toGeoJSON();
	data.id = this.mapID;
	
	URL = URL + "/" + getValueText("#gid");
	
	if(!formatGeoJSON(data, this)) {
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
			setTextValue("#gid", getIDFromURI(response.getResponseHeader("Location")));
			$("#gid").prop("disabled", true);
			$("#new-button").show();
			
			setTextValue("#server-result", "Success, ID: " + $("#gid").val() + " saved");
			$("#server-result").css("color", "#008000");
			$("#server-result").show();
			$("#server-result").fadeOut(15000);
		},
		error: function(data, status) {
			console.log(status);
			console.log(data);
			setTextValue("#server-result", status + ": " + data.statusText + " - " + data.responseText);
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
	var httpType = "DELETE";
	var URL = crudPath;
	
	URL = URL + "/" + getValueText("#gid");
	
	$.ajax({
		type: httpType,
		url: URL,
		success: function(data, status, response) {
			console.log(data);
			console.log(status);
			
			setTextValue("#server-result", "Success, ID: " + $("#gid").val() + " deleted");
			$("#server-result").css("color", "#008080");
			$("#server-result").show();
			$("#server-result").fadeOut(15000);
		},
		error: function(data, status) {
			console.log(status);
			console.log(data);
			
			setTextValue("#server-result", status + ": " + data.statusText + " - " + data.responseText);
			$("#server-result").css("color", "#800000");
			$("#server-result").show();
			$("#server-result").fadeOut(15000);
		}
	});
	
	return;
}

MapDriver.prototype.download = function() {
	var jsonData = this.featureLayer.toGeoJSON();
	var properties = null;
	
	for(var i = 0; i < jsonData.features.length; i++) {
		properties = jsonData.features[i].properties;
		properties.name = getValueText("#au-name");
		properties.startDate = getValueText("#start-date");
		properties.endDate = getValueText("#end-date");
		properties.parentGid = this.parent;
		properties.description = getValueText("#description");
	}
	
	if(!jsonData.id) {
		jsonData.id = this.mapID;
	}
	
	var data = "text/json;charset=utf-8," + encodeURIComponent(JSON.stringify(jsonData));
	
	//$('<a href="data:' + data + '" download="data.json">download JSON</a>').appendTo('#container');
	
	location.assign("data:'" + data);
	
	return jsonData;
}

MapDriver.prototype.upload = function() {
	var file = $('#file-input').get(0).files[0];
	var fileReader = new FileReader();
	var thisMapDriver = this;
	
	fileReader.onload = (function() {
		var kmlData = fileReader['result'];
		thisMapDriver.kml = kmlData;
		var kmlDOM = (new DOMParser()).parseFromString(kmlData, 'text/xml');
		
		var jsonData = toGeoJSON.kml(kmlDOM);
		
		if(jsonData.features.length == 0) {
			jsonData = JSON.parse(kmlData);
		}
		
		var properties = jsonData.features[0].properties;
		
		if(properties.name) {
			setTextValue("#au-name", properties.name);
		}
	
		if(properties.description) {
			setTextValue("#description", properties.description);
		}
		
		if(properties.startDate) {
			setTextValue("#start-date", properties.startDate);
		}
		
		if(properties.endDate) {
			setTextValue("#end-date", properties.endDate);
		}
		
		var i;
		for(i = 0; i < jsonData.features.length; i++) {
			jsonData.features[i].properties.description = jsonData.features[i].properties.name;
		}
		
		var parentGID = properties.parentGid;
		console.log("parent GID: " + parentGID);
		
		thisMapDriver.loadJSON(jsonData);
	});
	
	var fileString = fileReader.readAsText(file);
	
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

/* Helper Functions */
function formatGeoJSON(geoJSON, thisMapDriver) {
	var i;
	var geometry;
	var id = getValueText("#gid");
	var auName = getValueText("#au-name");
	var locationTypeName = getValueText("#au-type");
	var auCode = getValueText("#au-code");
	var auCodeType = getValueText("#au-codetype");
	var startDate = getValueText("#start-date");
	var endDate = getValueText("#end-date");
	var auParentGID = thisMapDriver.parent;
	var description = $("#description").val();
	
	var dateTokens = validDate(startDate);
	if(startDate == "today") {
		startDate = new Date().toString();
		dateTokens = 3;
	}
	
	if(dateTokens > 2) {
		startDate = toServerDate(new Date(startDate), dateTokens);
	}
	else {
		alert("Invalid start date: " + startDate);
		
		return null;
	}
	
	dateTokens = validDate(endDate);
	if(endDate == "today") {
		endDate = new Date().toString();
		dateTokens = 3;
	}
	
	if(dateTokens > 2) {
		endDate = toServerDate(new Date(endDate), dateTokens);
		
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
	
	if((!geoJSON) || (!thisMapDriver.kml)) {
		alert("Please upload a kml file");
		
		return null;
	}
	
	if(description.length == 0) {
		description = null;
	}
	
	geoJSON.properties = {};
	var properties = geoJSON.properties;
	
	geoJSON.id = Number(id);
	properties["kml"] = thisMapDriver.kml;
	properties["name"] = auName;
	properties["locationTypeName"] = locationTypeName;
	properties["codes"] = [{"code": auCode, "codeTypeName": auCodeType}];
	properties["locationDescription"] = description;
	properties["parentGid"] = auParentGID;
	properties["startDate"] = startDate;
	properties["endDate"] = endDate;
	
	return geoJSON;
}

function centerMap(geoJSON, thisMapDriver) {
	var geometry = null;
	var geometryCount = null;
	
	geometry = geoJSON.features[0].geometry;
	
	var latitude = null;
	var longitude = null;
	var minLat = null;
	var maxLat = null;
	var minLng = null;
	var maxLng = null;
	
	for(var a = 0; a < geoJSON.features.length; a++) {
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
			
			for(var geo = 0; geo < geometryCount; geo++) {
				var coordinates = geometry.geometries[geo].coordinates;
				var coordinatesCount = coordinates.length;
				
				for(var i = 0; i < coordinatesCount; i++) {
					var coordinateGroupCount = coordinates[i].length;
					var coordinateGroup = coordinates[i];
					
					var coordinate = null;
					for(var j = 0; j < coordinateGroupCount; j++) {
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
			
			var vertices;
			var coordinatesBody;
			for(var i = 0; i < geometryCount; i++) {
				coordinatesBody = geometry.coordinates[i];
				vertices = geometry.coordinates[i].length;
				
				for(var j = 0; j < vertices; j++) {
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
	
	var southWest = L.latLng(minLat, minLng);
	var northEast = L.latLng(maxLat, maxLng);
	var bounds = L.latLngBounds(southWest, northEast);
	
	console.log(bounds);
	
	return thisMapDriver.map.fitBounds(bounds);
}
