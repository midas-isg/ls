var SEARCH_RANK = (function() {
	function SearchRank() {
		return;
	}

	SearchRank.prototype.getScore = function(inputFeature, inputTarget, points) {
			var scoredPoints = 0,
				target = inputTarget.toLowerCase(),
				inputName = inputFeature.properties.name.toLowerCase(),
				searchIndex = inputName.search(target),
				lineage = inputFeature.properties.lineage,
				aliases = inputFeature.properties.otherNames,
				aliasName,
				j;

			if(searchIndex >= 0) {
				scoredPoints += points;

				if(inputName === target) {
					scoredPoints += (points >> 1);
				}
				else if(searchIndex === 0) {
					scoredPoints += (points >> 2);
				}
			}
			else {
				for(j = 0; j < aliases.length; j++) {
					aliasName = aliases[j].name.toLowerCase();
					searchIndex = aliasName.search(target);
					
					if(searchIndex >= 0) {
						if(aliasName === target) {
							scoredPoints += (points >> 1);
						}
						else if(searchIndex === 0) {
							scoredPoints += (points >> 2);
						}
					}
				}
				
				/*
				for(j = 0; j < lineage.length; j++) {
					searchIndex = lineage[j].name.toLowerCase().search(target);
					if(searchIndex >= 0) {
						scoredPoints += (points >> 1);

						if(inputName === target) {
							scoredPoints += (points >> 1);
						}
						else if(searchIndex === 0) {
							scoredPoints += (points >> 2);
						}
					}
				}
				*/
			}

			return scoredPoints;
		}

	return new SearchRank();
})();
