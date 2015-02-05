var crudPath = context + "/resources/aus";
var apolloJSONDataPath = context + "/api/locations/";
var MAP_DRIVER = null;

$(document).ready(function() {
	MAP_DRIVER = new MapDriver();
	
	$("#search-button").click(function() {
		return location.assign(context + "?q=" + $("#input").val());
	});
	
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

function MapDriver(){
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
	
	this.geoJSONURL = this.dataSourceURL + "?format=geojson"; //crudPath + "/" + id;
	this.apolloJSONURL = this.dataSourceURL + "?format=apollojson";
	this.kmlURL = this.dataSourceURL + "?format=kml";
	
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
	
	this.map = L.mapbox.map('map-data', 'examples.map-i86l3621', { worldCopyJump: true /*crs: L.CRS.EPSG385*/});
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
		
		thisMapDriver.mapID = thisMapDriver.featureLayer.getGeoJSON().id;
		$("#au-name").append("<strong>" + feature.properties.name + "</strong>");
		$("#au-location-type").append("<div class='pull-left pre-spaced'>" + feature.properties.locationTypeName + "</div>");
		
		setTextValue("#start-date", feature.properties.startDate);
		setTextValue("#end-date", feature.properties.endDate);
		
		if(feature.properties.endDate) {
			$("#historical-note").show();
		}
		
		$("#au-geojson").prop("href", thisMapDriver.geoJSONURL);
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
		
		listLineageRefs(feature.properties.lineage, "#au-lineage");
		
		var related = feature.properties.related;
		if(related.length > 0){
			$("#au-related").show();
			
			for(i = 0; i < related.length; i++) {
				$("#au-related").append("<a href='./browser?id=" + related[i].gid + "' class='pre-spaced'>" + related[i].name + "</a>");
				
				if(i < (related.length - 1)){
					$("#au-related").append("; ");
				}
			}
		}
		
		var children = feature.properties.children;
		show = false;
		var show2 = false;
		var show3 = false;
		if(children.length > 0) {
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
		
		setTextValue("#gid", feature.properties.gid);
		show = false;
		var codes = feature.properties.codes;
		for(i = 0; i < codes.length; i++) {
			if(codes[i].codeTypeName != "ISG") {
				$("#codes").append("<div style='text-indent: 50px;'><em>" + codes[i].codeTypeName + ":</em> " + codes[i].code + "</div>");
				show = true;
			}
		}
		if(show) {
			$("#codes").show();
		}
		
		feature.properties.title = feature.properties.name  + " " + feature.properties.locationTypeName + " from ";
		feature.properties.title = feature.properties.title + feature.properties.startDate;
		if(feature.properties.endDate) {
			feature.properties.title = feature.properties.title + " to " + feature.properties.endDate;
		}
		else {
			feature.properties.title += " to present";
		}
		
		thisMapDriver.map.legendControl.removeLegend(thisMapDriver.title);
		thisMapDriver.title = "<strong>" + feature.properties.title + "</strong>";
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

MapDriver.prototype.download = function() {
	var jsonData = this.featureLayer.toGeoJSON();
	var properties = null;
	
	for(var i = 0; i < jsonData.features.length; i++) {
		properties = jsonData.features[i].properties;
		properties.name = getValueText("#au-name");
		properties.startDate = getValueText("#start-date");
		properties.endDate = getValueText("#end-date");
		
		properties.code = getValueText("#au-geojson");
		
		properties.parentGid = getValueText("#au-lineage");
		properties.description = properties.name + ";" + properties.code + ";" + properties.startDate + ";" + properties.endDate + ";" + properties.parentGid;
	}
	
	if(!jsonData.id) {
		jsonData.id = this.mapID;
	}
	
	var data = "text/json;charset=utf-8," + encodeURIComponent(JSON.stringify(jsonData));
	
	//$('<a href="data:' + data + '" download="data.json">download JSON</a>').appendTo('#container');
	
	location.assign("data:'" + data);
	
	return jsonData;
}

MapDriver.prototype.upload = function() {
	var file = $('#json-input').get(0).files[0];
	var fileReader = new FileReader();
	
	fileReader.onload = (function() {
		var kmlData = fileReader['result'];
		var kmlDOM = (new DOMParser()).parseFromString(kmlData, 'text/xml');
		
		var jsonData = toGeoJSON.kml(kmlDOM);
		
		if(jsonData.features.length == 0) {
			jsonData = JSON.parse(kmlData);
		}
		
		var properties = jsonData.features[0].properties;
		setTextValue("#au-name", properties.name);
		setTextValue("#start-date", properties.startDate);
		setTextValue("#end-date", properties.endDate);
		
		setTextValue("#au-geojson", properties.code);
		
		//setTextValue("#au-lineage", properties.parentGid);
		
		thisMapDriver.loadJSON(jsonData);
	});
	
	var fileString = fileReader.readAsText(file);
	
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
