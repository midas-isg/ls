var SEARCH_MAP = null;
var MAP_DRIVER = null;

$(document).ready(function() {
	var url = context + "/api/au-tree";
	
	//override
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
				setTextValue("#description", feature.properties.description);
				setTextValue("#start-date", feature.properties.startDate);
				setTextValue("#end-date", feature.properties.endDate);
				
				var parentGID = feature.properties.parentGid;
				
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
				// Set the button title text for buttons
				L.drawLocal.draw.toolbar.buttons.polygon = "Search within user-defined area";
				L.drawLocal.draw.toolbar.buttons.marker = "Search via a point";
				
				// Set the tooltip start text when drawing
				L.drawLocal.draw.handlers.polygon.tooltip.start = "Click to define search area";
				L.drawLocal.draw.handlers.polygon.tooltip.cont = "Click to continue defining search area";
				L.drawLocal.draw.handlers.polygon.tooltip.end = "Click to continue defining search area <br> or click first point to finish";
				L.drawLocal.draw.handlers.marker.tooltip.start = "Click to place search point";
				
				thisMapDriver.drawControl = new L.Control.Draw({
					draw: {
						polyline: false,
						polygon: true,
						rectangle: false,
						circle: false,
						marker: true
					},
					edit: false /*{
						featureGroup: thisMapDriver.featureLayer
					}*/
				}).addTo(thisMapDriver.map);
			}
			
			thisMapDriver.map.on('draw:drawstart', function(e) {
				thisMapDriver.featureLayer.clearLayers();
				console.log(e);
				
				return;
			});
			
			thisMapDriver.map.on('draw:created', function(e) {
				thisMapDriver.featureLayer.addLayer(e.layer);
				var geoJSON = e.layer.toGeoJSON();
				
				if(geoJSON.geometry.type === "Point") {
					SEARCH_RESULTS.searchPoint(e.layer._latlng.lat, e.layer._latlng.lng);
				}
				else {
					geoJSON = { features:[geoJSON], type: "FeatureCollection" };
					SEARCH_RESULTS.searchByGeoJSON(geoJSON);
				}
				
				console.log(e);
				
				return;
			});
			
			thisMapDriver.map.on('draw:deleted', function(e) {
				var layers = e.layers;
				layers.eachLayer(function(layer) {
					if(thisMapDriver.featureLayer.hasLayer(layer._leaflet_id + 1)) {
						console.log(thisMapDriver.featureLayer.removeLayer(layer._leaflet_id + 1));
					}
					
					return;
				});
				
				return;
			});
			
			thisMapDriver.map.whenReady(function() {
				return thisMapDriver.map.setZoom(1);
			});
			
			return;
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
	
	function initialize() {
		SEARCH_MAP = new MapDriver();
		MAP_DRIVER = SEARCH_MAP;
		
		return;
	}
	initialize();
	
	return;
});
