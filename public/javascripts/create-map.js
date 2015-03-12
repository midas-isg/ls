var CREATE_MAP = null;
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
							setTextValue("#au-name", feature.properties.name);
							setTextValue("#au-code", feature.properties.code);
							setTextValue("#start-date", feature.properties.startDate);
							setTextValue("#end-date", feature.properties.endDate);
							PARENT_TREE.resetIsAboutList();
							AU_COMPOSITE_TREE.resetIsAboutList();
							
							var parentGID = feature.properties.parentGid;
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
				
				CREATE_MAP = new MapDriver();
				MAP_DRIVER = CREATE_MAP;
				var thisMapDriver = CREATE_MAP;
				
				thisMapDriver.map.whenReady(function() {
					return CREATE_MAP.map.setZoom(1, {minZoom: 1});
				});
				
				function loadFromDatabase(mapID) {
					thisMapDriver.geoJSONURL = crudPath + "/" + mapID;
					
					if(thisMapDriver.geoJSONURL) {
						thisMapDriver.featureLayer.loadURL(thisMapDriver.geoJSONURL);
					}
					
					thisMapDriver.featureLayer.on("ready", function() {
						var feature = thisMapDriver.featureLayer.getGeoJSON().features[0];
						
						thisMapDriver.kml = feature.properties.kml;
						
						$("#gid").prop("disabled", true);
						setTextValue("#au-type", feature.properties.locationTypeName);
						
						PARENT_TREE.clickIsAboutByValue(feature.properties.parentGid);
						setTextValue("input#parent", getFirstAlphaOnly(PARENT_TREE.tree.getNodeByKey(feature.properties.parentGid).title));
						$("input#parent").keyup();
						
						$("#save-button").hide();
						if(feature.properties.locationTypeName == "Epidemic Zone") {
							$("#update-button").show();
						}
						
						$("#new-button").show();
					});
					
					return;
				}
				var id = getURLParameterByName("id");
				if(id) {
					loadFromDatabase(id);
				}
				
				$("#new-button").click(function() {
					return location.assign(context + "/create");
				});
				
				$("#file-input").change(function() {
					CREATE_MAP.upload();
					
					return;
				});
				
				$("#download-button").click(function() {
					CREATE_MAP.download();
					
					return;
				});
				
				$("#save-button").click(function() {
					CREATE_MAP.saveMap();
					
					return;
				});
				
				$("#update-button").click(function() {
					CREATE_MAP.updateMap();
					
					return;
				});
				
				$("#composite-button").click(function() {
					console.log(CREATE_MAP.getAUComponents());
					
					var i;
					var currentAUGID;
					var currentAU;
					
					var compositeJSON = {};
					compositeJSON.type = "FeatureCollection";
					compositeJSON.id = null;
					compositeJSON.features = [];
					
					currentAUGID = CREATE_MAP.auComponents[0];
					currentAU = L.mapbox.featureLayer().loadURL(crudPath + "/" + currentAUGID);
					//currentAU.on('ready', function(){
						//TODO: Load JSON via call-back
						for(i = 1; i < CREATE_MAP.auComponents.length; i++) {
							currentAUGID = CREATE_MAP.auComponents[i];
							currentAU = L.mapbox.featureLayer().loadURL(crudPath + "/" + currentAUGID);
							
							console.log(currentAU);
							var j;
							for(j = 0; j < currentAU.geojson.features.length; j++) {
								compositeJSON.features.push(currentAU.geojson.features[j]);
							}
						}
						
						CREATE_MAP.loadJSON(compositeJSON);
					//});
				});
				
				var shiftKey = false;
				var controlKey = false;
				$(document).keydown(function(event) {
					//console.log("Key: " + event.which);
					
					if(event.which == 16) {
						shiftKey = true;
					}
					
					if(event.which == 17) {
						controlKey = true;
					}
					
					if(shiftKey && controlKey) {
						$("#au-create").show();
					}
					
					return;
				});
				
				$(document).keyup(function(event) {
					if(event.which == 16) {
						shiftKey = false;
					}
					
					if(event.which == 17) {
						controlKey = false;
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
