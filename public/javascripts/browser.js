var crudPath = context + "/resources/aus";
var apolloJSONDataPath = context + "/api/locations/";
var MAP_DRIVER = null;

$(document).ready(function() {
	MAP_DRIVER = new MapDriver();
	
	return;
});

function MapDriver(){
	var id = getURLParameterByName("id");
	var format = getURLParameterByName("format");
	this.title = "";
	this.mapID = id; //'tps23.k1765f0g';
	
	this.dataSourceURL = context + "/api/locations/" + id;
	this.geoJSONURL = this.dataSourceURL + "?format=geojson"; //crudPath + "/" + id;
	this.apolloJSONURL = this.dataSourceURL + "?format=apollojson";
	this.kmlURL = this.dataSourceURL + "?format=kml";
	
	this.accessToken = 'pk.eyJ1IjoidHBzMjMiLCJhIjoiVHEzc0tVWSJ9.0oYZqcggp29zNZlCcb2esA';
	this.featureLayer = null;
	this.map = null;
	
	switch(format) {
		case "apollojson":
		case "ApolloJSON":
			this.getJSONData(this.apolloJSONURL);
		break;
		
		case "GeoJSON":
		case "geojson":
			this.getJSONData(this.geoJSONURL);
		break;
		
		case "KML":
		case "kml":
			this.getKMLData();
		break;
		
		case "banana":
			location.assign("https://www.youtube.com/watch?v=ex0URF-hWj4");
		break;
		
		default:
			this.initialize();
		break;
	}
	
	return;
}

MapDriver.prototype.initialize = function() {
	$("#header-data").show();
	
	L.mapbox.accessToken = this.accessToken;
	
	this.map = L.mapbox.map('map-data', 'examples.map-i86l3621', { worldCopyJump: true /*crs: L.CRS.EPSG385*/});
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
		setTextValue("#start-date", feature.properties.startDate);
		setTextValue("#end-date", feature.properties.endDate);
		
		if(feature.properties.endDate) {
			$("#historical-note").show();
		}
		
		$("#au-geojson").prop("href", MAP_DRIVER.geoJSONURL);
		if(MAP_DRIVER.geoJSONURL) {
			$("#au-geojson").css("text-decoration", "underline");
		}
		
		$("#au-kml").prop("href", MAP_DRIVER.kmlURL);
		if(MAP_DRIVER.kmlURL) {
			$("#au-kml").css("text-decoration", "underline");
		}
		
		$("#au-apollojson").prop("href", MAP_DRIVER.apolloJSONURL);
		if(MAP_DRIVER.apolloJSONURL) {
			$("#au-apollojson").css("text-decoration", "underline");
		}
		
		var lineage = feature.properties.lineage;
		var i;
		var auName;
		var auGID;
		if(lineage.length > 0) {
			$("#au-lineage").show();
			
			for(i = (lineage.length - 1); i >= 0; i--) {
				auName = lineage[i].name;
				auGID = lineage[i].gid;
				
				$("#au-lineage").append("<a href='./browser?id=" + auGID + "' class='pre-spaced' style='text-decoration: underline;' title='ID: "+ auGID +"'>" + auName + "</a>");
				
				if(i > 0){
					$("#au-lineage").append(",");
				}
			}
		}
		
		var children = feature.properties.children;
		if(children.length > 0) {
			console.log(children);
			$("#au-children").show();
			
			auName = children[0].name;
			auGID = children[0].gid;
			$("#au-children").append("<a href='./browser?id=" + auGID + "' class='pre-spaced' style='text-decoration: underline;' title='ID: "+ auGID +"'>" + auName + "</a>; ");
			
			for(i = 1; i < children.length; i++) {
				auName = children[i].name;
				auGID = children[i].gid;
				
				$("#au-children").append("<a href='./browser?id=" + auGID + "' style='text-decoration: underline;' title='ID: "+ auGID +"'>" + auName + "</a>");
				
				if(i < (children.length - 1)) {
					$("#au-children").append("; ");
				}
			}
		}
		
		setTextValue("#gid", feature.properties.gid);
		feature.properties.title = feature.properties.name + "; " + feature.properties.startDate;
		
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
		properties.startDate = getValueText("#start-date");
		properties.endDate = getValueText("#end-date");
		
		properties.code = getValueText("#au-geojson");
		
		properties.parentGid = getValueText("#au-lineage");
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
		setTextValue("#start-date", properties.startDate);
		setTextValue("#end-date", properties.endDate);
		
		setTextValue("#au-geojson", properties.code);
		
		//setTextValue("#au-lineage", properties.parentGid);
		
		MAP_DRIVER.loadJSON(jsonData);
	});
	
	var fileString = fileReader.readAsText(file);
	
	return;
}

MapDriver.prototype.getJSONData = function(URL) {
	/*
	$.get(URL, function(data, status) {
		$("#map-data").text(JSON.stringify(data));
	});
	*/
	
	window.location.assign(URL);
	
	return;
}

MapDriver.prototype.getKMLData = function() {
	
	
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
