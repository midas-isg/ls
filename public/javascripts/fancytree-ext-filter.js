;(function($, window, document, undefined) {

	"use strict";


	/***************************************************************************
	 * Private functions and variables
	 */

	function _escapeRegex(str){
		/*jshint regexdash:true */
		return (str + "").replace(/([.?*+\^\$\[\]\\(){}|-])/g, "\\$1");
	}
	
	$.ui.fancytree._FancytreeClass.prototype._applyFilterImpl 
	= function(filter, branchMode, leavesOnly){
		var match, re,
			count = 0,
			hideMode = this.options.filter.mode === "hide";
			// leavesOnly = !branchMode && this.options.filter.leavesOnly;
		leavesOnly = !!leavesOnly && !branchMode;

		// Default to 'match title substring (not case sensitive)'
		if(typeof filter === "string"){
			/*match = _escapeRegex(filter);
			re = new RegExp(".*" + match + ".*", "i");*/
			match = filter + "";
			filter = function(node){
				//return !!re.exec(node.title);
				var path = node.data.path.toLowerCase();
				var terms = match.toLowerCase().split(" ");
				for (var i in terms) {
					if (path.search(terms[i]) < 0)
						return false;
				}
				return true; 
			};
		}

		this.enableFilter = true;
		this.$div.addClass("fancytree-ext-filter");
		if( hideMode ){
			this.$div.addClass("fancytree-ext-filter-hide");
		} else {
			this.$div.addClass("fancytree-ext-filter-dimm");
		}
		// Reset current filter
		this.visit(function(node){
			delete node.match;
			delete node.subMatch;
		});
		// Adjust node.hide, .match, .subMatch flags
		this.visit(function(node){
			if ((!leavesOnly || node.children == null) && filter(node)) {
				count++;
				node.match = true;
				node.visitParents(function(p){
					p.subMatch = true;
				});
				if( branchMode ) {
					node.visit(function(p){
						p.match = true;
					});
					return "skip";
				}
			}
		});
		this.render();
		return count;
	};

	/**
	 * [ext-filter] Dimm or hide nodes.
	 *
	 * @param {function | string} filter
	 * @param {boolean} [leavesOnly=false]
	 * @returns {integer} count
	 * @alias Fancytree#filterNodes
	 * @requires jquery.fancytree.filter.js
	 */
	$.ui.fancytree._FancytreeClass.prototype.filterNodes 
	= function(filter, leavesOnly){
		return this._applyFilterImpl(filter, false, leavesOnly);
	};

	/**
	 * [ext-filter] Dimm or hide whole branches.
	 *
	 * @param {function | string} filter
	 * @returns {integer} count
	 * @alias Fancytree#filterBranches
	 * @requires jquery.fancytree.filter.js
	 */
	$.ui.fancytree._FancytreeClass.prototype.filterBranches = function(filter){
		return this._applyFilterImpl(filter, true, null);
	};


	/**
	 * [ext-filter] Reset the filter.
	 *
	 * @alias Fancytree#clearFilter
	 * @requires jquery.fancytree.filter.js
	 */
	$.ui.fancytree._FancytreeClass.prototype.clearFilter = function(){
		this.visit(function(node){
			delete node.match;
			delete node.subMatch;
		});
		this.enableFilter = false;
		this.$div.removeClass("fancytree-ext-filter " +
				"fancytree-ext-filter-dimm fancytree-ext-filter-hide");
		this.render();
	};


	/*******************************************************************************
	 * Extension code
	 */
	$.ui.fancytree.registerExtension({
		name: "filter",
		version: "0.2.0",
		// Default options for this extension.
		options: {
			mode: "dimm"
//			leavesOnly: false
		},
		treeInit: function(ctx){
			this._super(ctx);
		},
		nodeRenderStatus: function(ctx) {
			// Set classes for current status
			var res,
				node = ctx.node,
				tree = ctx.tree,
				$spanParent = $(node[tree.statusClassPropName]).parent();

			res = this._super(ctx);
			// nothing to do, if node was not yet rendered
			if( !$spanParent.length || !tree.enableFilter ) {
				return res;
			}
			$spanParent
				//.toggleClass("fancytree-match", !!node.match)
				//.toggleClass("fancytree-submatch", !!node.subMatch)
				.toggleClass("fancytree-hide", !(node.match || node.subMatch));

			return res;
		}
	});
}(jQuery, window, document));
