var ausPath = context;
var INDEXING_TERMS_TREE = new IndexingTermsTree();

function IndexingTermsTree() {
	this.tree = null;
	this.activatedNodes = [];
	
	return;
}

IndexingTermsTree.prototype.initInteractBetweenTreeAndTable = function(picklistName, callbackFunctionAfterTreeInitialized) {
	this.initTree(picklistName, callbackFunctionAfterTreeInitialized);
	//interactBetweenTreeAndTable();
	//makeTableSelectable();
	//bindAllButtons();
	this.resetIsAboutCheckboxes();
	
	return;
}

IndexingTermsTree.prototype.makeTableSelectable = function() {
	/* Get all rows from your 'table' but not the first one 
	 * that includes headers. */
	var rows = $('tr').not(':first');
	
	/* Create 'click' event handler for rows */
	rows.on('click', function(e) {
		var row = $(this);
		addHighlighForRow(rows, row, e);
		
	});
	
	/* This 'event' is used just to avoid that the table text 
	 * gets selected (just for styling). 
	 * For example, when pressing 'Shift' keyboard key and clicking 
	 * (without this 'event') the text of the 'table' will be selected.
	 * You can remove it if you want, I just tested this in 
	 * Chrome v30.0.1599.69 */
	$('#indexingTerms').bind('selectstart dragstart', function(e) { 
		e.preventDefault(); return false; 
	});
	
	return;
}

IndexingTermsTree.prototype.addHighlighForRow = function(rows, row, e) {
//function addHighlighForRow(rows, row, e){
	if ((e.ctrlKey || e.metaKey) || e.shiftKey) {
		row.addClass('highlight');
	} else {
		unHighLightAll(rows);
		row.addClass('highlight');
	}
	
	return;
}

IndexingTermsTree.prototype.unHighLightAll = function(rows) {
//function unHighLightAll(rows){
	rows.removeClass('highlight');
}

//IndexingTermsTree.prototype.interactBetweenTreeAndTable = function() {
function interactBetweenTreeAndTable(){
}

//IndexingTermsTree.prototype.bindAllButtons = function() {
function bindAllButtons(){
	bindRemoveButton();
	bindAddButton();
	bindClearButton();
}

//IndexingTermsTree.prototype.bindRemoveButton = function() {
function bindRemoveButton(){
	$("#remove_button").bind('click', function(e){
		e.preventDefault()
		var highlightedTr = "tr.highlight"
		$(highlightedTr).each(function() {
			  $this = $(this)
			  var key = $this.find("td:first").html();
			  var node = this.tree.getNodeByKey(key);
			  node.setSelected(false)
			});
	});
	
	return;
}

//IndexingTermsTree.prototype.bindAddButton = function() {
function bindAddButton(){
	$("#add_button").bind('click', function(e){
		e.preventDefault()
		if (this.activatedNodes){
			while (this.activatedNodes.length > 0){
				var node = this.activatedNodes.shift()
				node.extraClasses = "";
				select(node);
				node.render();
			}
		} 
	});
	
	return;
}

//IndexingTermsTree.prototype.select = function(node) {
function select(node){
	if (node.data.type)
		node.setSelected(true)
}

//IndexingTermsTree.prototype.bindClearButton = function() {
function bindClearButton(){
	$("#clear_button").click(function(e){
		e.preventDefault();
		resetIsAboutlist();
	})
}

//IndexingTermsTree.prototype.resetIsAboutlist = function() {
function resetIsAboutlist(){
	resetIsAboutCheckboxes();
	collapseAllFolders();
}

//IndexingTermsTree.prototype.collapseAllFolders = function() {
function collapseAllFolders(){
	if (this.tree) {
		this.tree.reload();
	}
	
	return;
}

IndexingTermsTree.prototype.resetIsAboutCheckboxes = function() {
	if (! this.tree) {
		return;
	}
	
	var nodes = this.tree.getSelectedNodes();
	for (var i in nodes){
		var node = nodes[i];
		node.setSelected(false);
	}
	
	return;
}

//IndexingTermsTree.prototype.style = function(text) {
function style(text){
	return '<font size="2">' + text + '</font>';
}

IndexingTermsTree.prototype.initTree = function(picklistName, callbackFunction) {
	var url = ausPath + "/api/locations/tree";
	var data;
	
	 data = [
		{"title":"abiotic ecosystem","key":null,"icon":false,"hideCheckbox":false,"folder":true,"unselectable":true,"expanded":false,"children":[
				{"title":"abiotic ecosystem of West Africa","key":"http://www.pitt.edu/obc/IDE_0000000090","icon":false,"hideCheckbox":false,"folder":null,"unselectable":false,"expanded":true,"children":null,"tooltip":"abiotic ecosystem:abiotic ecosystem of West Africa","type":"abiotic ecosystem","path":"abiotic ecosystem:abiotic ecosystem of West Africa"}
			],"tooltip":"abiotic ecosystem","type":null,"path":"abiotic ecosystem"},
		{"title":"biotic ecosystem","key":null,"icon":false,"hideCheckbox":false,"folder":true,"unselectable":true,"expanded":false,"children":[
			{"title":"biotic ecosystem of West Africa","key":"http://www.pitt.edu/obc/IDE_0000000053","icon":false,"hideCheckbox":false,"folder":null,"unselectable":false,"expanded":true,"children":null,"tooltip":"biotic ecosystem:biotic ecosystem of West Africa","type":"biotic ecosystem","path":"biotic ecosystem:biotic ecosystem of West Africa"}
		],"tooltip":"biotic ecosystem","type":null,"path":"biotic ecosystem"},
		{"title":"geographical region","key":null,"icon":false,"hideCheckbox":false,"folder":true,"unselectable":true,"expanded":false,"children":[
			{"title":"Earth","key":"http://purl.obolibrary.org/obo/GEO_000000345","icon":false,"hideCheckbox":false,"folder":null,"unselectable":false,"expanded":true,"children":null,"tooltip":"geographical region:Earth","type":"geographical region","path":"geographical region:Earth"},
			{"title":"region of Allegheny County, PA","key":"http://purl.obolibrary.org/obo/GEO_000000786","icon":false,"hideCheckbox":false,"folder":null,"unselectable":false,"expanded":true,"children":null,"tooltip":"geographical region:region of Allegheny County, PA","type":"geographical region","path":"geographical region:region of Allegheny County, PA"},
			{"title":"region of Florida","key":"http://purl.obolibrary.org/obo/GEO_000000380","icon":false,"hideCheckbox":false,"folder":null,"unselectable":false,"expanded":true,"children":null,"tooltip":"geographical region:region of Florida","type":"geographical region","path":"geographical region:region of Florida"},
			{"title":"region of Uganda","key":"http://purl.obolibrary.org/obo/GEO_000000554","icon":false,"hideCheckbox":false,"folder":null,"unselectable":false,"expanded":true,"children":null,"tooltip":"geographical region:region of Uganda","type":"geographical region","path":"geographical region:region of Uganda"},
			{"title":"region of United States of America","key":"http://purl.obolibrary.org/obo/GEO_000000587","icon":false,"hideCheckbox":false,"folder":null,"unselectable":false,"expanded":true,"children":null,"tooltip":"geographical region:region of United States of America","type":"geographical region","path":"geographical region:region of United States of America"},
			{"title":"UN Western Africa geographical region","key":"http://purl.obolibrary.org/obo/GEO_000000729","icon":false,"hideCheckbox":false,"folder":null,"unselectable":false,"expanded":true,"children":null,"tooltip":"geographical region:UN Western Africa geographical region","type":"geographical region","path":"geographical region:UN Western Africa geographical region"}
		],"tooltip":"geographical region","type":null,"path":"geographical region"},
		{"title":"host population","key":null,"icon":false,"hideCheckbox":false,"folder":true,"unselectable":true,"expanded":false,"children":[
			{"title":"Homo sapiens","key":null,"icon":false,"hideCheckbox":false,"folder":true,"unselectable":true,"expanded":true,"children":[
				{"title":"humans in Sierra Leone","key":"http://www.pitt.edu/obc/IDE_0000000075","icon":false,"hideCheckbox":false,"folder":null,"unselectable":false,"expanded":true,"children":null,"tooltip":"host population>Homo sapiens:humans in Sierra Leone","type":"host population","path":"host population>Homo sapiens:humans in Sierra Leone"},
				{"title":"humans in the United States","key":"http://www.pitt.edu/obc/IDE_0000000021","icon":false,"hideCheckbox":false,"folder":null,"unselectable":false,"expanded":true,"children":null,"tooltip":"host population>Homo sapiens:humans in the United States","type":"host population","path":"host population>Homo sapiens:humans in the United States"}
			],"tooltip":"host population>Homo sapiens","type":null,"path":"host population>Homo sapiens"}
		],"tooltip":"host population","type":null,"path":"host population"},
		{"title":"infection in ecosystem","key":null,"icon":false,"hideCheckbox":false,"folder":true,"unselectable":true,"expanded":false,"children":[
			{"title":"epidemic","key":"http://purl.obolibrary.org/obo/APOLLO_SV_00000298","icon":false,"hideCheckbox":false,"folder":null,"unselectable":false,"expanded":true,"children":[
				{"title":"H1N1 subtype, humans, global, 2009-2010","key":"http://www.pitt.edu/obc/IDE_0000000002","icon":false,"hideCheckbox":false,"folder":null,"unselectable":false,"expanded":true,"children":null,"tooltip":"infection in ecosystem>epidemic:H1N1 subtype, humans, global, 2009-2010","type":"infection in ecosystem","path":"infection in ecosystem>epidemic:H1N1 subtype, humans, global, 2009-2010"},
				{"title":"H2N2 subtype, humans, global, 1957 to 1958","key":"http://www.pitt.edu/obc/IDE_0000000098","icon":false,"hideCheckbox":false,"folder":null,"unselectable":false,"expanded":true,"children":null,"tooltip":"infection in ecosystem>epidemic:H2N2 subtype, humans, global, 1957 to 1958","type":"infection in ecosystem","path":"infection in ecosystem>epidemic:H2N2 subtype, humans, global, 1957 to 1958"},
				{"title":"H3N2 subtype, humans, global, 1968 to 1969","key":"http://www.pitt.edu/obc/IDE_0000000099","icon":false,"hideCheckbox":false,"folder":null,"unselectable":false,"expanded":true,"children":null,"tooltip":"infection in ecosystem>epidemic:H3N2 subtype, humans, global, 1968 to 1969","type":"infection in ecosystem","path":"infection in ecosystem>epidemic:H3N2 subtype, humans, global, 1968 to 1969"},
				{"title":"Sudan ebolavirus, humans, Sudan, 1976","key":"http://www.pitt.edu/obc/IDE_0000000081","icon":false,"hideCheckbox":false,"folder":null,"unselectable":false,"expanded":true,"children":null,"tooltip":"infection in ecosystem>epidemic:Sudan ebolavirus, humans, Sudan, 1976","type":"infection in ecosystem","path":"infection in ecosystem>epidemic:Sudan ebolavirus, humans, Sudan, 1976"},
				{"title":"Zaire ebolavirus, humans, Democratic Republic of Congo, 2014 to present","key":"http://www.pitt.edu/obc/IDE_0000000106","icon":false,"hideCheckbox":false,"folder":null,"unselectable":false,"expanded":true,"children":null,"tooltip":"infection in ecosystem>epidemic:Zaire ebolavirus, humans, Democratic Republic of Congo, 2014 to present","type":"infection in ecosystem","path":"infection in ecosystem>epidemic:Zaire ebolavirus, humans, Democratic Republic of Congo, 2014 to present"},
				{"title":"Zaire ebolavirus, humans, Guinea, 2014 to present","key":"http://www.pitt.edu/obc/IDE_0000000103","icon":false,"hideCheckbox":false,"folder":null,"unselectable":false,"expanded":true,"children":null,"tooltip":"infection in ecosystem>epidemic:Zaire ebolavirus, humans, Guinea, 2014 to present","type":"infection in ecosystem","path":"infection in ecosystem>epidemic:Zaire ebolavirus, humans, Guinea, 2014 to present"},
				{"title":"Zaire ebolavirus, humans, Liberia, 2014 to present","key":"http://www.pitt.edu/obc/IDE_0000000105","icon":false,"hideCheckbox":false,"folder":null,"unselectable":false,"expanded":true,"children":null,"tooltip":"infection in ecosystem>epidemic:Zaire ebolavirus, humans, Liberia, 2014 to present","type":"infection in ecosystem","path":"infection in ecosystem>epidemic:Zaire ebolavirus, humans, Liberia, 2014 to present"},
				{"title":"Zaire ebolavirus, humans, Nigeria, 2014-07-25 to 2014-09-05","key":"http://www.pitt.edu/obc/IDE_0000000055","icon":false,"hideCheckbox":false,"folder":null,"unselectable":false,"expanded":true,"children":null,"tooltip":"infection in ecosystem>epidemic:Zaire ebolavirus, humans, Nigeria, 2014-07-25 to 2014-09-05","type":"infection in ecosystem","path":"infection in ecosystem>epidemic:Zaire ebolavirus, humans, Nigeria, 2014-07-25 to 2014-09-05"},
				{"title":"Zaire ebolavirus, humans, Sierra Leone, 2014 to present","key":"http://www.pitt.edu/obc/IDE_0000000104","icon":false,"hideCheckbox":false,"folder":null,"unselectable":false,"expanded":true,"children":null,"tooltip":"infection in ecosystem>epidemic:Zaire ebolavirus, humans, Sierra Leone, 2014 to present","type":"infection in ecosystem","path":"infection in ecosystem>epidemic:Zaire ebolavirus, humans, Sierra Leone, 2014 to present"},
				{"title":"Zaire ebolavirus, humans, West Africa, 2014 to present","key":"http://www.pitt.edu/obc/IDE_0000000050","icon":false,"hideCheckbox":false,"folder":null,"unselectable":false,"expanded":true,"children":null,"tooltip":"infection in ecosystem>epidemic:Zaire ebolavirus, humans, West Africa, 2014 to present","type":"infection in ecosystem","path":"infection in ecosystem>epidemic:Zaire ebolavirus, humans, West Africa, 2014 to present"}],"tooltip":"infection in ecosystem>epidemic:epidemic","type":"infection in ecosystem","path":"infection in ecosystem>epidemic:epidemic"}
			],"tooltip":"infection in ecosystem","type":null,"path":"infection in ecosystem"},
		{"title":"infectious disease control strategy","key":null,"icon":false,"hideCheckbox":false,"folder":true,"unselectable":true,"expanded":false,"children":[
			{"title":"case quarantine control strategy","key":"http://purl.obolibrary.org/obo/APOLLO_SV_00000230","icon":false,"hideCheckbox":false,"folder":null,"unselectable":false,"expanded":true,"children":[],"tooltip":"infectious disease control strategy>case quarantine control strategy:case quarantine control strategy","type":"infectious disease control strategy","path":"infectious disease control strategy>case quarantine control strategy:case quarantine control strategy"},
			{"title":"individual treatment control strategy","key":null,"icon":false,"hideCheckbox":false,"folder":true,"unselectable":true,"expanded":true,"children":[
				{"title":"antiviral control strategy","key":"http://purl.obolibrary.org/obo/APOLLO_SV_00000181","icon":false,"hideCheckbox":false,"folder":null,"unselectable":false,"expanded":true,"children":[],"tooltip":"infectious disease control strategy>individual treatment control strategy>anti-infective individual treatment control strategy>antiviral control strategy:antiviral control strategy","type":"infectious disease control strategy","path":"infectious disease control strategy>individual treatment control strategy>anti-infective individual treatment control strategy>antiviral control strategy:antiviral control strategy"},
				{"title":"vaccination control strategy","key":"http://purl.obolibrary.org/obo/APOLLO_SV_00000136","icon":false,"hideCheckbox":false,"folder":null,"unselectable":false,"expanded":true,"children":[],"tooltip":"infectious disease control strategy>individual treatment control strategy>vaccination control strategy:vaccination control strategy","type":"infectious disease control strategy","path":"infectious disease control strategy>individual treatment control strategy>vaccination control strategy:vaccination control strategy"}
			],"tooltip":"infectious disease control strategy>individual treatment control strategy","type":null,"path":"infectious disease control strategy>individual treatment control strategy"},
			{"title":"school closure control strategy","key":"http://purl.obolibrary.org/obo/APOLLO_SV_00000123","icon":false,"hideCheckbox":false,"folder":null,"unselectable":false,"expanded":true,"children":[],"tooltip":"infectious disease control strategy>place closure control strategy>school closure control strategy:school closure control strategy","type":"infectious disease control strategy","path":"infectious disease control strategy>place closure control strategy>school closure control strategy:school closure control strategy"},
			{"title":"travel restriction control strategy","key":"http://purl.obolibrary.org/obo/APOLLO_SV_00000231","icon":false,"hideCheckbox":false,"folder":null,"unselectable":false,"expanded":true,"children":[],"tooltip":"infectious disease control strategy>travel restriction control strategy:travel restriction control strategy","type":"infectious disease control strategy","path":"infectious disease control strategy>travel restriction control strategy:travel restriction control strategy"}
		],"tooltip":"infectious disease control strategy","type":null,"path":"infectious disease control strategy"}
	];
	
	function loadTree(data) {
		var treeDiv = $("#tree").fancytree({
			extensions: ["filter"],
			filter: {
				mode: "hide"
			},
			checkbox: true,
			selectMode: 2,
			source: data,
			click: function(event, data) {
				var node = data.node;
				if(node.isActive()) {
					INDEXING_TERMS_TREE.changeState(event, data);
				}
				
				return true;
			},
			activate: function(event, data) {
				INDEXING_TERMS_TREE.changeState(event, data);
				
				return false;
			},
			dblclick: function(event, data) {
				var node = data.node;
				if(! node.unselectable) {
					node.setSelected(! (node.selected));
				}
				
				return false;
			},
			select : function(event, data) {
				var node = data.node;
				var uri = node.key;
				var id = encodeId(uri);
				
				if(node.selected) {
					var key = node.title;
					var type = node.data.type;
					var row = $('#indexingTerms > tbody:last').append(
							'<tr id="tr' + id + '"><td class="hide">'
									+ uri + '</td><td>' + style(type)
									+ '</td><td>' + style(key)
									+ '</td></tr>');
					
					MAP_DRIVER.addParent(node.key);
				}
				else {
					$('#tr' + id).remove();
					
					MAP_DRIVER.removeParent(node.key);
				}
				//makeTableSelectable(); //TODO TBR
			}
		});
		this.tree = treeDiv.fancytree("getTree");
		initFilterForTree();
		if(callbackFunction) {
			callbackFunction();
		}
		
		return;
	}
	
	$.get(url, function(data, status) {
		loadTree(data);
		
		return;
	});
	
	function initFilterForTree(){
		bindResetSearchButton();
		$("input[name=search]").keyup(function(e){
			var n,
				leavesOnly = !true,
				match = $(this).val();
	
			if(e && e.which === $.ui.keyCode.ESCAPE || $.trim(match) === ""){
				$("button#btnResetSearch").click();
				return;
			}
			n = this.tree.filterNodes(match, leavesOnly);
			$("button#btnResetSearch").attr("disabled", false);
			$("span#matches").text("(" + n + " matches)");
		}).focus();
		
		return;
	}
	
	function bindResetSearchButton(){
		$("button#btnResetSearch").click(function(e){
			e.preventDefault();
			$("input[name=search]").val("");
			$("span#matches").text("");
			this.tree.clearFilter();
		}).attr("disabled", true);
		
		return;
	}
	
	return;
}

IndexingTermsTree.prototype.changeState = function(event, data) {
	var node = data.node;
	
	if(!isMultiSelect(event)){
		while (this.activatedNodes.length > 0){
			var node1 = this.activatedNodes.pop();
			node1.extraClasses = "";
			node1.render();
		}
	}
	
	if(node.unselectable) {
		return;
	}
	
	if (node.extraClasses){
		node.extraClasses = "";
		var index = this.activatedNodes.indexOf(node);
		if(index > -1) {
			this.activatedNodes.splice(index, 1);
		}
	}
	else {
		node.extraClasses = "highlight";
		this.activatedNodes.push(node);
	}
	
	node.render();
	
	return;
}

//IndexingTermsTree.prototype.isMultiSelect = function(e) {
function isMultiSelect(e){
	return (e.ctrlKey || e.metaKey) || e.shiftKey;
}

//IndexingTermsTree.prototype.encodeId = function(id) {
function encodeId(id){
	return replaceAll(replaceAll(replaceAll(id, "\\.", "_"), "/", "_"), 
			":", "_")
}

//IndexingTermsTree.prototype.replaceAll = function(str, find, replace) {
function replaceAll(str, find, replace) {
  return str.replace(new RegExp(find, 'g'), replace);
}

//IndexingTermsTree.prototype.getAbouts  = function(node) {
function getAbouts(){
	var nodes = this.tree.getSelectedNodes();
	return $.map(nodes, function(node) {
		return node.key;
	});
}

//IndexingTermsTree.prototype.selectAbouts = function(abouts) {
function selectAbouts(abouts){
	if (abouts){
		for (i = 0, l = abouts.length; i < l; i++){
			clickIsAboutByValue(abouts[i]);
		}
	}
}

//IndexingTermsTree.prototype.clickIsAboutByValue = function(key) {
function clickIsAboutByValue(key){
	if (this.tree) {
		this.tree.getNodeByKey(key).setSelected(true);
	}
	
	return;
}
