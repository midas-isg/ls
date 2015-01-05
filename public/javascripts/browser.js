//!!!!!!!!!TODO: use http://localhost:9000/ls/api/locations/1169 for back information!!!!!!!!!

var crudPath = context + '/resources/aus';
var MAP_DRIVER = null;

$(document).ready(function() {
	MAP_DRIVER = new MapDriver();
	
	$("#new-button").click(function() {
		MAP_DRIVER.mapID = Date().valueOf();
		MAP_DRIVER.featureLayer.clearLayers();
		setTextValue("#au-name", "");
		setTextValue("#au-code", "");
		setTextValue("#au-codepath", "");
		
		var today = new Date();
		setTextValue("#start-date", today.getUTCFullYear() + "-" + (today.getUTCMonth() + 1) + "-" + today.getUTCDate());
		setTextValue("#end-date", "");
		setTextValue("#au-parent", "");
	});
	
	$('#upload-button').click(function() {
		MAP_DRIVER.upload();
		
		return;
	});
	
	$('#download-button').click(function() {
		MAP_DRIVER.download();
		
		return;
	});
	
	$('#db-load-button').click(function() {
		var mapID = getValueText("#gid");
		MAP_DRIVER.geoJSONURL = crudPath + "/" + mapID;
		//"http://tps23-nb.univ.pitt.edu/test.json";
		
		if(MAP_DRIVER.geoJSONURL) {
			MAP_DRIVER.featureLayer.loadURL(MAP_DRIVER.geoJSONURL);
			//MAP_DRIVER.loadFeatureLayer();
		}
		
		return;
	});
	
	$('#save-button').click(function() {
		MAP_DRIVER.saveMap();
		
		return;
	});
	
	//INDEXING_TERMS_TREE.initInteractBetweenTreeAndTable("picklist");
	
	return;
});

function MapDriver(){
	var id = getURLParameterByName("id");
	this.title = "<strong>Sierra Leone</strong> 0001-01-01 to now";
	this.mapID = id;//'tps23.k1765f0g';
	this.geoJSONURL = crudPath + "/" + id;
	//"http://tps23-nb.univ.pitt.edu/test.json";
	this.startingCoordinates = [6.944028854370401, -11.534582138061467];
	this.zoom = 5;
	this.accessToken = 'pk.eyJ1IjoidHBzMjMiLCJhIjoiVHEzc0tVWSJ9.0oYZqcggp29zNZlCcb2esA';
	this.featureLayer = null;
	this.map = null;
	this.parents = [];
	
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
	if(this.geoJSONURL) {
		this.featureLayer = L.mapbox.featureLayer().loadURL(this.geoJSONURL);
	}
	else if(this.mapID) {
		this.featureLayer = L.mapbox.featureLayer().loadID(this.mapID);
	}
	
	this.featureLayer.on('ready', function() {
		MAP_DRIVER.loadJSON(MAP_DRIVER.featureLayer.getGeoJSON());
		
		MAP_DRIVER.featureLayer.addTo(MAP_DRIVER.map);
		
		var feature = MAP_DRIVER.featureLayer.getGeoJSON().features[0];
		
		function centerMap(geoJSON) {
			var geometry = geoJSON.features[0].geometry;
			
			if(!geometry) {
				return;
			}
			
			var geometryCount = geometry.coordinates.length;
			var latitude = geometry.coordinates[0][0][1];
			var longitude = geometry.coordinates[0][0][0];
			var minLat = latitude;
			var maxLat = minLat;
			var minLng = longitude;
			var maxLng = minLng;
			
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
			
			var southWest = L.latLng(minLat, minLng);
			var northEast = L.latLng(maxLat, maxLng);
			var bounds = L.latLngBounds(southWest, northEast);
			
			return MAP_DRIVER.map.fitBounds(bounds);
		}
		
		centerMap(MAP_DRIVER.featureLayer.getGeoJSON());
		
		MAP_DRIVER.mapID = MAP_DRIVER.featureLayer.getGeoJSON().id;
		setTextValue("#au-name", feature.properties.name);
		setTextValue("#au-code", feature.properties.code);
		setTextValue("#au-codepath", feature.properties.codePath);
		setTextValue("#start-date", feature.properties.startDate);
		setTextValue("#end-date", feature.properties.endDate);
		
		if(feature.properties.parentGid) {
			$("#au-parent").prop("href", "./browser?id=" + feature.properties.parentGid);
			$("#au-parent").css("text-decoration", "underline");
			setTextValue("#au-parent", feature.properties.parentGid);
		}
		else {
			$("#au-parent").prop("href", "");
			setTextValue("#au-parent", "");
		}
		
		setTextValue("#gid", feature.properties.gid);
		feature.properties.title = feature.properties.name + " [" + feature.properties.codePath + "] " + "; " + feature.properties.startDate;
		
		if(feature.properties.endDate) {
			feature.properties.title = feature.properties.title + " to " + feature.properties.endDate;
		}
		else {
			feature.properties.title += " to present";
		}
		
		MAP_DRIVER.map.legendControl.removeLegend(MAP_DRIVER.title);
		MAP_DRIVER.title = "<strong>" + feature.properties.title + "</strong>";
		MAP_DRIVER.map.legendControl.addLegend(MAP_DRIVER.title);
	});
	
	this.featureLayer.on('error', function(err) {
		console.log("Error: " + err['error']['statusText']);
		
		if((MAP_DRIVER.featureLayer.getLayers().length == 0) && MAP_DRIVER.mapID) {
			console.log("Attempting to load via mapbox ID");
			MAP_DRIVER.featureLayer = L.mapbox.featureLayer().loadID(MAP_DRIVER.mapID);
		}
		
		MAP_DRIVER.featureLayer.on('ready', function() {
			MAP_DRIVER.featureLayer.addTo(MAP_DRIVER.map);
		});
	});
	
	return;
}

MapDriver.prototype.loadJSON = function(jsonData) {
	multiPolygonsToPolygons(jsonData);
	this.featureLayer.setGeoJSON(jsonData);
	
	return;
}

MapDriver.prototype.download = function() {
	var jsonData = this.featureLayer.toGeoJSON();
	var properties = null;
	
	for(var i = 0; i < jsonData.features.length; i++) {
		properties = jsonData.features[i].properties;
		properties.name = getValueText("#au-name");
		properties.code = getValueText("#au-code");
		properties.codePath = getValueText("#au-codepath");
		properties.startDate = getValueText("#start-date");
		properties.endDate = getValueText("#end-date");
		properties.parentGid = getValueText("#au-parent");
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
	var file = $('#json-input').get(0).files[0];
	var fileReader = new FileReader();
	
	fileReader.onload = (function() {
		var kmlData = fileReader['result'];
		var kmlDOM = (new DOMParser()).parseFromString(kmlData, 'text/xml');
		
		var jsonData = toGeoJSON.kml(kmlDOM);
		
		if(jsonData.features.length == 0) {
			jsonData = JSON.parse(kmlData);
		}
		
		var properties = jsonData.features[0].properties;
		setTextValue("#au-name", properties.name);
		setTextValue("#au-code", properties.code);
		setTextValue("#au-codepath", properties.codePath);
		setTextValue("#start-date", properties.startDate);
		setTextValue("#end-date", properties.endDate);
		setTextValue("#au-parent", properties.parentGid);
		
		MAP_DRIVER.loadJSON(jsonData);
	});
	
	var fileString = fileReader.readAsText(file);
	
	return;
}

/* Helper Functions */
function getIDFromURI(URI) {
	var components = URI.split('/');
	var id = components[components.length - 1];
	
	return id;
}

function multiPolygonsToPolygons(geoJSON) {
	var features = geoJSON.features;
	var count = features.length;
	
	for(var i = 0; i < count; i++) {
		if(features[i].geometry.type == "MultiPolygon") {
			var properties = features[i].properties;
			properties.description = properties.name; //+ ";" + properties.code + ";" + properties.startDate + ";" + properties.endDate + ";" + properties.parentGid;
			
			for(var j = 0; j < features[i].geometry.coordinates.length; j++) {
				features.push({"type": "Feature", "geometry": {"type": "Polygon", "coordinates": null}, "properties": properties});
				var addedFeature = features[features.length - 1];
				addedFeature.geometry.coordinates = features[i].geometry.coordinates[j];
			}
			
			features.splice(i, 1);
			i--;
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
