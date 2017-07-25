#search-button {
	vertical-align: inherit;
}

#leaflet-draw-marker a:hover{
	cursor: default;
	background-color: transparent;
	border-bottom-width: 1px;
	border-bottom-style: solid;
	border-bottom-color: rgba(0, 0, 0, 0.1);
}

@media only screen and (min-width: 768px) {
	#map-container {
		width: 49.75%;
		margin-right: 0.5%;
	}

	#results-output {
		width: 49.75%;
	}
}
@media only screen and (max-width: 767px) {
	#map-container {
		width: 100%;
	}

	#results-output {
		width: 100%;
		margin-bottom: 5px;
	}
}

#result-count {
	border-radius: 5px 5px 0px 0px;
	background-color: #000000;
	color: #00FFFF;
}

#result table {
	display: table;
	width: 100%;
}

#result table thead,
#result table tbody {
	width: 100%;
	float: left;
}

#result table tbody {
	border-radius: 5px;
	overflow: auto;
	max-height: 500px;
}

#result table tr {
	width: 100%;
	display: table;
}

#result tbody tr:nth-child(even) {
	background-color: #F0FFFF;
}

#result tbody tr:nth-child(odd) {
	background-color: #FFFFFF;
}

#result table thead tr {
	background-color: #F0FFFF;
	border-bottom-color: rgb(221, 221, 221);
	border-bottom-width: 1px;
	border-bottom-style: solid;
}

#result table tbody tr td,
#result table thead tr th {
	padding: 0px;
	border-bottom: 0px;
}

#result table thead .location-col {
	width: 38%;
}

#result table tbody .location-col {
	width: 40%;
}

#result table .type-col {
	width: 20%;
}

#result table .within-col {
	width: 40%;
}