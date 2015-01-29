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
				setTextValue("#au-codepath", feature.properties.codePath);
				setTextValue("#start-date", feature.properties.startDate);
				setTextValue("#end-date", feature.properties.endDate);
				//PARENT_TREE.resetIsAboutList();
				//AU_COMPOSITE_TREE.resetIsAboutList();
				
				var i;
				var parentGID = feature.properties.parentGid;
				if(parentGID) {
					//for(i = 0; i < parentGID.length; i++) {
					//	AU_COMPOSITE_TREE.clickIsAboutByValue(parentGID[i]);
					//}
					
					//PARENT_TREE.clickIsAboutByValue(parentGID);
				}
				
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
						polygon: false,
						rectangle: false,
						circle: false,
						marker: true
					},
					edit: {
						featureGroup: thisMapDriver.featureLayer
					}
				}).addTo(thisMapDriver.map);
			}
			
			thisMapDriver.map.on('draw:drawstart', function(e) {
				thisMapDriver.featureLayer.clearLayers();
				console.log(e);
				
				return;
			});
			
			thisMapDriver.map.on('draw:created', function(e) {
				thisMapDriver.featureLayer.addLayer(e.layer);
				searchPoint(e.layer._latlng.lat, e.layer._latlng.lng);
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
	
	$.get(url, function(data, status) {
		treeData = data;
		//console.log(data);
		
		//initialize defined here
		
		/*
		PARENT_TREE.initInteractBetweenTreeAndTable("parent-list", function() {
			AU_COMPOSITE_TREE.initInteractBetweenTreeAndTable("au-list", initialize());
			
			return;
		});
		*/
		//initialize();
		
		return;
	});
	
	return;
});
