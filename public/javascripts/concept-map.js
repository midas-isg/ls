var crudPath = context + '/resources/aus';
var CONCEPT_MAP = null;
var MAP_DRIVER = null;

$(document).ready(function() {
	var url = ausPath + "/api/au-tree";
	
	$.get(url, function(data, status) {
		treeData = data;
		//console.log(data);
		
		PARENT_TREE.initInteractBetweenTreeAndTable("parent-list", function() {
			AU_COMPOSITE_TREE.initInteractBetweenTreeAndTable("au-list", initialize());
			
			function initialize() {
				MapDriver.loadFeatureLayer = function() {
					var thisMapDriver = this;
					
					if(this.geoJSONURL) {
						this.featureLayer = L.mapbox.featureLayer().loadURL(this.geoJSONURL);
					}
					else if(this.mapID) {
						this.featureLayer = L.mapbox.featureLayer().loadID(this.mapID);
					}
					
					this.featureLayer.on('ready', function() {
						thisMapDriver.loadJSON(thisMapDriver.featureLayer.getGeoJSON());
						
						thisMapDriver.featureLayer.addTo(thisMapDriver.map);
						
						var feature = thisMapDriver.featureLayer.getGeoJSON().features[0];
						
						centerMap(thisMapDriver.featureLayer.getGeoJSON(), thisMapDriver);
						
						thisMapDriver.mapID = thisMapDriver.featureLayer.getGeoJSON().id;
						setTextValue("#au-name", feature.properties.name);
						setTextValue("#au-code", feature.properties.code);
						setTextValue("#au-codepath", feature.properties.codePath);
						setTextValue("#start-date", feature.properties.startDate);
						setTextValue("#end-date", feature.properties.endDate);
						PARENT_TREE.resetIsAboutList();
						AU_COMPOSITE_TREE.resetIsAboutList();
						
						var i;
						var parentGID = feature.properties.parentGid;
						if(parentGID) {
							//for(i = 0; i < parentGID.length; i++) {
							//	AU_COMPOSITE_TREE.clickIsAboutByValue(parentGID[i]);
							//}
							
							PARENT_TREE.clickIsAboutByValue(parentGID);
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
					
					return;
				}
				
				CONCEPT_MAP = new MapDriver();
				MAP_DRIVER = CONCEPT_MAP;
				
				$("#new-button").click(function() {
					CONCEPT_MAP.mapID = Date().valueOf();
					CONCEPT_MAP.featureLayer.clearLayers();
					setTextValue("#au-name", "");
					setTextValue("#au-code", "");
					setTextValue("#au-codepath", "");
					
					var today = new Date();
					setTextValue("#start-date", today.getUTCFullYear() + "-" + (today.getUTCMonth() + 1) + "-" + today.getUTCDate());
					setTextValue("#end-date", "");
				});
				
				$("#upload-button").click(function() {
					CONCEPT_MAP.upload();
					
					return;
				});
				
				$("#download-button").click(function() {
					CONCEPT_MAP.download();
					
					return;
				});
				
				$("#db-load-button").click(function() {
					var mapID = getValueText("#gid");
					CONCEPT_MAP.geoJSONURL = crudPath + "/" + mapID;
					//"http://tps23-nb.univ.pitt.edu/test.json";
					
					if(CONCEPT_MAP.geoJSONURL) {
						CONCEPT_MAP.featureLayer.loadURL(CONCEPT_MAP.geoJSONURL);
						//CONCEPT_MAP.loadFeatureLayer();
					}
					
					return;
				});
				
				$("#save-button").click(function() {
					CONCEPT_MAP.saveMap();
					
					return;
				});
				
				$("#composite-button").click(function() {
					console.log(CONCEPT_MAP.getAUComponents());
					
					var i;
					var currentAUGID;
					var currentAU;
					
					var compositeJSON = {};
					compositeJSON.type = "FeatureCollection";
					compositeJSON.id = null;
					compositeJSON.features = [];
					
					currentAUGID = CONCEPT_MAP.auComponents[0];
					currentAU = L.mapbox.featureLayer().loadURL(crudPath + "/" + currentAUGID);
					//currentAU.on('ready', function(){
						//TODO: Load JSON via call-back
						for(i = 1; i < CONCEPT_MAP.auComponents.length; i++) {
							currentAUGID = CONCEPT_MAP.auComponents[i];
							currentAU = L.mapbox.featureLayer().loadURL(crudPath + "/" + currentAUGID);
							
							console.log(currentAU);
							var j;
							for(j = 0; j < currentAU.geojson.features.length; j++) {
								compositeJSON.features.push(currentAU.geojson.features[j]);
							}
						}
						
						CONCEPT_MAP.loadJSON(compositeJSON);
					//});
				});
				
				return;
			}
		});
		
		return;
	});
	
	return;
});