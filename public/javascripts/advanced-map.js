$(document).ready(function() {
	var resultsURL = CONTEXT + "/results?q=@",
		marker = L.marker([0, 0], {
				icon: L.mapbox.marker.icon({
					'marker-size': 'small',
					'marker-symbol': 'star',
					'marker-color': '#0000ff'
				}),
				clickable: false
			}
		),
		showMarker = false;
	
	marker.setOpacity(0);
	marker.addTo(SEARCH_MAP.map);
	
	ADVANCED_OPTIONS_TOGGLE_EFFECTS = function() {
		showMarker = !showMarker;
		
		if(showMarker) {
			marker.setOpacity(0.5);
		}
		else {
			marker.setOpacity(0);
		}
		
		return;
	}
	
	function updateMarker() {
		var latLng = L.latLng($("#latitude").val(), $("#longitude").val());
		
		return marker.setLatLng(latLng);
	}
	
	$("#latitude").change(function() {
		return updateMarker();
	});
	
	$("#longitude").change(function() {
		return updateMarker();
	});
	
	
	$("#search-button").click(function() {
		var latitude = $("#latitude").val(),
			longitude = $("#longitude").val();
		
		return location.assign(resultsURL + latitude + "," + longitude);
	});
	
	return;
});
