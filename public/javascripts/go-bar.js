/*
go-bar.js
*/

$(document).ready(function() {
	var browserRoute = CONTEXT + "/browser";
	
	$("#location-code-input").keyup(function(event) {
		switch(event.which)
		{
			case 13:
				$("#go-button").click();
				$("#go-button").focus();
			break;
			
			default:
			break;
		}
		
		return;
	});
	
	$("#go-button").click(function() {
		goClick();
	});

	function goClick() {
		var id = $("#location-code-input").val();
		
		if(id) {
			return location.assign(browserRoute + "?id=" + id);
		}
	}
	
	return;
});