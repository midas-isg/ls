var ausPath = context;
//INDEXING_TERMS_TREE = new IndexingTermsTree();
var treeData;

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
		//e.preventDefault();
		return false; 
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

IndexingTermsTree.prototype.bindAllButtons = function() {
	this.bindRemoveButton();
	this.bindAddButton();
	this.bindClearButton();
	
	return;
}

IndexingTermsTree.prototype.bindRemoveButton = function() {
	$("#remove_button").bind('click', function(e){
		//e.preventDefault()
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

IndexingTermsTree.prototype.bindAddButton = function() {
	$("#add_button").bind('click', function(e){
		//e.preventDefault()
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

IndexingTermsTree.prototype.select = function(node) {
	if(node.data.type) {
		node.setSelected(true);
	}
	
	return;
}

IndexingTermsTree.prototype.bindClearButton = function() {
	$("#clear_button").click(function(e){
		//e.preventDefault();
		this.resetIsAboutList();
	})
}

IndexingTermsTree.prototype.resetIsAboutList = function() {
	this.resetIsAboutCheckboxes();
	this.collapseAllFolders();
	
	return;
}

IndexingTermsTree.prototype.collapseAllFolders = function() {
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

IndexingTermsTree.prototype.style = function(text) {
	return '<font size="2">' + text + '</font>';
}

IndexingTermsTree.prototype.initTree = function(picklistName, callbackFunction) {
	var url = ausPath + "/api/au-tree";
	
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
					//INDEXING_TERMS_TREE.changeState(event, data);
				}
				
				return true;
			},
			activate: function(event, data) {
				//INDEXING_TERMS_TREE.changeState(event, data);
				
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
					
					MAP_DRIVER.addAUComponent(node.key);
				}
				else {
					$('#tr' + id).remove();
					
					MAP_DRIVER.removeAUComponent(node.key);
				}
				//makeTableSelectable(); //TODO TBR
			}
		});
		//INDEXING_TERMS_TREE.tree = treeDiv.fancytree("getTree");
		initFilterForTree();
		if(callbackFunction) {
			callbackFunction();
		}
		
		return;
	}
	
	//loadTree(treeData);
	/**/
	$.get(url, function(data, status) {
		loadTree(data);
		
		return;
	});
	/**/
	
	function initFilterForTree(){
		bindResetSearchButton();
		$("input[name=search]").keyup(function(e) {
			var n;
			var leavesOnly = !true;
			var match = $(this).val();
			
			if(e && e.which === $.ui.keyCode.ESCAPE || $.trim(match) === ""){
				$("button#btnResetSearch").click();
				return;
			}
			
			n = 0;//INDEXING_TERMS_TREE.tree.filterNodes(match, leavesOnly);
			$("button#btnResetSearch").attr("disabled", false);
			$("span#matches").text("(" + n + " matches)");
		}).focus();
		
		return;
	}
	
	function bindResetSearchButton(){
		$("button#btnResetSearch").click(function(e){
			//e.preventDefault();
			$("input[name=search]").val("");
			$("span#matches").text("");
			//INDEXING_TERMS_TREE.tree.clearFilter();
		}).attr("disabled", true);
		
		return;
	}
	
	return;
}

IndexingTermsTree.prototype.changeState = function(event, data) {
	var node = data.node;
	
	if(!this.isMultiSelect(event)){
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

IndexingTermsTree.prototype.isMultiSelect = function(e) {
	return (e.ctrlKey || e.metaKey) || e.shiftKey;
}

IndexingTermsTree.prototype.encodeId = function(id) {
	return this.replaceAll(this.replaceAll(this.replaceAll(id, "\\.", "_"), "/", "_"), 
		":", "_");
}

IndexingTermsTree.prototype.replaceAll = function(str, find, replace) {
	return str.replace(new RegExp(find, 'g'), replace);
}

IndexingTermsTree.prototype.getAbouts  = function(node) {
	var nodes = this.tree.getSelectedNodes();
	
	return $.map(nodes, function(node) {
		return node.key;
	});
}

IndexingTermsTree.prototype.selectAbouts = function(abouts) {
	if(abouts) {
		for (i = 0, l = abouts.length; i < l; i++){
			this.clickIsAboutByValue(abouts[i]);
		}
	}
	
	return;
}

IndexingTermsTree.prototype.clickIsAboutByValue = function(key) {
	if(this.tree) {
		this.tree.getNodeByKey(key).setSelected(true);
	}
	
	return;
}
