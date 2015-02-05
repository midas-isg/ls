var crudPath = context + '/resources/aus';

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
	
	this.map = L.mapbox.map('map-one', 'examples.map-i86l3621', { worldCopyJump: true /*crs: L.CRS.EPSG385*/});
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
			var feature = thisMapDriver.featureLayer.getGeoJSON().features[0];
			
			centerMap(thisMapDriver.featureLayer.getGeoJSON(), thisMapDriver);
			
			thisMapDriver.mapID = thisMapDriver.featureLayer.getGeoJSON().id;
			setTextValue("#au-name", feature.properties.name);
			setTextValue("#au-code", feature.properties.code);
			setTextValue("#start-date", feature.properties.startDate);
			setTextValue("#end-date", feature.properties.endDate);
			
			var i;
			var parentGID = feature.properties.parentGid;
			console.log(parentGID);
			
			setTextValue("#gid", feature.properties.gid);
			feature.properties.title = feature.properties.name + " [" + feature.properties.codePath + "] " + "; " + feature.properties.startDate;
			
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
		
		multiPolygonsToPolygons(jsonData);
		
		this.featureLayer.setGeoJSON(jsonData);
		centerMap(jsonData, thisMapDriver);
		
		var feature = jsonData.features[0];
		var title = feature.properties.name + " [" + feature.properties.codePath + "] " + "; " + feature.properties.startDate;
		this.map.legendControl.removeLegend(this.title);
		this.title = "<strong>" + title + "</strong>";
		this.map.legendControl.addLegend(this.title);
	}
	
	return;
}

MapDriver.prototype.saveMap = function() {
	// Create //POST /resources/aus
	var httpType = "POST";
	var URL = crudPath;
	
	/*
	if(isUpdate) {
		// Update //PUT /resources/aus
		httpType = "PUT";
	}
	*/
	
	var data = this.featureLayer.toGeoJSON();
	data.id = this.mapID;
	
	function formatGeoJSON(geoJSON, thisMapDriver) {
		var i;
		var geometry;
		var auName = getValueText("#au-name");
		var auType = getValueText("#au-type");
		var auCode = getValueText("#au-code");
		var auCodeType = getValueText("#au-codetype");
		var startDate = getValueText("#start-date");
		var endDate = getValueText("#end-date");
		var auParentGID = thisMapDriver.parent;
		
		var dateTokens = validDate(startDate);
		if(startDate == "today") {
			startDate = new Date().toString();
			dateTokens = 3;
		}
		
		if(dateTokens != 0) {
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
		
		if(dateTokens != 0) {
			endDate = toServerDate(new Date(endDate), dateTokens);
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
		
		if(auType.length == 0) {
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
		
		if(!auParentGID || (auParentGID.length < 1)) {
			alert("Please select an encompassing location");
			
			return null;
		}
		
		
		geoJSON.features[0].properties["kml"] = thisMapDriver.kml;
		
		for(i = 0; i < geoJSON.features.length; i++) {
			geoJSON.features[i].properties["name"] = auName;
			geoJSON.features[i].properties["type"] = auType;
			geoJSON.features[i].properties["codes"] = [{"code": auCode, "codeTypeName": auCodeType}];
			geoJSON.features[i].properties["parent"] = auParentGID;
			geoJSON.features[i].properties["startDate"] = startDate;
			geoJSON.features[i].properties["endDate"] = endDate;
			
			geometry = geoJSON.features[i].geometry;
			
			if(geometry.type == "Polygon") {
				geometry.coordinates = [geometry.coordinates];
				geometry.type = "MultiPolygon";
			}
		}
		
		return geoJSON;
	}
	
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
			//indexingObject.informationObject.setURI(indexingObject.successChange(data, status, "added"));
			console.log(data);
			console.log(status);
			setTextValue("#gid", getIDFromURI(response.getResponseHeader("Location")));
			$("#gid").prop("disabled", true);
			$("#new-button").show();
			
			setTextValue("#server-result", "Success. ID: " + $("#gid").val() + " created");
			$("#server-result").css("color", "#008000");
			$("#server-result").show();
			$("#server-result").fadeOut(15000);
		},
		error: function(data, status) {
			//if(data['responseJSON'] && data['responseJSON']['duplicatedUri']) {
			//	indexingObject.duplicateDialog(data['responseJSON']['duplicatedUri']);
			//}
			//else {
			//	indexingObject.successChange(data, status, "error");
			//}
			
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
		properties.code = getValueText("#au-code");
		properties.startDate = getValueText("#start-date");
		properties.endDate = getValueText("#end-date");
		properties.parentGid = this.parent;
		properties.description = properties.name + ";" + properties.code + ";" + properties.startDate + ";" + properties.endDate + ";" + properties.parentGid;
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
		setTextValue("#au-name", properties.name);
		setTextValue("#au-code", properties.code);
		setTextValue("#start-date", properties.startDate);
		setTextValue("#end-date", properties.endDate);
		
		var i;
		var parentGID = properties.parentGid;
		console.log(parentGID);
		
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
			
			latitude = geometry.coordinates[0][0][1];
			longitude = geometry.coordinates[0][0][0];
			minLat = latitude;
			maxLat = minLat;
			minLng = longitude;
			maxLng = minLng;
			
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
