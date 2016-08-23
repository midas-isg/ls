$(document).ready(function Resolver() {
	var DEBUG = true,
		countriesURL = "api/locations/find-by-type/1",
		searchURL = "api/locations/find-bulk",
		input,
		output,
		fileReader = new FileReader();
	
	(function initialize() {
		(function() {
			$.ajax({
				url: countriesURL,
				type: "GET",
				success: function(result, status, xhr) {
					var i,
						features = result.features,
						countries = [],
						countryOption;
					
					for(i = 0; i < features.length; i++) {
						countries.push({gid: undefined, name:""});
						countries[i].gid = features[i].properties.gid;
						countries[i].name = features[i].properties.name;
					}
					
					countries.sort(function(countryA, countryB) {
						return (countryA.name < countryB.name ? -1: 1);
					});
					
					for(i = 0; i < countries.length; i++) {
						countryOption = document.createElement("option");
						countryOption.value = countries[i].gid;
						countryOption.innerHTML = countries[i].name;
						$("#country-selection").append(countryOption);
					}
					
					return;
				},
				error: function(xhr, status, error) {
					console.warn(error);
					return;
				}
			});
			
			return;
		})();
		
		$("#process-button").click(function() {
			output = {headers: [], rows: [], mappingHeaders: [], mappings:[]};
			$("#search-priorities").empty();
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
		
		$("#download").click(function() {
			var link = document.createElement("a"),
				downloadable = "",
				i,
				j;
			
			for(i = 0; i < output.headers.length; i++) {
				downloadable += "\"" + output.headers[i] + "\","; 
			}
			
			for(i = 0; i < (output.mappingHeaders.length - 1); i++) {
				downloadable += "\"" + output.mappingHeaders[i] + "\",";
			}
			downloadable += "\"" + output.mappingHeaders[i] + "\"\n";
			
			for(i = 0; i < output.rows.length; i++) {
				for(j = 0; j < output.rows[i].columns.length; j++) {
					downloadable += "\"" + output.rows[i].columns[j] + "\",";
				}
				
				for(j = 0; j < (output.mappings[i].length - 1); j++) {
					downloadable += "\"" + output.mappings[i].columns[j] + "\",";
				}
				downloadable += "\"" + output.mappings[i].columns[j] + "\"\n";
			}
			
			link.href = "data:text/plain;charset=utf-8," + encodeURIComponent(downloadable);
			link.download = "mappings.csv";
			link.click();
			
			return;
		});
		
		fileReader.onload = processCSVdata;
		
		return;
	})();
	
	function resolveAmbiguities() {
		var bulkInput = [],
			mappings = {},
			rootALC = $("#country-selection").val(),
			columnNumber = $("#priority-0").val(),
			i;
		
		for(i = 0; i < output.rows.length; i++) {
			bulkInput.push({"queryTerm": output.rows[i].columns[columnNumber]});
			
			if(rootALC !== -1) {
				bulkInput[i]["rootALC"] = rootALC;
			}
		}
		
		$.ajax({
			url: searchURL,
			type: "POST",
			data: JSON.stringify(bulkInput),
			contentType: "application/json",
			beforeSend: function(xhr) {
				$("#loading-gif").show();
				
				return;
			},
			success: function(result, status, xhr) {
				populateMappings(result);
				displayTable();
				
				return;
			},
			error: function(xhr, status, error) {
				console.warn(error);
				
				return;
			},
			complete: function(xhr, status) {
				$("#loading-gif").hide();
				
				return;
			}
		});
		
		return;
	}
	
	function populateMappings(batchResult) {
		var possibleMappings = {},
			properties,
			features,
			columnName = $($("#priority-0").children()[$("#priority-0").val()]).text(),
			entryName,
			entryChoices,
			option,
			i,
			j;
		
		for(i = 0; i < batchResult.length; i++) {
			properties = batchResult[i].properties;
			features = batchResult[i].features;
			
			possibleMappings[properties.queryTerm] = [];
			for(j = 0; j < features.length; j++) {
				possibleMappings[properties.queryTerm].push(features[j].properties);
			}
		}
		
		for(i = 0; i < output.rows.length; i++) {
			entryName = output.rows[i].header[columnName];
			
			$("#input-" + i).empty();
			$("#code-" + i).empty();
			if(possibleMappings[entryName].length === 1) {
				//todo: attach well-named input for re-resolution
				output.mappings[i].columns[0] = possibleMappings[entryName][0].name;
				output.mappings[i].columns[1] = possibleMappings[entryName][0].gid;
				
				entryChoices = document.createElement("input");
				entryChoices.type = "text";
				entryChoices.value = output.mappings[i][0];
				$("#input-" + i).append(entryChoices);
				$("#code-" + i).append(output.mappings[i].columns[1]);
			}
			else if(possibleMappings[entryName].length > 1) {
				entryChoices = document.createElement("select");
				entryChoices.id = "input-selection-" + i;
				entryChoices.row = i;
				
				for(j = 0; j < possibleMappings[entryName].length; j++) {
					option = document.createElement("option");
					option.value = possibleMappings[entryName][j].gid;
					option.id = "input-code-" + option.value;
					option.innerHTML = possibleMappings[entryName][j].name;
					$(entryChoices).append(option);
				}
				
				$("#input-" + i).append(entryChoices);
				$("#input-selection-" + i).change(function() {
					output.mappings[this.row].columns[0] = $("#input-code-" + this.value).text();
					output.mappings[this.row].columns[1] = this.value;
					$("#code-" + this.row).append(output.mappings[this.row].columns[1]);
					
					return;
				});
				$("#input-selection-" + i).change();
			}
			else /*if(possibleMappings[entryName].length < 1)*/ {
				//todo: attach well-named input for re-resolution
				output.mappings[i].columns[0] = "[" + columnName + "]";
				output.mappings[i].columns[1] = undefined;
				
				entryChoices = document.createElement("input");
				entryChoices.type = "text";
				entryChoices.value = output.mappings[i].columns[0];
				$("#input-" + i).append(entryChoices);
			}
		}
		
if(DEBUG) {
	console.debug(output);
}
		
		return;
	}
	
	function processCSVdata() {
		parseData();
		displayPreview();
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
				output.mappings[c].columns.push();
				output.mappings[c].columns.push();
				c++;
			}
		}
		
		return;
	}
	
	function displayPreview() {
		var i,
			j,
			row = document.createElement("tr"),
			columnCount = output.headers.length + output.mappingHeaders.length,
			headerWidth = 99 / columnCount,
			columnWidth = 100 / columnCount;
		
		$("#input-table thead").remove();
		$("#input-table tbody").remove();
		$("#input-table").append("<caption style='display: inherit; margin-left: auto; margin-right: auto;'>PREVIEW</caption>");
		$("#input-table").append("<thead></thead>");
		$("#input-table").append("<tbody></tbody>");
		
		for(i = 0; i < output.headers.length; i++) {
			$(row).append("<td style='width: " + headerWidth + "%;'>" + output.headers[i] + "</td>");
		}
		$(row).append("<td style='width: " + headerWidth + "%;'>" + output.mappingHeaders[0] + "</td>");
		$(row).append("<td style='width: " + headerWidth + "%;'>" + output.mappingHeaders[1] + "</td>");
		$("#input-table thead").append(row);
		
		for(i = 0; i < 10; i++) {
			row = document.createElement("tr");
			for(j = 0; j < output.rows[i].columns.length; j++) {
				$(row).append("<td style='width: " + columnWidth + "%;'>" + output.rows[i].columns[j] + "</td>");
			}
			
			$(row).append("<td id='input-" + i + "' style='width: " + columnWidth + "%;'>?</td>");
			$(row).append("<td id='code-" + i + "' style='width: " + columnWidth + "%;'>?</td>");
			
			$("#input-table tbody").append(row);
		}
		
		return;
	}
	
	function displayTable() {
		var i,
			j,
			row = document.createElement("tr"),
			columnCount = output.headers.length + output.mappingHeaders.length,
			headerWidth = 99 / columnCount,
			columnWidth = 100 / columnCount;
		
		$("#input-table caption").remove();
		$("#input-table thead").remove();
		$("#input-table tbody").remove();
		$("#input-table").append("<thead></thead>");
		$("#input-table").append("<tbody></tbody>");
		$("#busy-message").show();
		
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
			
			
			/*
			if(possibleMappings[entryName].length === 1) {
				//todo: attach well-named input for re-resolution
				output.mappings[i].columns[0] = possibleMappings[entryName][0].name;
				output.mappings[i].columns[1] = possibleMappings[entryName][0].gid;
				
				entryChoices = document.createElement("input");
				entryChoices.type = "text";
				entryChoices.value = output.mappings[i][0];
				$("#input-" + i).append(entryChoices);
				$("#code-" + i).append(output.mappings[i].columns[1]);
			}
			else if(possibleMappings[entryName].length > 1) {
				entryChoices = document.createElement("select");
				entryChoices.id = "input-selection-" + i;
				entryChoices.row = i;
				
				for(j = 0; j < possibleMappings[entryName].length; j++) {
					option = document.createElement("option");
					option.value = possibleMappings[entryName][j].gid;
					option.id = "input-code-" + option.value;
					option.innerHTML = possibleMappings[entryName][j].name;
					$(entryChoices).append(option);
				}
				
				$("#input-" + i).append(entryChoices);
				$("#input-selection-" + i).change(function() {
					output.mappings[this.row].columns[0] = $("#input-code-" + this.value).text();
					output.mappings[this.row].columns[1] = this.value;
					$("#code-" + this.row).append(output.mappings[this.row].columns[1]);
					
					return;
				});
				$("#input-selection-" + i).change();
			}
			else { //if(possibleMappings[entryName].length < 1)
				//todo: attach well-named input for re-resolution
				output.mappings[i].columns[0] = "unresolved";
				output.mappings[i].columns[1] = undefined;
				
				entryChoices = document.createElement("input");
				entryChoices.type = "text";
				entryChoices.value = output.mappings[i].columns[0];
				$("#input-" + i).append(entryChoices);
			}
			*/
			
			
			$(row).append("<td id='input-" + i + "' style='width: " + columnWidth + "%;'>" + (output.mappings[i].columns[0] || "") + "</td>");
			$(row).append("<td id='code-" + i + "' style='width: " + columnWidth + "%;'>" + (output.mappings[i].columns[1] || "") + "</td>");
			
			$("#input-table tbody").append(row);
		}
		
		$("#busy-message").hide();
		
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
		
		$("#search-priorities").append("<div id='priority-col-" + order + "' style='float: left;'><legend>Input Priority " + (order + 1) + "</legend></div>");
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