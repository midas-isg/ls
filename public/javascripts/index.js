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
	this.geojsonFile = 'http://tps23-nb.univ.pitt.edu/pittsburgh.json'; //'http://localhost:9000/leaflet/nyc'; //'http://localhost/countries.geo.json';
	this.featureLayerObject = null;
	this.startingCoordinates = [40.45826240784896, -79.93491411209106]; //[44.95167427365481, 582771.4257198056];
	this.zoom = 12;
	this.accessToken = 'pk.eyJ1IjoidHBzMjMiLCJhIjoiVHEzc0tVWSJ9.0oYZqcggp29zNZlCcb2esA';
	this.featureLayer = null;
	
	this.initialize();
	
	return;
}

MapDriver.prototype.initialize = function() {
	L.mapbox.accessToken = this.accessToken;
	
	var map = L.mapbox.map('map-one', 'examples.map-i86l3621', { worldCopyJump: true /*crs: L.CRS.EPSG385*/}).setView(this.startingCoordinates, this.zoom);
	map.legendControl.addLegend(this.title);
	
	this.featureLayer = L.mapbox.featureLayer().addTo(map);
	
	if(this.geojsonFile) {
		this.featureLayerObject = this.featureLayer.loadURL(this.geojsonFile);
	}
	
	if(!this.featureLayerObject && this.mapID) {
		console.log("couldn't load json; attempting to load via map ID");
		this.featureLayer.loadID(this.mapID);
	}
	
	var drawControl = new L.Control.Draw({
		edit: {
		  featureGroup: this.featureLayer
		}
	}).addTo(map);
	
	map.on('draw:created', function(e) {
		MAP_DRIVER.featureLayer.addLayer(e.layer);
	});
	
	return;
}

MapDriver.prototype.download = function() {
	var jsonData = this.featureLayer.getGeoJSON();
	
	var data = "text/json;charset=utf-8," + encodeURIComponent(JSON.stringify(jsonData));
	
	//$('<a href="data:' + data + '" download="data.json">download JSON</a>').appendTo('#container');
	
	location.assign("data:'" + data + "'");
	
	return jsonData;
}