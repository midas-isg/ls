$(document).ready(function Resolver() {
	var input,
		output = {headers: [], rows: []},
		fileReader = new FileReader();
	
	(function initialize() {
		$("#process-button").click(function() {
			readInputFile();
			
			return;
		});
		
		fileReader.onload = processCSVdata;
		
		function processCSVdata() {
			var resultLines = fileReader.result.split("\n"),
				currentLine = resultLines[0].split(","),
				i,
				j;
			
			$("#input-display").text(fileReader.result);
			
			for(j = 0; j < currentLine.length; j++) {
				output.headers.push(currentLine[j].replace(/"/g, ""));
			}
			output.headers.push("ls_id");
			
			for(i = 0; i < (resultLines.length - 1); i++) {
				currentLine = resultLines[i + 1].split(",");
				output.rows.push({columns: [], header: {}});
				
				for(j = 0; j < currentLine.length; j++) {
					output.rows[i].columns.push(currentLine[j].replace(/"/g, ""));
					output.rows[i].header[output.headers[j]] = output.rows[i].columns[j];
				}
				
				output.rows[i].columns.push(-1);
				output.rows[i].header[output.headers[j]] = output.rows[i].columns[j];
			}
			
			console.log(output);
			
			displayTable();
			
			function displayTable() {
				var i,
					j,
					row;
				
				for(i = 0; i < output.headers.length; i++) {
					$("#mapping-table thead tr").append("<td>" + output.headers[i] + "</td>");
				}
				
				for(i = 0; i < output.rows.length; i++) {
					row = document.createElement("tr");
					
					for(j = 0; j < output.rows[i].columns.length; j++) {
						$(row).append("<td>" + output.rows[i].columns[j] + "</td>");
					}
					
					$("#mapping-table tbody").append(row);
				}
				
				return;
			}
			
			return;
		}
		
		return;
	})();
	
	function readInputFile() {
		input = document.getElementById("csv-input").files[0];
		
		if(!input) {
			alert("Please select CSV file to upload");
			
			return;
		}
		
		fileReader.readAsText(input);
		
		return;
	}
	
	return;
});