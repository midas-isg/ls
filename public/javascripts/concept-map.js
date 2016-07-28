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
				MapDriver.prototype.loadFeatureLayer = function() {
					var noLoad = false;
					var thisMapDriver = this;
					
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
							var feature = geoJSON.features[0];
							
							var minLng = geoJSON.bbox[0];
							var minLat = geoJSON.bbox[1];
							var maxLng = geoJSON.bbox[2];
							var maxLat = geoJSON.bbox[3];
							var southWest = L.latLng(minLat, minLng);
							var northEast = L.latLng(maxLat, maxLng);
							var bounds = L.latLngBounds(southWest, northEast);
							thisMapDriver.map.fitBounds(bounds);
							
							thisMapDriver.mapID = thisMapDriver.featureLayer.getGeoJSON().id;
							HELPERS.setTextValue("#au-name", feature.properties.name);
							HELPERS.setTextValue("#au-code", feature.properties.code);
							HELPERS.setTextValue("#start-date", feature.properties.startDate);
							HELPERS.setTextValue("#end-date", feature.properties.endDate);
							PARENT_TREE.resetIsAboutList();
							AU_COMPOSITE_TREE.resetIsAboutList();
							
							var parentGID = feature.properties.parentGid;
							if(parentGID) {
								//var i;
								//for(i = 0; i < parentGID.length; i++) {
								//	AU_COMPOSITE_TREE.clickIsAboutByValue(parentGID[i]);
								//}
								
								PARENT_TREE.clickIsAboutByValue(parentGID);
							}

							HELPERS.setTextValue("#gid", feature.properties.gid);
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
				var thisMapDriver = CONCEPT_MAP;
				
				function loadFromDatabase(mapID) {
					thisMapDriver.geoJSONURL = crudPath + "/" + mapID;
					
					if(thisMapDriver.geoJSONURL) {
						thisMapDriver.featureLayer.loadURL(thisMapDriver.geoJSONURL);
					}
					
					thisMapDriver.featureLayer.on("ready", function() {
						var feature = thisMapDriver.featureLayer.getGeoJSON().features[0];
						
						thisMapDriver.kml = feature.properties.kml;
						
						$("#gid").prop("disabled", true);
						HELPERS.setTextValue("#au-type", feature.properties.locationTypeName);
						
						PARENT_TREE.clickIsAboutByValue(feature.properties.parentGid);
						HELPERS.setTextValue("input#parent", HELPERS.getFirstAlphaOnly(PARENT_TREE.tree.getNodeByKey(feature.properties.parentGid).title));
						$("input#parent").keyup();
						
						$("#save-button").hide();
						if(feature.properties.locationTypeName == "Epidemic Zone") {
							$("#update-button").show();
						}
						
						$("#new-button").show();
					});
					
					return;
				}
				var id = HELPERS.getURLParameterByName("id");
				if(id) {
					loadFromDatabase(id);
				}
				
				$("#new-button").click(function() {
					CONCEPT_MAP.mapID = Date().valueOf();
					CONCEPT_MAP.featureLayer.clearLayers();
					HELPERS.setTextValue("#au-name", "");
					HELPERS.setTextValue("#au-code", "");
					HELPERS.setTextValue("#au-codepath", "");
					
					var today = new Date();
					HELPERS.setTextValue("#start-date", today.getUTCFullYear() + "-" + (today.getUTCMonth() + 1) + "-" + today.getUTCDate());
					HELPERS.setTextValue("#end-date", "");
				});
				
				$("#delete-button").click(function() {
					CONCEPT_MAP.deleteLocation();
					
					return;
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
					var mapID = HELPERS.getValueText("#gid");
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
					
					var i,
					currentAUGID,
					currentAU,
					compositeJSON = {};
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
