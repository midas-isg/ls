describe("Back-End", ()=>{
	'use strict'; `comment: use ES6`;
	changeJasmineTimeout(1000);
	testApis();
	testPages();
	/*
POST          /api/locations                      controllers.LocationServices.create()
GET           /api/locations/:gid                 controllers.LocationServices.locations(gid:Long, format?="GeoJSON", maxExteriorRings:java.lang.Integer?=null)
PUT           /api/locations/:gid                 controllers.LocationServices.update(gid:Long)
DELETE        /api/locations/:gid                 controllers.LocationServices.delete(gid:Long)

POST          /api/locations-by-geometry          controllers.LocationServices.findByFeatureCollection(superTypeId:java.lang.Long ?= null, typeId:java.lang.Long ?= null)
	 */
	
	function testPages(){
		describe("Pages", ()=>{
			testPage("", "landing");
			testPage("read-only");
			testPage("browser");
			testPage("create");
			//TODO [JS throws exception => running struck]	testPage("api-docs");
		});
	}
	
	function testApis(){
		describe("APIs", ()=>{
			testApisGetReturnJSON();
		});
	}
	
	function testApisGetReturnJSON(){
		describe("GET methods returning JSON", ()=>{
			let validGid = 12;
			testApiGetReturnJSON("api/au-tree", 30000);
			testApiGetReturnJSON("api/locations/" + validGid);
			testApiGetReturnJSON("api/locations?q=abcd", 3000);
			//TODO [PersistenceException: org.hibernate.exception.GenericJDBCException: could not extract ResultSet] testApiGetReturnJSON("api/locations-by-coordinate?lat=0.1&long=0.2");
			testApiGetReturnJSON("api/locations-by-coordinate?lat=-73.8&long=9.1");
			testApiGetReturnJSON("api/super-types");
			testApiGetReturnJSON("api/location-types");
			testApiGetReturnJSON("api/unique-location-names?q=abc", 3000);
			testApiGetReturnJSON("api/geometry-metadata/" + validGid);
			testApiGetReturnJSON("api-docs.json");
			testApiGetReturnJSON("api-docs.json/api/locations");
		});
	}
	
	function changeJasmineTimeout(newVal){
		let original = jasmine.DEFAULT_TIMEOUT_INTERVAL;
		jasmine.DEFAULT_TIMEOUT_INTERVAL = newVal;
		console.log(`jasmine.DEFAULT_TIMEOUT_INTERVAL: changed from ${original} to ${jasmine.DEFAULT_TIMEOUT_INTERVAL}`);
		return original;
	}
	
	function testApiGetReturnJSON(path, timeout){
		describe(`when call ${path}`, ()=>{
			var originalTimeout;
			beforeAll(()=>{
				if (timeout)
					originalTimeout = changeJasmineTimeout(timeout);
			});
			var response = null;
			beforeEach((done)=>{
				get(path, ($0, $1, rsp)=>{
					response = rsp; 
					done();
				}, done);
			});
			let statusText = 'OK'
			it(`should return ${statusText} with a valid JSON`, ()=>{
				if (originalTimeout){
					changeJasmineTimeout(originalTimeout);
				}
				expect(response.statusText).toBe(statusText);
				expect(response.responseText).toBeJsonString();
				let contentType = response.getResponseHeader('Content-Type');
				expect(contentType).toStartWith('application/');
				expect(contentType).toContain('json');
			});
		});
	}

	function testPage(path, label){
		if (!label)
			label = `/${path}`;
		
		describe(`when browse to the ${label} page`, ()=>{
			var response = null;
			beforeEach((done)=>{
				get(path, ($0, $1, rsp)=>{
					response = rsp; 
					done();
				}, done);
			});
			let statusText = 'OK'
			it(`should return ${statusText} with a valid html`, ()=>{
				expect(response.statusText).toBe(statusText);
				expect(response.responseText).toBeHtmlString();
				let contentType = response.getResponseHeader('Content-Type');
				expect(contentType).toStartWith('text/html');
			});
		});
	}
	
	function get(path, success, error){
		let url = toUrl(path);
		$.ajax({type: 'GET', url, success, error});
	}
	
	function toUrl(path){
		var result = CONTEXT;
		if (path)
			result += `/${path}`; 
		return result;
	}
});
