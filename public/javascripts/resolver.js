var DEBUG = true;

$(document).ready(function Resolver() {
	var countriesURL = "api/locations/find-by-type/1",
		searchURL = "api/locations/find-bulk",
		locationURL = "browser?id=",
		countryName,
		input,
		output,
		firstResolutionPass = true,
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
					
					countries.push({gid: 0, name: "[Multiple Countries]"});
					
					for(i = 0; i < countries.length; i++) {
						countryOption = document.createElement("option");
						countryOption.value = countries[i].gid;
						countryOption.id = "country-option-" + countryOption.value;
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
		
		$("#country-selection").change(function() {
			firstResolutionPass = true;
			
			if(this.value === 0) {
				countryName = "Multiple_countries";
				
				return;
			}
			
			countryName = $("#country-option-" + this.value).text();
			
			return;
		});
		
		$("#process-button").click(function() {
			if($("#").val()) {
				alert("Please select a file");
			}
			
			output = {headers: [], rows: [], mappingHeaders: [], mappings:[]};
			$("#search-priorities").empty();
			readInputFile();
			
			return;
		});
		
		$("#resolve-button").click(function() {
			if(!output) {
				alert("Please process a CSV");
				
				return;
			}
			
			if($("#country-selection").val() === "-1") {
				alert("Please select a country choice");
				
				return;
			}
			
			if(firstResolutionPass) {
				resolveAmbiguities();
			}
			else {
				resolveExceptions();
			}
			
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
			if(!output) {
				alert("No data to download yet");
				
				return;
			}
			
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
				
				if(output.mappings[i].selectedOption > -1) {
					downloadable += "\"" + output.mappings[i].options[output.mappings[i].selectedOption].inputName + "\",";
					downloadable += "\"" + output.mappings[i].options[output.mappings[i].selectedOption].id + "\"\n";
				}
				else {
					downloadable += "\"\",\"\"\n";
				}
			}
			
			link.href = "data:text/plain;charset=utf-8," + encodeURIComponent(downloadable);
			link.download = countryName + "-" + "mappings.csv";
			link.click();
			
			return;
		});
		
		fileReader.onload = processCSVdata;
		
		return;
	})();
	
	function resolveExceptions() {
		var bulkInput = [],
			name2rows = {},
			rootALC = $("#country-selection").val(),
			exceptions = $("[id^='exception-']"),
			i;
		
		for(i = 0; i < exceptions.length; i++) {
			name2rows[exceptions[i].value] = exceptions[i].row;
			bulkInput.push({"queryTerm": exceptions[i].value});
			
			if(rootALC > 0) {
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
				populateExceptions(name2rows, result);
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
	
	function populateExceptions(nameToRows, bulkResult) {
		var i,
			j,
			entryRow,
			properties,
			features;
		
		for(i = 0; i < bulkResult.length; i++) {
			properties = bulkResult[i].properties;
			features = bulkResult[i].features;
			entryRow = nameToRows[properties.queryTerm];
			output.mappings[entryRow].options = [];
			
			for(j = 0; j < features.length; j++) {
				output.mappings[entryRow].options.push({inputName: features[j].properties.name, id: features[j].properties.gid});
			}
		}
		
		return;
	}
	
	function resolveAmbiguities() {
		var bulkInput = [],
			rootALC = $("#country-selection").val(),
			columnNumber = $("#priority-0").val(),
			i;
		
		for(i = 0; i < output.rows.length; i++) {
			bulkInput.push({"queryTerm": output.rows[i].columns[columnNumber]});
			
			if(rootALC > 0) {
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
				firstResolutionPass = false;
				
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
			output.mappings[i].options = [];
			
			for(j = 0; j < possibleMappings[entryName].length; j++) {
				output.mappings[i].options.push({inputName: possibleMappings[entryName][j].name, id: possibleMappings[entryName][j].gid});
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
		firstResolutionPass = true;
		
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
				
				output.mappings.push({options: []});
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
		
		$("#input-table caption").remove();
		$("#input-table thead").remove();
		$("#input-table tbody").remove();
		$("#input-table").append("<caption style='text-align: center; background-color: #EEEE00;'>PREVIEW OF FIRST TEN ROWS</caption>");
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
			inputCell,
			codeCell,
			codeURL,
			columnCount = output.headers.length + output.mappingHeaders.length,
			entryChoices,
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
		$(row).append("<td style='width: " + headerWidth + "%;'><strong>" + output.mappingHeaders[0] + "</strong></td>");
		$(row).append("<td style='width: " + headerWidth + "%;'><strong>" + output.mappingHeaders[1] + "</strong></td>");
		$("#input-table thead").append(row);
		
		for(i = 0; i < output.rows.length; i++) {
			row = document.createElement("tr");
			for(j = 0; j < output.rows[i].columns.length; j++) {
				$(row).append("<td style='width: " + columnWidth + "%;'>" + output.rows[i].columns[j] + "</td>");
			}
			
			inputCell = document.createElement("td");
			codeCell = document.createElement("td");
			inputCell.id = "input-" + i;
			codeCell.id = "code-" + i;
			inputCell.style.width = columnWidth + "%";
			codeCell.style.width = columnWidth + "%";
			codeURL = document.createElement("a");
			codeURL.target = "_blank";
			$(codeCell).append(codeURL);
			
			if(output.mappings[i].options.length === 1) {
				inputCell.innerHTML = "<strong>" + output.mappings[i].options[0].inputName + "</strong>";
				codeURL.href = locationURL + output.mappings[i].options[0].id;
				codeURL.innerHTML = "<strong>" + output.mappings[i].options[0].id + "</strong>";
				output.mappings[i].selectedOption = 0;
			}
			else if(output.mappings[i].options.length > 1) {
				entryChoices = document.createElement("select");
				entryChoices.id = "input-selection-" + i;
				entryChoices.style.width = "100%";
				entryChoices.row = i;
				
				for(j = 0; j < output.mappings[i].options.length; j++) {
					option = document.createElement("option");
					option.number = j;
					option.value = output.mappings[i].options[j].id;
					option.id = "input-code-" + option.value;
					option.innerHTML = "<strong>" + output.mappings[i].options[j].inputName + "</strong>";
					$(entryChoices).append(option);
				}
				
				$(inputCell).append(entryChoices);
				$(entryChoices).change(function() {
					var codeURL = document.createElement("a");
					codeURL.target = "_blank";
					codeURL.href = locationURL + this.value;
					codeURL.innerHTML = "<strong>" + this.value +"</strong>";
					
					$("#code-" + this.row).empty();
					$("#code-" + this.row).append(codeURL);
					output.mappings[i].selectedOption = this.number;
					
					return;
				});
				
				entryChoices.value = output.mappings[i].options[0].id;
				codeURL.href = locationURL + output.mappings[i].options[0].id;
				codeURL.innerHTML = "<strong>" + output.mappings[i].options[0].id +"</strong>";
				output.mappings[i].selectedOption = 0;
			}
			else { //if(output.mappings[i].options.length < 1)
				entryChoices = document.createElement("input");
				entryChoices.id = "exception-" + i;
				entryChoices.row = i;
				entryChoices.type = "text";
				entryChoices.style.width = "100%";
				entryChoices.placeholder = "New input";
				$(inputCell).append(entryChoices);
				
				output.mappings[i].selectedOption = -1;
			}
			
			$(row).append(inputCell);
			$(row).append(codeCell);
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
		$(newSelector).change(function() {
			firstResolutionPass = true;
			return;
		});
		
		$("#search-priorities").append("<div id='priority-col-" + order + "' style='float: left;'><legend>Fetch ID Column</legend></div>");
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