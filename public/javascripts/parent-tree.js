var PARENT_TREE;

$(document).ready( function() {
	PARENT_TREE = new IndexingTermsTree();
	PARENT_TREE.initTree = function(picklistName, callbackFunction) {
		function loadTree(data) {
			var treeDiv = $("#parent-tree").fancytree({
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
					var id = encodeId(uri);
					
					if(node.selected) {
						var key = node.title;
						var type = node.data.type;
						var row = $('#indexingTerms > tbody:last').append(
								'<tr id="tr' + id + '"><td class="hide">'
										+ uri + '</td><td>' + style(type)
										+ '</td><td>' + style(key)
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
			PARENT_TREE.tree = treeDiv.fancytree("getTree");
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
					$("button#btnResetSearch").click();
					return;
				}
				
				n = PARENT_TREE.tree.filterNodes(match, leavesOnly);
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
				PARENT_TREE.tree.clearFilter();
			}).attr("disabled", true);
			
			return;
		}
		
		return;
	}
	
	function initialize() {
		var url = ausPath + "/api/au-tree";
		
		$.get(url, function(data, status) {
			treeData = data;
			console.log(data);
			PARENT_TREE.initInteractBetweenTreeAndTable("parent-list", function() {
				AU_COMPOSITE_TREE.initInteractBetweenTreeAndTable("au-list");
				
				return;
			});
			
			return;
		});
		
		return;
	}
	
	initialize();
	
	return;
});
