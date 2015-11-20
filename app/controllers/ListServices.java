package controllers;

import interactors.LocationTypeRule;

import java.util.ArrayList;
import java.util.List;

import play.db.jpa.Transactional;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import dao.entities.LocationType;
import dao.entities.SuperType;

public class ListServices extends Controller {
	@Transactional
	public Result findSuperTypes(){
		List<SuperType> result = LocationTypeRule.findSuperTypes();
		return okAsJson(result);
	}

	@Transactional
	public Result findLocationTypes(Long superTypeId){
		List<LocationType> types = LocationTypeRule.findAllBySuperTypeId(superTypeId);
		return okAsJson(types);
	}

	private Result okAsJson(Object result) {
		return ok(Json.toJson(result));
	}
	
	public static class Wire {
		public static List<String> findLocationTypeNamesBySuperTypeId(Long superTypeId){
			List<LocationType> types = LocationTypeRule.findAllBySuperTypeId(superTypeId);
			List<String> names = toNames(types);
			return names;
		}

		private static List<String> toNames(List<LocationType> types) {
			List<String> names = new ArrayList<>();
			for (LocationType t : types){
				names.add(t.getName());
			}
			return names;
		}
	}
	
}
