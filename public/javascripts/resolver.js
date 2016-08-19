$(document).ready(function Resolver() {
	var input,
		output = {headers: [], rows: [], mappingHeaders: [], mappings:[]},
		fileReader = new FileReader();
	
	(function initialize() {
		$("#process-button").click(function() {
			$("#input-table thead").remove();
			$("#input-table tbody").remove();
			$("#input-table").append("<thead></thead>");
			$("#input-table").append("<tbody></tbody>");
			
			readInputFile();
			
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
		
	function processCSVdata() {
		var resultLines = fileReader.result.split("\n"),
			currentLine = resultLines[0].split(","),
			i,
			j;
		
		for(j = 0; j < currentLine.length; j++) {
			output.headers.push(currentLine[j].replace(/"/g, ""));
		}
		output.mappingHeaders.push("used_input");
		output.mappingHeaders.push("ls_id");
		
		for(i = 0; i < (resultLines.length - 1); i++) {
			if(resultLines[i + 1] !== "") {
				currentLine = resultLines[i + 1].split(",");
				output.rows.push({columns: [], header: {}});
				
				for(j = 0; j < currentLine.length; j++) {
					output.rows[i].columns.push(currentLine[j].replace(/"/g, ""));
					output.rows[i].header[output.headers[j]] = output.rows[i].columns[j];
				}
				
				output.mappings.push({columns: []});
				output.mappings[i].columns.push(undefined);
				output.mappings[i].columns.push(undefined);
			}
		}
		
		console.log(output);
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
		
		fileReader.readAsText(input);
		
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
			
			$(row).append("<td style='width: " + columnWidth + "%;'>" + output.mappings[i].columns[0] + "</td>");
			$(row).append("<td style='width: " + columnWidth + "%;'>" + output.mappings[i].columns[1] + "</td>");
			
			$("#input-table tbody").append(row);
		}
		
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