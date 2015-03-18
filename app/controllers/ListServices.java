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
	public static Result findSuperTypes(){
		List<SuperType> result = LocationTypeRule.findSuperTypes();
		return okAsJson(result);
	}

	@Transactional
	public static Result findLocationTypeNames(String superTypeName){
		List<String> result = Wire.getTypes(superTypeName);
		return okAsJson(result);
	}

	private static Result okAsJson(Object result) {
		return ok(Json.toJson(result));
	}
	
	public static class Wire {
		public static List<String> getTypes(String superTypeName){
			List<LocationType> types = LocationTypeRule.findAllBySuperTypeName(superTypeName);
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
