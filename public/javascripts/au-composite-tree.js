var AU_COMPOSITE_TREE = new IndexingTermsTree();

AU_COMPOSITE_TREE.initTree = function(picklistName, callbackFunction) {
	function loadTree(data) {
		var treeDiv = $("#au-composite-tree").fancytree({
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
					AU_COMPOSITE_TREE.changeState(event, data);
				}
				
				return true;
			},
			activate: function(event, data) {
				AU_COMPOSITE_TREE.changeState(event, data);
				
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
		AU_COMPOSITE_TREE.tree = treeDiv.fancytree("getTree");
		initFilterForTree();
		if(callbackFunction) {
			callbackFunction();
		}
		
		return;
	}
	
	loadTree(treeData);
	
	function initFilterForTree(){
		bindResetSearchButton();
		$("input[name=au-search]").keyup(function(e) {
			var n;
			var leavesOnly = !true;
			var match = $(this).val();
			
			if(e && e.which === $.ui.keyCode.ESCAPE || $.trim(match) === ""){
				$("button#resetCompositeSearchButton").click();
				return;
			}
			
			n = AU_COMPOSITE_TREE.tree.filterNodes(match, leavesOnly);
			$("button#resetCompositeSearchButton").attr("disabled", false);
			$("span#composite-matches").text("(" + n + " matches)");
		}).focus();
		
		return;
	}
	
	function bindResetSearchButton(){
		$("button#resetCompositeSearchButton").click(function(e){
			//e.preventDefault();
			$("input[name=au-search]").val("");
			$("span#composite-matches").text("");
			AU_COMPOSITE_TREE.tree.clearFilter();
		}).attr("disabled", true);
		
		return;
	}
	
	return;
}
