
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
	this.geojsonFile = "http://tps23-nb.univ.pitt.edu/counties.json"; //'http://localhost:9000/counties'; //'http://localhost/countries.geo.json';
	this.featureLayerObject = null;
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
			edit: {
			  featureGroup: MAP_DRIVER.featureLayer
			}
		}).addTo(MAP_DRIVER.map);
		
		MAP_DRIVER.map.on('draw:created', function(e) {
			MAP_DRIVER.featureLayer.addLayer(e.layer);
console.log(e);
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
console.log(e);
				MAP_DRIVER.featureLayer.addLayer(e.layer);
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
	
	var data = this.featureLayer.getGeoJSON();
	/*
	data = {
		type: 'FeatureCollection',
		features: [
			{
				geometry: [
					12,
					54
				],
				properties: 'props'
			},
			{
				geometry: [
					42,
					28
				],
				properties: 'props'
			},
			{
				geometry: [
					33,
					99
				],
				properties: 'props'
			}
		]
	}
	*/
	
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
	jsonData['id'] = this.mapID;
	
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
