var MAP_DRIVER = null;

$(document).ready(function() {
	MAP_DRIVER = new MapDriver();
	
	return;
});

function MapDriver(){
	this.title = '<strong>Pitt</strong>sburgh';
	this.mapID = 'tps23.k1765f0g';
	this.geojsonFile = 'http://tps23-nb.univ.pitt.edu/pittsburgh.json';
	this.geojson = null;
	this.startingCoordinates = [40.45826240784896, -79.93491411209106];
	this.zoom = 14;
	this.accessToken = 'pk.eyJ1IjoidHBzMjMiLCJhIjoiVHEzc0tVWSJ9.0oYZqcggp29zNZlCcb2esA';
	
	this.initialize();
	
	return;
}

MapDriver.prototype.initialize = function() {
	L.mapbox.accessToken = this.accessToken;
	
	var map = L.mapbox.map('map-one', 'examples.map-i86l3621').setView(this.startingCoordinates, this.zoom);
	map.legendControl.addLegend(this.title);
	
	var featureLayer = L.mapbox.featureLayer().addTo(map);
	
	if(this.geojsonFile) {
		this.geojson = featureLayer.loadURL(this.geojsonFile);
	}
	
	if(!this.geojson && this.mapID) {
		console.log("couldn't load json; attempting to load via map ID");
		featureLayer.loadID(this.mapID);
	}
	
	var featureGroup = L.featureGroup().addTo(map);
	
	var drawControl = new L.Control.Draw({
		edit: {
		  featureGroup: featureGroup
		}
	}).addTo(map);
	
	map.on('draw:created', function(e) {
		featureGroup.addLayer(e.layer);
	});
	
	return;
}
