var MAP_DRIVER;

$(document).ready(function() {
	var url = ausPath + "/api/au-tree";
	
	$.get(url, function(data, status) {
		treeData = data;
		//console.log(data);
		
		PARENT_TREE.initInteractBetweenTreeAndTable("parent-list", function() {
			AU_COMPOSITE_TREE.initInteractBetweenTreeAndTable("au-list", initialize());
			
			function initialize() {
				var tildeKey = false,
					altKey = false;

				MapDriver.prototype.loadFeatureLayer = function() {
					var noLoad = false,
						thisMapDriver = this;
					
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
							var feature = geoJSON.features[0],
								minLng = geoJSON.bbox[0],
								minLat = geoJSON.bbox[1],
								maxLng = geoJSON.bbox[2],
								maxLat = geoJSON.bbox[3],
								southWest = L.latLng(minLat, minLng),
								northEast = L.latLng(maxLat, maxLng),
								bounds = L.latLngBounds(southWest, northEast),
								parentGID;

							thisMapDriver.map.fitBounds(bounds);
							thisMapDriver.mapID = thisMapDriver.featureLayer.getGeoJSON().id;
							setTextValue("#au-name", feature.properties.name);
							setTextValue("#au-code", feature.properties.code);
							setTextValue("#start-date", feature.properties.startDate);
							setTextValue("#end-date", feature.properties.endDate);
							PARENT_TREE.resetIsAboutList();
							AU_COMPOSITE_TREE.resetIsAboutList();
							
							parentGID = feature.properties.parentGid;
							if(parentGID) {
								//for(var i = 0; i < parentGID.length; i++) {
								//	AU_COMPOSITE_TREE.clickIsAboutByValue(parentGID[i]);
								//}
								
								PARENT_TREE.clickIsAboutByValue(parentGID);
							}
							
							setTextValue("#gid", feature.properties.gid);
							setTextValue("#description", feature.properties.locationDescription);
							
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
						
						thisMapDriver.drawControl = null;
						/*
						if(!thisMapDriver.drawControl) {
							thisMapDriver.drawControl = new L.Control.Draw({
								draw: {
									polyline: false,
									rectangle: false,
									circle: false,
									marker: false
								},
								edit: {
								//	featureGroup: thisMapDriver.featureLayer
								}
							}).addTo(thisMapDriver.map);
						}
						
						thisMapDriver.map.on('draw:created', function(e) {
							thisMapDriver.featureLayer.addLayer(e.layer);
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
						*/
						
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
							
							thisMapDriver.drawControl = null;
							/*
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
							*/
							
							return;
						});
						
						return;
					});
					
					if(noLoad) {
						this.featureLayer.fireEvent("ready");
					}
					
					return;
				}

				MAP_DRIVER = new MapDriver();

				MAP_DRIVER.map.whenReady(function() {
					return MAP_DRIVER.map.setZoom(1, {minZoom: 1});
				});
				
				function loadFromDatabase(mapID) {
					MAP_DRIVER.geoJSONURL = crudPath + "/" + mapID;
					
					if(MAP_DRIVER.geoJSONURL) {
						MAP_DRIVER.featureLayer.loadURL(MAP_DRIVER.geoJSONURL);
					}
					
					MAP_DRIVER.featureLayer.on("ready", function() {
						var feature = MAP_DRIVER.featureLayer.getGeoJSON().features[0],
							IDs;
						
						MAP_DRIVER.kml = feature.properties.kml;
						
						$("#gid").prop("disabled", true);
						setTextValue("#au-type", feature.properties.locationTypeName);
						
						if(feature.properties.parentGid) {
							PARENT_TREE.clickIsAboutByValue(feature.properties.parentGid);
							IDs = [feature.properties.parentGid];
							PARENT_TREE.tree.filterNodes(function(node) {
								var set = new Set(IDs);
								
								return set.has(node.key);
							});
							PARENT_TREE.tree.visit(function(node) {
								node.setExpanded(true);
							});
						}
						
						$("#save-button").hide();
						if(feature.properties.locationTypeName == "Epidemic Zone") {
							$("#update-button").show();
						}
						
						$("#new-button").show();
					});
					
					return;
				}

				id = getURLParameterByName("id");
				if(id) {
					loadFromDatabase(id);
				}
				
				$("#new-button").click(function() {
					return location.assign(context + "/create");
				});
				
				$("#file-input").change(function() {
					MAP_DRIVER.upload();
					
					return;
				});
				
				$("#suggestion-button").click(function() {
					MAP_DRIVER.populateSuggestions();
					
					return;
				});
				
				$("#download-button").click(function() {
					MAP_DRIVER.download();
					
					return;
				});
				
				$("#save-button").click(function() {
					MAP_DRIVER.saveMap();
					
					return;
				});
				
				$("#update-button").click(function() {
					MAP_DRIVER.updateMap();
					
					return;
				});
				
				$("#composite-button").click(function() {
					console.log(MAP_DRIVER.getAUComponents());
					
					var i,
						currentAUGID,
						currentAU,
						compositeJSON = {},
						j;

					compositeJSON.type = "FeatureCollection";
					compositeJSON.id = null;
					compositeJSON.features = [];
					
					currentAUGID = MAP_DRIVER.auComponents[0];
					currentAU = L.mapbox.featureLayer().loadURL(crudPath + "/" + currentAUGID);
					//currentAU.on('ready', function(){
						//TODO: Load JSON via call-back
						for(i = 1; i < MAP_DRIVER.auComponents.length; i++) {
							currentAUGID = MAP_DRIVER.auComponents[i];
							currentAU = L.mapbox.featureLayer().loadURL(crudPath + "/" + currentAUGID);
							
							console.log(currentAU);

							for(j = 0; j < currentAU.geojson.features.length; j++) {
								compositeJSON.features.push(currentAU.geojson.features[j]);
							}
						}
						
						MAP_DRIVER.loadJSON(compositeJSON);
					//});
				});
				
				$(document).keydown(function(event) {
					//console.log("Key: " + event.which);
					
					if(event.which == 192) {
						tildeKey = true;
					}
					
					if(event.which == 18) {
						altKey = true;
					}
					
					if(tildeKey && altKey) {
						$("#au-create").show();
					}
					
					return;
				});
				
				$(document).keyup(function(event) {
					if(event.which == 192) {
						tildeKey = false;
					}
					
					if(event.which == 18) {
						altKey = false;
					}
					
					return;
				});
				
				return;
			}
			
			return;
		});
		
		return;
	});
	
	return;
});
