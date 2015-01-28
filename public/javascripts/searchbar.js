/*
searchbar.js
*/

$(document).ready(function() {
	$("#input").keyup(function(event) {
		switch(event.which)
		{
			case 13:
				$("#search-button").click();
				$("#search-button").focus();
			break;
			
			default:
			break;
		}
		
		return;
	});
	
	var limit = 5;
	bindSuggestionBox("#input", context + "/api/unique-location-names?limit=" + limit + "&q=");
	
	return;
});

function bindSuggestionBox(inputBox, URL) {
	jQuery(inputBox).autocomplete({
		source: function (request, response) {
			jQuery.get(URL + $(inputBox).val(), {
				name: request.name
			}, function (data) {
				var index = 0;
				var locations = [];
				
				locations[0] = "No search results found...";
				
				for(index = 0; index < data.length; index++) {
					locations[index] = data[index]['name'];
				}
				
				response(locations);
			});
		},
		minLength: 3,
		delay: 500
	});
	
	return;
}
