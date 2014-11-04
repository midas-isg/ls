var MAP_DRIVER = null;

$(document).ready(function() {
	MAP_DRIVER = new MapDriver();
	
	$('#download_button').click(function() {
		MAP_DRIVER.download();
		
		return;
	});
	
	return;
});

function MapDriver(){
	this.title = '<strong>Pitt</strong>sburgh';
	this.mapID = 'tps23.k1765f0g';
	this.geojsonFile = 'http://localhost:9000/counties'; //'http://localhost/countries.geo.json';
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
			});
		});
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