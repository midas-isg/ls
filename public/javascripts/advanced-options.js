var ADVANCED_OPTIONS_TOGGLE_EFFECTS = function() {
	return console.warn("Please override ADVANCED_OPTIONS_TOGGLE_EFFECTS with appropriate function");
};

$(document).ready(function() {
	$("#advanced-options-toggle").click(function() {
		$("#advanced-options").toggle();
		$("#advanced-options-toggle").toggleClass("active");
		ADVANCED_OPTIONS_TOGGLE_EFFECTS();
		
		return;
	});
	
	return;
});
