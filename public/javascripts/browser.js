var crudPath = context + "/api/locations";

var apolloJSONDataPath = context + "/api/locations/";
var BROWSER_MAP = null;

$(document).ready(function() {
	BROWSER_MAP = new BrowserMap();
	
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

function BrowserMap() {
	var thisBrowserMap = this;
	var id = getURLParameterByName("id");
	var format = getURLParameterByName("format");
	var query = getURLParameterByName("q");
	var limit = getURLParameterByName("limit");
	var offset = getURLParameterByName("offset");
	this.title = "";
	this.mapID = id; //'tps23.k1765f0g';
	
	if(id) {
		this.dataSourceURL = context + "/api/locations/" + id;
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
	
	var southWest = L.latLng(-90, -180);
	var northEast = L.latLng(90, 180);
	var mapBounds = L.latLngBounds(southWest, northEast);
	
	this.map = L.mapbox.map('map-data', 'examples.map-i86l3621', { worldCopyJump: true, minZoom: 1, bounceAtZoomLimits: false, maxBounds: mapBounds /*crs: L.CRS.EPSG385*/});
	this.map.legendControl.addLegend(this.title);
	
	this.drawControl = null;
	
	this.loadFeatureLayer();
	
	return;
}

BrowserMap.prototype.loadFeatureLayer = function() {
	thisBrowserMap = this;
	
	if(this.geoJSONURL) {
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
		
		var geoJSON = thisBrowserMap.featureLayer.getGeoJSON();
		var feature = geoJSON.features[0];
		
		var minLng = geoJSON.bbox[0];
		var minLat = geoJSON.bbox[1];
		var maxLng = geoJSON.bbox[2];
		var maxLat = geoJSON.bbox[3];
		var southWest = L.latLng(minLat, minLng);
		var northEast = L.latLng(maxLat, maxLng);
		var bounds = L.latLngBounds(southWest, northEast);
		thisBrowserMap.map.fitBounds(bounds);
		
		var properties = null;
		if(geoJSON.properties) {
			properties = geoJSON.properties;
		}
		else {
			properties = feature.properties;
		}
		
		thisBrowserMap.mapID = properties.gid;
		properties.description = properties.name;
		
		$("#au-name").append("<strong>" + properties.name + "</strong>");
		$("#au-location-type").append("<div class='pull-left pre-spaced'>" + properties.locationTypeName + "</div>");
		
		if(properties.locationDescription) {
			$("#description").append("<div class='pull-left pre-spaced'>" + properties.locationDescription + "</div>");
			$("#description").show();
		}
		
		setTextValue("#start-date", properties.startDate);
		setTextValue("#end-date", properties.endDate);
		
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
		
		listLineageRefs(properties.lineage, "#au-lineage");
		
		var related = properties.related;
		if(related && (related.length > 0)){
			$("#au-related").show();
			
			for(i = 0; i < related.length; i++) {
				$("#au-related").append("<a href='./browser?id=" + related[i].gid + "' class='pre-spaced'>" + related[i].name + "</a>");
				
				if(i < (related.length - 1)){
					$("#au-related").append("; ");
				}
			}
		}
		
		var children = properties.children;
		if(children && (children.length > 0)) {
			var buckets = [],
			i,
			locationType,
			locationDivID;
			for(i = 0; i < children.length; i++) {
				buckets[i] = children[i].locationTypeName;
			}
			buckets.sort();
			
			for(i = 0; i < buckets.length; i++) {
				locationDivID = buckets[i].replace(/ /g, "-").toLowerCase();
				
				if($("#" + locationDivID).length == 0) {
					$("#au-children").append("<div id='" + locationDivID + "' class='extra-bottom-space'><em class='pull-left'>" + buckets[i] + " children:</em></div>");
				}
			}
			
			for(i = 0; i < children.length; i++) {
				auName = children[i].name;
				auGID = children[i].gid;
				
				locationType = children[i].locationTypeName;
				locationDivID = locationType.replace(/ /g, "-").toLowerCase();
				
				if($("#" + locationDivID).children().length > 1) {
					$("#" + locationDivID).append(", ");
				}
				
				$("#" + locationDivID).append("<a href='./browser?id=" + auGID + "' class='pre-spaced' style='text-decoration: underline;' title='ID: "+ auGID +"'>" + auName + "</a>");
			}
			
			$("#au-children").show();
		}
		
		setTextValue("#gid", thisBrowserMap.mapID);
		var show = false;
		var codes = properties.codes;
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
		
		properties.title = properties.name  + " " + properties.locationTypeName + " from ";
		properties.title = properties.title + properties.startDate;
		if(properties.endDate) {
			properties.title = properties.title + " to " + properties.endDate;
		}
		else {
			properties.title += " to present";
		}
		
		thisBrowserMap.map.legendControl.removeLegend(thisBrowserMap.title);
		thisBrowserMap.title = "<strong>" + properties.title + "</strong>";
		thisBrowserMap.map.legendControl.addLegend(thisBrowserMap.title);
	});
	
	this.featureLayer.on('error', function(err) {
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
		var i;
		var features = jsonData.features;
		for(i = 0; i < features.length; i++) {
			features[i].properties.description = features[i].properties.name;
		}
		
		this.featureLayer.setGeoJSON(jsonData);
	}
	
	return;
}

BrowserMap.prototype.getJSONData = function(URL) {
	/*
	$.get(URL, function(data, status) {
		$("#map-data").text(JSON.stringify(data));
	});
	*/
	
	window.location.assign(URL);
	
	return;
}

BrowserMap.prototype.getKMLData = function() {
	
	return;
}
