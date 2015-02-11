var PARENT_TREE = new IndexingTermsTree();

PARENT_TREE.initTree = function(picklistName, callbackFunction) {
	var thisTreeWidget = this;
	
	function loadTree(data) {
		var treeDiv = $("#parent-tree").fancytree({
			extensions: ["filter"],
			filter: {
				mode: "hide"
			},
			checkbox: true,
			selectMode: 1,
			source: data,
			click: function(event, data) {
				var node = data.node;
				if(node.isActive()) {
					PARENT_TREE.changeState(event, data);
				}
				
				return true;
			},
			activate: function(event, data) {
				PARENT_TREE.changeState(event, data);
				
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
				var id = PARENT_TREE.encodeId(uri);
				
				if(node.selected) {
					var key = node.title;
					var type = node.data.type;
					var row = $('#indexingTerms > tbody:last').append(
							'<tr id="tr' + id + '"><td class="hide">'
									+ uri + '</td><td>' + PARENT_TREE.style(type)
									+ '</td><td>' + PARENT_TREE.style(key)
									+ '</td></tr>');
					
					MAP_DRIVER.parent = node.key;
				}
				else {
					$('#tr' + id).remove();
					
					MAP_DRIVER.parent = null;
				}
				//makeTableSelectable(); //TODO TBR
			}
		});
		
		thisTreeWidget.tree = treeDiv.fancytree("getTree");
		initFilterForTree();
		if(callbackFunction) {
			callbackFunction();
		}
		
		return;
	}
	
	loadTree(treeData);
	
	function initFilterForTree(){
		bindResetSearchButton();
		$("input[name=search]").keyup(function(e) {
			var n;
			var leavesOnly = !true;
			var match = $(this).val();
			
			if(e && e.which === $.ui.keyCode.ESCAPE || $.trim(match) === ""){
				$("button#resetParentSearchButton").click();
				return;
			}
			
			n = thisTreeWidget.tree.filterNodes(match, leavesOnly);
			$("button#resetParentSearchButton").attr("disabled", false);
			$("span#parent-matches").text("(" + n + " matches)");
		}).focus();
		
		return;
	}
	
	function bindResetSearchButton(){
		$("button#resetParentSearchButton").click(function(e){
			//e.preventDefault();
			$("input[name=search]").val("");
			$("span#parent-matches").text("");
			thisTreeWidget.tree.clearFilter();
		}).attr("disabled", true);
		
		return;
	}
	
	return;
}
