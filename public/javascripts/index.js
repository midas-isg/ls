
var path = ".";
var MAP_DRIVER = null;

$(document).ready(function() {
	MAP_DRIVER = new MapDriver();
	
	$('#upload_button').click(function() {
		MAP_DRIVER.upload();
		
		return;
	});
	
	$('#download_button').click(function() {
		MAP_DRIVER.download();
		
		return;
	});
	
	$('#save_button').click(function() {
		MAP_DRIVER.saveMap();
		
		return;
	});
	
	return;
});

function MapDriver(){
	this.title = '<strong>Pitt</strong>sburgh';
	this.mapID = 'tps23.k1765f0g';
	this.geojsonFile = 'http://localhost:9000/counties'; //"http://tps23-nb.univ.pitt.edu/counties.json"; //'http://localhost/countries.geo.json';
	this.startingCoordinates = [42.004097, -97.019516]; //[44.95167427365481, 582771.4257198056];
	this.zoom = 6;
	this.accessToken = 'pk.eyJ1IjoidHBzMjMiLCJhIjoiVHEzc0tVWSJ9.0oYZqcggp29zNZlCcb2esA';
	this.featureLayer = null;
	this.map = null;
	
	this.initialize();
	
	return;
}

MapDriver.prototype.initialize = function() {
	L.mapbox.accessToken = this.accessToken;
	
	this.map = L.mapbox.map('map-one', 'examples.map-i86l3621', { worldCopyJump: true /*crs: L.CRS.EPSG385*/}).setView(this.startingCoordinates, this.zoom);
	this.map.legendControl.addLegend(this.title);
	
	if(this.geojsonFile) {
		this.featureLayer = L.mapbox.featureLayer().loadURL(this.geojsonFile);
	}
	else if(this.mapID) {
		this.featureLayer = L.mapbox.featureLayer().loadID(this.mapID);
	}
	
	this.featureLayer.on('ready', function() {
		MAP_DRIVER.featureLayer.addTo(MAP_DRIVER.map);
		
		var drawControl = new L.Control.Draw({
			draw: {
				polyline: false,
				rectangle: false,
				circle: false,
				marker: false
			},
			edit: {
				featureGroup: MAP_DRIVER.featureLayer
			}
		}).addTo(MAP_DRIVER.map);
		
		MAP_DRIVER.map.on('draw:created', function(e) {
			MAP_DRIVER.featureLayer.addLayer(e.layer);
			console.log(e);
		});
		
		MAP_DRIVER.map.on('draw:deleted', function(e) {
			var layers = e.layers;
			layers.eachLayer(function(layer) {
				if(MAP_DRIVER.featureLayer.hasLayer(layer._leaflet_id + 1)) {
					console.log(MAP_DRIVER.featureLayer.removeLayer(layer._leaflet_id + 1));
				}
			});
		});
	});
	
	this.featureLayer.on('error', function(err) {
		console.log("Error: " + err['error']['statusText']);
		
		if((MAP_DRIVER.featureLayer.getLayers().length == 0) && MAP_DRIVER.mapID) {
			console.log("Attempting to load via mapID");
			MAP_DRIVER.featureLayer = L.mapbox.featureLayer().loadID(MAP_DRIVER.mapID);
		}
		
		MAP_DRIVER.featureLayer.on('ready', function() {
			MAP_DRIVER.featureLayer.addTo(MAP_DRIVER.map);
			
			var drawControl = new L.Control.Draw({
				edit: {
				  featureGroup: MAP_DRIVER.featureLayer
				}
			}).addTo(MAP_DRIVER.map);
			
			MAP_DRIVER.map.on('draw:created', function(e) {
				MAP_DRIVER.featureLayer.addLayer(e.layer);
				console.log(e);
			});
			
			MAP_DRIVER.map.on('draw:deleted', function(e) {
				var layers = e.layers;
				layers.eachLayer(function(layer) {
					if(MAP_DRIVER.featureLayer.hasLayer(layer._leaflet_id + 1)) {
						console.log(MAP_DRIVER.featureLayer.removeLayer(layer._leaflet_id + 1));
					}
				});
			});
		});
	});
	
	return;
}

MapDriver.prototype.loadJSON = function(jsonData) {
	this.featureLayer.setGeoJSON(jsonData);
	
	return;
}

MapDriver.prototype.saveMap = function() {
	// Create //POST /resources/aus
	var httpType = "POST";
	var URL = path + "/resources/aus";
	
	/*
	if(isUpdate) {
		// Update //PUT /resources/aus
		httpType = "PUT";
	}
	*/
	
	var data = this.featureLayer.toGeoJSON();
	data.id = this.mapID;
	
	function formatGeoJSON(geoJSON) {
		var i;
		var geometry;
		var auName = $("#au-name").val();
		var auCode = $("#au-code").val();
		var startDate = $("#start-date").val();
		var endDate = $("#end-date").val();
		
		if(!validDate(startDate)) {
			alert("Not a valid start date: " + startDate);
			
			return null;
		}
		
		if(auName.length == 0) {
			alert("Please enter the Administrative Unit's name");
			
			return  null;
		}
		
		if(auCode.length == 0) {
			alert("Please enter the Administrative Unit's code");
			
			return null;
		}
		
		if(endDate.length == 0) {
			endDate = null;
		}
		
		for(i = 0; i < geoJSON.features.length; i++) {
			geoJSON.features[i].properties["name"] = auName;
			geoJSON.features[i].properties["code"] = auCode;
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
	
	if(!formatGeoJSON(data)) {
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
		success: function(data, status) {
			//indexingObject.informationObject.setURI(indexingObject.successChange(data, status, "added"));
			console.log(data);
			console.log(status);
		},
		error: function(data, status) {
			//if(data['responseJSON'] && data['responseJSON']['duplicatedUri']) {
			//	indexingObject.duplicateDialog(data['responseJSON']['duplicatedUri']);
			//}
			//else {
			//	indexingObject.successChange(data, status, "error");
			//}
		}
	});
	
	return;
}

MapDriver.prototype.download = function() {
	var jsonData = this.featureLayer.toGeoJSON();
	jsonData.id = this.mapID;
	
	var data = "text/json;charset=utf-8," + encodeURIComponent(JSON.stringify(jsonData));
	
	//$('<a href="data:' + data + '" download="data.json">download JSON</a>').appendTo('#container');
	
	location.assign("data:'" + data);
	
	return jsonData;
}

MapDriver.prototype.upload = function() {
	var file = $('#json-input').get(0).files[0];
	var fileReader = new FileReader();
	
	fileReader.onload = (function() {
		var jsonData = JSON.parse(fileReader['result']);
		
		MAP_DRIVER.loadJSON(jsonData);
	});
	
	var fileString = fileReader.readAsText(file);
	
	return;
}

/* Helper Functions */
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

function dateToServerDate(inputDate, fields) {
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
