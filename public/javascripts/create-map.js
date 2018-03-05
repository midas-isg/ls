var MAP_DRIVER;

$(document).ready(function() {
	var url = ausPath + "/api/au-tree";
	
	(function setup() {
		var tildeKey = false,
			altKey = false;
		
		MAP_DRIVER = new MapDriver();

		MAP_DRIVER.map.whenReady(function() {
			return MAP_DRIVER.map.setZoom(1.6, {minZoom: 1.6});
		});
		
		$("#file-input").change(function() {
			$("#file-name").val(this.value);
			
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
				$("#code-types").show();
				$("#update-button").show();
				$("#download-button").show();
				
				// are we still using this one...?
				$("#au-list").show();
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
		
		$("#new-button").click(function() {
			return location.assign(CONTEXT + "/create");
		});
		
		return;
	})();
	
	$.get(url, function(data, status) {
		treeData = data;
		//console.log(data);
		
		PARENT_TREE.initInteractBetweenTreeAndTable("parent-list", function() {
			AU_COMPOSITE_TREE.initInteractBetweenTreeAndTable("au-list", initialize());
			
			function initialize() {
				var id;
				
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
							HELPERS.setTextValue("#au-name", feature.properties.name);
							HELPERS.setTextValue("#au-code", feature.properties.code);
							HELPERS.setTextValue("#start-date", feature.properties.startDate);
							HELPERS.setTextValue("#end-date", feature.properties.endDate);
							PARENT_TREE.resetIsAboutList();
							AU_COMPOSITE_TREE.resetIsAboutList();
							
							parentGID = feature.properties.parentGid;
							if(parentGID) {
								//for(var i = 0; i < parentGID.length; i++) {
								//	AU_COMPOSITE_TREE.clickIsAboutByValue(parentGID[i]);
								//}
								
								PARENT_TREE.clickIsAboutByValue(parentGID);
							}

							HELPERS.setTextValue("#gid", feature.properties.gid);
							HELPERS.setTextValue("#description", feature.properties.locationDescription);
							
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
				
				function loadFromDatabase(mapID) {
					MAP_DRIVER.geoJSONURL = CRUD_PATH + "/" + mapID;
					
					if(MAP_DRIVER.geoJSONURL) {
						MAP_DRIVER.featureLayer.loadURL(MAP_DRIVER.geoJSONURL);
					}
					
					MAP_DRIVER.featureLayer.on("ready", function() {
						var feature = MAP_DRIVER.featureLayer.getGeoJSON().features[0],
							IDs;
						
						MAP_DRIVER.kml = feature.properties.kml;
						
						$("#gid").prop("disabled", true);
						HELPERS.setTextValue("#au-type", feature.properties.locationTypeName);
						
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
						$("#save-button").addClass("hidden");
						if(feature.properties.locationTypeName == "Epidemic Zone") {
							$("#update-button").show();
							$("#update-button").removeClass("hidden");
						}
						
						$("#new-button").show();
						$("#new-button").removeClass("hidden");
					});
					
					return;
				}

				id = HELPERS.getURLParameterByName("id");
				if(id) {
					loadFromDatabase(id);
				}
				
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
					currentAU = L.mapbox.featureLayer().loadURL(CRUD_PATH + "/" + currentAUGID);
					//currentAU.on('ready', function(){
						//TODO: Load JSON via call-back
						for(i = 1; i < MAP_DRIVER.auComponents.length; i++) {
							currentAUGID = MAP_DRIVER.auComponents[i];
							currentAU = L.mapbox.featureLayer().loadURL(CRUD_PATH + "/" + currentAUGID);
							
							console.log(currentAU);

							for(j = 0; j < currentAU.geojson.features.length; j++) {
								compositeJSON.features.push(currentAU.geojson.features[j]);
							}
						}
						
						MAP_DRIVER.loadJSON(compositeJSON);
					//});
				});
				
				return;
			}
			
			return;
		});
		
		return;
	});
	
	return;
});
