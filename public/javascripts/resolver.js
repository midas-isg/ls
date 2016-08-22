$(document).ready(function Resolver() {
	var countriesURL = "",
		searchURL = "",
		input,
		output,
		fileReader = new FileReader();
	
	(function initialize() {
		$("#process-button").click(function() {
			output = {headers: [], rows: [], mappingHeaders: [], mappings:[]};
			$("#search-priorities").empty();
			$("#input-table thead").remove();
			$("#input-table tbody").remove();
			$("#input-table").append("<thead></thead>");
			$("#input-table").append("<tbody></tbody>");
			
			readInputFile();
			
			return;
		});
		
		$("#resolve-button").click(function() {
			resolveAmbiguities();
			
			return;
		});
		
		$("#add-priority").click(function() {
			addPrioritySelector();
			return;
		});
		
		$("#remove-priority").click(function() {
			removePrioritySelector();
			
			return;
		});
		
		fileReader.onload = processCSVdata;
		
		return;
	})();
	
	function resolveAmbiguities() {
		
		
		return;
	}
	
	function processCSVdata() {
		parseData();
		displayTable();
		addPrioritySelector();
		
		return;
	}
	
	function readInputFile() {
		input = document.getElementById("csv-input").files[0];
		
		if(!input) {
			alert("Please select CSV file to upload");
			
			return;
		}
		
		$("#loading-gif").show();
		fileReader.readAsText(input);
		
		return;
	}
	
	function parseData() {
		var parsedData = Papa.parse(fileReader.result),
			c = 0,
			i = 0,
			j;
		
		for(j = 0; j < parsedData.data[i].length; j++) {
			output.headers.push(parsedData.data[i][j]);
		}
		output.mappingHeaders.push("used_input");
		output.mappingHeaders.push("apollo_location_code");
		
		for(i = 1; i < parsedData.data.length; i++) {
			if(parsedData.data[i].length === output.headers.length) {
				output.rows.push({columns: [], header: {}});
				
				for(j = 0; j < parsedData.data[i].length; j++) {
					output.rows[c].columns.push(parsedData.data[i][j]);
					output.rows[c].header[output.headers[j]] = output.rows[c].columns[j];
				}
				
				output.mappings.push({columns: []});
				output.mappings[c].columns.push(undefined);
				output.mappings[c].columns.push(undefined);
				c++;
			}
		}
		console.log(output);
		
		return;
	}
	
	function displayTable() {
		var i,
			j,
			row = document.createElement("tr"),
			columnCount = output.headers.length + output.mappingHeaders.length,
			headerWidth = 99 / columnCount,
			columnWidth = 100 / columnCount;
		
		for(i = 0; i < output.headers.length; i++) {
			$(row).append("<td style='width: " + headerWidth + "%;'>" + output.headers[i] + "</td>");
		}
		$(row).append("<td style='width: " + headerWidth + "%;'>" + output.mappingHeaders[0] + "</td>");
		$(row).append("<td style='width: " + headerWidth + "%;'>" + output.mappingHeaders[1] + "</td>");
		$("#input-table thead").append(row);
		
		for(i = 0; i < output.rows.length; i++) {
			row = document.createElement("tr");
			for(j = 0; j < output.rows[i].columns.length; j++) {
				$(row).append("<td style='width: " + columnWidth + "%;'>" + output.rows[i].columns[j] + "</td>");
			}
			
			$(row).append("<td id='input-" + i + "' style='width: " + columnWidth + "%;'>" + output.mappings[i].columns[0] + "</td>");
			$(row).append("<td id='code-" + i + "' style='width: " + columnWidth + "%;'>" + output.mappings[i].columns[1] + "</td>");
			
			$("#input-table tbody").append(row);
		}
		
		$("#loading-gif").hide();
		
		return;
	}
	
	function addPrioritySelector() {
		var newSelector = document.createElement("select"),
			i,
			order = $("#search-priorities").children().length;
		
		for(i = 0; i < output.headers.length; i++) {
			$(newSelector).append("<option value='" + i + "'>" + output.headers[i] + "</option>");
		}
		
		newSelector.id = "priority-" + order;
		
		$("#search-priorities").append("<div id='priority-col-" + order + "' style='float: left;'><legend>Priority-" + (order + 1) + "</legend></div>");
		$("#priority-col-" + order).append(newSelector);
		
		return;
	}
	
	function removePrioritySelector() {
		var deletedSelector = "#priority-col-" + ($("#search-priorities").children().length - 1);
		
		$(deletedSelector).remove();
		
		return;
	}
	
	return;
});