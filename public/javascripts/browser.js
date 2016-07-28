var crudPath = context + "/api/locations",
	apolloJSONDataPath = context + "/api/locations/",
	BROWSER_MAP =
(function() {
	function BrowserMap() {
		var thisBrowserMap = this,
			id = HELPERS.getURLParameterByName("id"),
			format = HELPERS.getURLParameterByName("format"),
			query = HELPERS.getURLParameterByName("q"),
			limit = HELPERS.getURLParameterByName("limit"),
			offset = HELPERS.getURLParameterByName("offset");

		this.title = "";
		this.mapID = id; //'tps23.k1765f0g';

		if(id) {
			this.dataSourceURL = context + "/api/locations/" + id;
			this.propertiesURL = this.dataSourceURL + "?maxExteriorRings=0";
			format = "ID";
		}
		else if(query) {
			format = "query";
			this.dataSourceURL = context + "/api/locations?q=" + query;// + "&limit=" + limit + "&offset=" + offset;
		}

		this.geoJSONURL = this.dataSourceURL + ".geojson";
		this.apolloJSONURL = this.dataSourceURL + ".json";
		this.kmlURL = this.dataSourceURL + ".kml";

		this.accessToken = 'pk.eyJ1IjoidHBzMjMiLCJhIjoiVHEzc0tVWSJ9.0oYZqcggp29zNZlCcb2esA';
		this.featureLayer = null;
		this.map = null;

		switch(format) {
			case "apollojson":
			case "ApolloJSON":
				this.getJSONData(this.apolloJSONURL);
			break;

			case "GeoJSON":
			case "geojson":
				this.getJSONData(this.geoJSONURL);
			break;

			case "KML":
			case "kml":
				this.getKMLData();
			break;

			case "banana":
				location.assign("https://www.youtube.com/watch?v=ex0URF-hWj4");
			break;

			case "query":
				this.initialize();

				$.get(this.dataSourceURL, function(data, status) {
					/*
					var i;
					var geoJSON = {};
					for(i = 0; i < data.; i++) {
						//
					}
					*/

					//console.log(data);
					thisBrowserMap.loadJSON(data.geoJSON);
				});
			break;

			case "ID":
				this.initialize();
			break;

			default:
				//this.initialize();
			break;
		}

		return;
	}

	BrowserMap.prototype.initialize = function() {
		$("#map-data").text("");
		$("#header-data").show();

		L.mapbox.accessToken = this.accessToken;

		var southWest = L.latLng(-90, -180),
			northEast = L.latLng(90, 180),
			mapBounds = L.latLngBounds(southWest, northEast);

		this.map = L.mapbox.map('map-data', 'examples.map-i86l3621', { worldCopyJump: true, minZoom: 1, bounceAtZoomLimits: false, maxBounds: mapBounds /*crs: L.CRS.EPSG385*/});
		this.map.legendControl.addLegend(this.title);

		this.drawControl = null;

		this.loadFeatureLayer();

		return;
	}

	BrowserMap.prototype.loadFeatureLayer = function() {
		thisBrowserMap = this;

		if(this.geoJSONURL) {
			//initialize meta data-related properties here
			(function initializeProperties() {
				$.get(thisBrowserMap.propertiesURL, function(data, status) {
					var properties = data.properties || data.features[0].properties,
						related,
						children,
						buckets = [],
						locationType,
						locationDivID,
						show,
						codes,
						i,
						delimiter,
						OtherNames,
						unsupportedCharactersRegExp = /([^a-zA-Z0-9À-öø-ÿ])/g;

					thisBrowserMap.mapID = properties.gid;
					properties.description = properties.name;

					$("#au-name").append("<strong>" + properties.name + "</strong>");
					$("#au-location-type").append("<div class=''>" + properties.locationTypeName + "</div>");

					if(properties.locationDescription) {
						$("#description").append("<div class='pull-left pre-spaced'>" + properties.locationDescription + "</div>");
						$("#description").show();
					}

					HELPERS.setTextValue("#start-date", properties.startDate);
					HELPERS.setTextValue("#end-date", properties.endDate);

					if(properties.startDate == "0001-01-01") {
						$("#founding-date-div").hide();
					}

					if(properties.endDate) {
						$("#historical-note").show();
					}

					$("#au-geojson").prop("href", thisBrowserMap.geoJSONURL);
					$("#au-geojson").prop("type", "application/vnd.geo+json");
					if(thisBrowserMap.geoJSONURL) {
						$("#au-geojson").css("text-decoration", "underline");
					}

					$("#au-kml").prop("href", thisBrowserMap.kmlURL);
					if(thisBrowserMap.kmlURL) {
						$("#au-kml").css("text-decoration", "underline");
					}

					$("#au-apollojson").prop("href", thisBrowserMap.apolloJSONURL);
					if(thisBrowserMap.apolloJSONURL) {
						$("#au-apollojson").css("text-decoration", "underline");
					}

                    HELPERS.listLineageRefs(properties.lineage, "#au-lineage");

					related = properties.related;
					if(related && (related.length > 0)){
						$("#au-related").show();

						for(i = 0; i < related.length; i++) {
							$("#au-related").append("<a href='./browser?id=" + related[i].gid + "' class='pre-spaced'>" + related[i].name + "</a>");

							if(i < (related.length - 1)){
								$("#au-related").append("; ");
							}
						}
					}

					children = properties.children;
					if(children && (children.length > 0)) {
						for(i = 0; i < children.length; i++) {
							buckets[i] = children[i].locationTypeName;
						}
						buckets.sort();

						for(i = 0; i < buckets.length; i++) {
							locationDivID = buckets[i].replace(unsupportedCharactersRegExp, "-").toLowerCase();

							if($("#" + locationDivID).length == 0) {
								$("#au-children").append("<div id='" + locationDivID + "' class='extra-bottom-space'><em class='pull-left'>" + buckets[i] + " sub-locations:</em></div>");
							}
						}

						for(i = 0; i < children.length; i++) {
							auName = children[i].name;
							auGID = children[i].gid;

							locationType = children[i].locationTypeName;
							locationDivID = locationType.replace(unsupportedCharactersRegExp, "-").toLowerCase();

							if($("#" + locationDivID).children().length > 1) {
								$("#" + locationDivID).append(", ");
							}

							$("#" + locationDivID).append("<a href='./browser?id=" + auGID + "' class='pre-spaced' style='text-decoration: underline;' title='ID: "+ auGID +"'>" + auName + "</a>");
						}

						$("#au-children").show();

						if($("#epidemic-zone").children().length > 1) {
							$("#epidemic-zone").show();
						}
					}

					HELPERS.setTextValue("#gid", thisBrowserMap.mapID);
					show = false;
					codes = properties.codes;
					if(codes) {
						for(i = 0; i < codes.length; i++) {
							if(codes[i].codeTypeName != "ISG") {
								$("#codes").append("<div style='text-indent: 50px;'><em>" + codes[i].codeTypeName + ":</em> " + codes[i].code + "</div>");
								show = true;
							}
						}
						if(show) {
							$("#codes").show();
						}
					}

					OtherNames = properties.otherNames;
					if(OtherNames.length > 0) {
						delimiter = ",";
						for(i = 0; i < OtherNames.length; i++) {
							if(OtherNames[i].name.search(",") !== -1) {
								delimiter = ";";
								break;
							}
						}

						for(i = 0; i < OtherNames.length; i++) {
							name = "<strong class='pull-left pre-spaced'>" + OtherNames[i].name;

							if(i < OtherNames.length - 1){
								name += delimiter;
							}

							name += "</strong>"
							$("#otherNames").append(name);
							show = true;
						}
						if(show) {
							$("#otherNames").show();
						}
					}

					/*---TODO: This is data that should be shared with the map within the geoJSON call back (perhaps via passing or a *global*)---*/
					properties.title = properties.name  + " " + properties.locationTypeName + " from " + properties.startDate;
					if(properties.endDate) {
						properties.title = properties.title + " to " + properties.endDate;
					}
					else {
						properties.title += " to present";
					}
					/*------*/

					return;
				});

				return;
			})();

			this.featureLayer = L.mapbox.featureLayer().loadURL(this.geoJSONURL);
		}
		else if(this.mapID) {
			this.featureLayer = L.mapbox.featureLayer().loadID(this.mapID);
		}
		else /* if(!this.mapID) */ {
			this.featureLayer = L.mapbox.featureLayer({
				type: "FeatureCollection",
				features: [{
					type: "Feature",
					geometry: {
						type: "Point",
						coordinates: [0, 0]
					},
					properties: { }
				}]
			});
		}

		this.featureLayer.on('ready', function() {
			thisBrowserMap.loadJSON(thisBrowserMap.featureLayer.getGeoJSON());

			thisBrowserMap.featureLayer.addTo(thisBrowserMap.map);

			var geoJSON = thisBrowserMap.featureLayer.getGeoJSON(),
				feature = geoJSON.features[0],
				minLng = geoJSON.bbox[0],
				minLat = geoJSON.bbox[1],
				maxLng = geoJSON.bbox[2],
				maxLat = geoJSON.bbox[3],
				southWest = L.latLng(minLat, minLng),
				northEast = L.latLng(maxLat, maxLng),
				bounds = L.latLngBounds(southWest, northEast),
				properties = null;

			thisBrowserMap.map.fitBounds(bounds);

			/*---This data needs to be pulled from a global or shared variable if properties are removed from the geojson---*/
			properties = geoJSON.properties || feature.properties;

			properties.title = properties.name  + " " + properties.locationTypeName + " from " + properties.startDate;
			if(properties.endDate) {
				properties.title = properties.title + " to " + properties.endDate;
			}
			else {
				properties.title += " to present";
			}
			/*------*/

			thisBrowserMap.map.legendControl.removeLegend(thisBrowserMap.title);
			thisBrowserMap.title = "<strong>" + properties.title + "</strong>";
			thisBrowserMap.map.legendControl.addLegend(thisBrowserMap.title);
			$("#loader").fadeOut("slow");

			return;
		});

		this.featureLayer.on('error', function(err) {
			$("#loader").fadeOut("slow");
			$("#error").append("Error: " + err['error']['statusText']);
			console.log("Error: " + err['error']['statusText']);

			if((thisBrowserMap.featureLayer.getLayers().length == 0) && thisBrowserMap.mapID) {
				console.log("Attempting to load via mapbox ID");
				thisBrowserMap.featureLayer = L.mapbox.featureLayer().loadID(thisBrowserMap.mapID);
			}

			thisBrowserMap.featureLayer.on('ready', function() {
				thisBrowserMap.featureLayer.addTo(thisBrowserMap.map);
			});
		});

		return;
	}

	BrowserMap.prototype.loadJSON = function(jsonData) {
		if(jsonData) {
			//multiPolygonsToPolygons(jsonData);
			var i,
				features = jsonData.features;

			for(i = 0; i < features.length; i++) {
				features[i].properties.description = features[i].properties.name;
			}

			this.featureLayer.setGeoJSON(jsonData);
		}

		return;
	}

	BrowserMap.prototype.getJSONData = function(URL) {
		window.location.assign(URL);

		return;
	}

	BrowserMap.prototype.getKMLData = function() {

		return;
	}

	return new BrowserMap();
})();

$(document).ready(function() {
	$("#input").keyup(function(event) {
		switch(event.which)
		{
			case 13:
				$("#search-button").click();
				break;

			default:
				break;
		}

		return;
	});

	return;
});
