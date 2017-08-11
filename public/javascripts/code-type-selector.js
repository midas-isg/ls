(function(HELPERS) {
	$(document).ready(function() {
		var codeTypesURL = CONTEXT + "/api/code-types";
		
		$.get(codeTypesURL, function(data, status, xhr) {
			var codeTypesInput = document.getElementById("code-type"),
				codeTypeOption,
				i;
			
			data.sort(HELPERS.nameCompare);
			codeTypes = data;
			
			for(i = 0; i < codeTypes.length; i++) {
				codeTypeOption = document.createElement("option");
				codeTypeOption.value = codeTypes[i].id;
				codeTypeOption.innerHTML = codeTypes[i].name;
				codeTypesInput.appendChild(codeTypeOption);
			}
			
			return;
		});
		
		return;
	});
	
	return;
})(HELPERS);
