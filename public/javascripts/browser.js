var crudPath = context + "/resources/aus";
var apolloJSONDataPath = context + "/api/locations/";
var MAP_DRIVER = null;

$(document).ready(function() {
	MAP_DRIVER = new MapDriver();
	
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

function MapDriver() {
	var thisMapDriver = this;
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
	
	this.geoJSONURL = this.dataSourceURL + ".geojson"; //crudPath + "/" + id;
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
				var i;
				/*
				var geoJSON = {};
				for(i = 0; i < data.; i++) {
					//
				}
				*/
				
				//console.log(data);
				thisMapDriver.loadJSON(data.geoJSON);
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

MapDriver.prototype.initialize = function() {
	$("#map-data").text("");
	$("#header-data").show();
	
	L.mapbox.accessToken = this.accessToken;
	
	this.map = L.mapbox.map('map-data', 'examples.map-i86l3621', { worldCopyJump: true, minZoom: 1, bounceAtZoomLimits: false /*crs: L.CRS.EPSG385*/});
	this.map.legendControl.addLegend(this.title);
	
	this.drawControl = null;
	
	this.loadFeatureLayer();
	
	return;
}

MapDriver.prototype.loadFeatureLayer = function() {
	thisMapDriver = this;
	
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
		thisMapDriver.loadJSON(thisMapDriver.featureLayer.getGeoJSON());
		
		thisMapDriver.featureLayer.addTo(thisMapDriver.map);
		
		var feature = thisMapDriver.featureLayer.getGeoJSON().features[0];
		
		var geoJSON = thisMapDriver.featureLayer.getGeoJSON();
		var minLng = geoJSON.bbox[0];
		var minLat = geoJSON.bbox[1];
		var maxLng = geoJSON.bbox[2];
		var maxLat = geoJSON.bbox[3];
		var southWest = L.latLng(minLat, minLng);
		var northEast = L.latLng(maxLat, maxLng);
		var bounds = L.latLngBounds(southWest, northEast);
		thisMapDriver.map.fitBounds(bounds);
		
		var properties = null;
		if(geoJSON.properties) {
			properties = geoJSON.properties;
		}
		else {
			properties = feature.properties;
		}
		
		thisMapDriver.mapID = properties.gid;
		
		$("#au-name").append("<strong>" + properties.name + "</strong>");
		$("#au-location-type").append("<div class='pull-left pre-spaced'>" + properties.locationTypeName + "</div>");
		
		if(properties.description) {
			$("#description").append("<div class='pull-left pre-spaced'>" + properties.description + "</div>");
			$("#description").show();
		}
		
		setTextValue("#start-date", properties.startDate);
		setTextValue("#end-date", properties.endDate);
		
		if(properties.endDate) {
			$("#historical-note").show();
		}
		
		$("#au-geojson").prop("href", thisMapDriver.geoJSONURL);
		$("#au-geojson").prop("type", "application/vnd.geo+json");
		if(thisMapDriver.geoJSONURL) {
			$("#au-geojson").css("text-decoration", "underline");
		}
		
		$("#au-kml").prop("href", thisMapDriver.kmlURL);
		if(thisMapDriver.kmlURL) {
			$("#au-kml").css("text-decoration", "underline");
		}
		
		$("#au-apollojson").prop("href", thisMapDriver.apolloJSONURL);
		if(thisMapDriver.apolloJSONURL) {
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
		show = false;
		var show2 = false;
		var show3 = false;
		if(children && (children.length > 0)) {
			//console.log(children);
			//auName = children[0].name;
			//auGID = children[0].gid;
			//$("#au-children").append("<a href='./browser?id=" + auGID + "' class='pre-spaced' style='text-decoration: underline;' title='ID: "+ auGID +"'>" + auName + "</a>");
			
			for(i = 0; i < children.length; i++) {
				auName = children[i].name;
				auGID = children[i].gid;
				
				if(children[i].locationTypeName == "Epidemic Zone") {
					if(show) {
						$("#au-epidemic-zones").append(";");
					}
					
					$("#au-epidemic-zones").append("<a href='./browser?id=" + auGID + "' class='pre-spaced' style='text-decoration: underline;' title='ID: "+ auGID +"'>" + auName + "</a>");
					show = true;
				}
				else if(children[i].locationTypeName == "Census Tract") {
					if(show2) {
						$("#census-tract").append("; ");
					}
					
					$("#census-tract").append("<a href='./browser?id=" + auGID + "' class='pre-spaced' style='text-decoration: underline;' title='ID: "+ auGID +"'>" + auName + "</a>");
					show2 = true;
				}
				else /* if(children[i].locationTypeName != "Epidemic Zone") */ {
					if(show3) {
						$("#au-children").append("; ");
					}
					
					$("#au-children").append("<a href='./browser?id=" + auGID + "' class='pre-spaced' style='text-decoration: underline;' title='ID: "+ auGID +"'>" + auName + "</a>");
					show3 = true;
				}
			}
			
			if(show) {
				$("#au-epidemic-zones").show();
			}
			
			if(show2) {
				$("#census-tract").show();
			}
			
			if(show3) {
				$("#au-children").show();
			}
		}
		
		setTextValue("#gid", thisMapDriver.mapID);
		show = false;
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
		
		thisMapDriver.map.legendControl.removeLegend(thisMapDriver.title);
		thisMapDriver.title = "<strong>" + properties.title + "</strong>";
		thisMapDriver.map.legendControl.addLegend(thisMapDriver.title);
	});
	
	this.featureLayer.on('error', function(err) {
		console.log("Error: " + err['error']['statusText']);
		
		if((thisMapDriver.featureLayer.getLayers().length == 0) && thisMapDriver.mapID) {
			console.log("Attempting to load via mapbox ID");
			thisMapDriver.featureLayer = L.mapbox.featureLayer().loadID(thisMapDriver.mapID);
		}
		
		thisMapDriver.featureLayer.on('ready', function() {
			thisMapDriver.featureLayer.addTo(thisMapDriver.map);
		});
	});
	
	return;
}

MapDriver.prototype.loadJSON = function(jsonData) {
	multiPolygonsToPolygons(jsonData);
	this.featureLayer.setGeoJSON(jsonData);
	
	return;
}

MapDriver.prototype.getJSONData = function(URL) {
	/*
	$.get(URL, function(data, status) {
		$("#map-data").text(JSON.stringify(data));
	});
	*/
	
	window.location.assign(URL);
	
	return;
}

MapDriver.prototype.getKMLData = function() {
	
	
	return;
}
