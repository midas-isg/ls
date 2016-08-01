
import static org.fest.assertions.Assertions.assertThat;
import static play.mvc.Http.HeaderNames.LOCATION;
import static play.test.Helpers.HTMLUNIT;
import static play.test.Helpers.contentAsString;
import static play.test.Helpers.fakeApplication;
import static play.test.Helpers.route;
import static play.test.Helpers.running;
import static play.test.Helpers.testServer;
import interactors.GeoJSONParser;
import interactors.GeoJsonRule;
import interactors.GeometryRule;
import interactors.KmlRule;
import interactors.LocationRule;
import interactors.LocationTypeRule;
import interactors.XmlRule;

import java.io.File;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;

import models.geo.Feature;
import models.geo.FeatureCollection;
import models.geo.FeatureGeometry;

import org.fest.assertions.StringAssert;
import org.fluentlenium.core.Fluent;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import play.Configuration;
import play.api.mvc.Call;
import play.db.jpa.JPA;
import play.libs.F.Callback;
import play.libs.Json;
import play.mvc.Http.RequestBuilder;
import play.mvc.Http.Status;
import play.mvc.Result;
import play.test.Helpers;
import play.test.TestBrowser;
import play.test.TestServer;
import play.test.WithApplication;
import play.twirl.api.Content;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.MultiPolygon;

import controllers.ApolloLocationServices;
import controllers.LocationServices;
import controllers.routes;
import dao.entities.Location;
import dao.entities.LocationType;

public class IntegrationTest extends WithApplication {
	private static String context = "http://localhost:3333/ls";
	private TestServer testServer = null; 
	private static Map<String, Object> additionalConfiguration = null;
	
	@BeforeClass
	public static void initTestConf(){
		Config config = ConfigFactory.parseFile(new File("conf/test.conf"));
		Configuration  additionalConfigurations = new Configuration(config);
		additionalConfiguration = additionalConfigurations.asMap();
	}
	
	@Before
	public void init(){
		testServer = testServer(3333, fakeApplication(additionalConfiguration));
	}

	private EntityManager initEntityManager() {
		EntityManager em = JPA.em("default");
    	JPA.bindForSync(em);
		return em;
	}
	
	@Test
    public void testWithJpaThenRollback() {
		running(testServer, HTMLUNIT, new Callback<TestBrowser>() {
            public void invoke(TestBrowser browser) throws Exception {
        		EntityManager em = initEntityManager();
        		EntityTransaction transaction = em.getTransaction();
				transaction.begin();
				transaction.setRollbackOnly();
				
				testCreateComposite();
				testLocationTypes(browser);
				testGetSuperTypes(browser);
				testFindLocationsByFeatureCollection();
				testCreateEzFromAu();
				testLocationType_PumaComposedOfCensusTract();
				testMaxExteriorRings(browser);
				testGeoMetadata(browser);
				tesCrudAu();
				testApolloLocation();
				tesCrudEz();
				tesCreateAuWithInvalidGeom();
				tesCrudAuWithDuplication();
				
				transaction.rollback();
            }
        });
    }
	
	private void testCreateComposite() throws Exception {
		String fileName = "test/CompositeTestPUMA.geojson";
		FeatureCollection fc = readFeatureCollectionFromFile(fileName);
		Feature f0 = fc.getFeatures().get(0);
		assertThat(f0.getId()).isEqualTo("72676");
		Location l = GeoJsonRule.asLocation(fc);
		Geometry geometry = l.getGeometry().getShapeGeom();
		String type = geometry.getGeometryType();
		assertThat(type).isEqualTo(MultiPolygon.class.getSimpleName());
		MultiPolygon mp = (MultiPolygon)geometry;
		int expectedNumGeometries = 1;
		assertThat(mp.getNumGeometries()).isEqualTo(expectedNumGeometries);
		
		String json = KmlRule.getStringFromFile(fileName);
        JsonNode node = Json.parse(json);
        String path = "/api/locations";
		Result result = request(routes.LocationServices.create(), node);

        assertThat(result.status()).isEqualTo(Status.CREATED);
        String location = result.header(LOCATION);
        assertThat(location).containsIgnoringCase(path);
        long gid = toGid(location);
        Location readLocation = LocationRule.read(gid);
        assertThat(readLocation.getGid()).isEqualTo(gid);
        MultiPolygon readMp = (MultiPolygon)readLocation.getGeometry().getShapeGeom();
		assertThat(readMp.getNumGeometries()).isEqualTo(expectedNumGeometries);
		assertThat(readLocation.getLocationsIncluded()).isNotEmpty();

        String deletePath = path + "/" + gid;
        Result deleteResult = request(routes.LocationServices.delete(gid), node);
        assertThat(deleteResult.status()).isEqualTo(Status.NO_CONTENT);
		assertThat(deleteResult.header(LOCATION)).endsWith(deletePath);
	}

	private Result request(final Call call, JsonNode body) {
		final RequestBuilder requestBuilder = Helpers.fakeRequest(call);
		if (body != null)
			requestBuilder.bodyJson(body);
        return route(requestBuilder);
	}

	private void testGetSuperTypes(TestBrowser browser) {
		Fluent page = browser.goTo(context+"/api/super-types");
		String json = page.pageSource();
		assertJsonNameValue(json, "Administrative Unit");
		assertJsonNameValue(json, "Epidemic Zone");
		assertJsonArrayAsNonEmpty(json);
	}

	public ArrayNode assertJsonArrayAsNonEmpty(String json) {
		JsonNode node = Json.parse(json);
		assertThat(node.getNodeType()).isEqualTo(JsonNodeType.ARRAY);
		assertThat(node.size()).isGreaterThan(0);
		return (ArrayNode)node;
	}

	public void assertJsonNameValue(String json, String expected) {
		assertThat(json).containsIgnoringCase(asJsonPair("name", expected));
	}

	public String asJsonPair(String key, String value) {
		return quote(key) + ":" + quote(value);
	}
	
	private String quote(String text) {
		return "\"" + text + "\"";
	}

	private void testFindLocationsByFeatureCollection() throws Exception {
		testFindLocationsByPoint();
		testFindLocationsByPolygon();
	}
	
	private void testFindLocationsByPoint() throws Exception {
		String text = KmlRule.getStringFromFile("test/geojson/point_test.geojson");
		String geojson = toGeometryString(text);
		
		List<BigInteger> list = GeometryRule.findGidsByGeometry(geojson, null, null);
		assertThat(list).isNotEmpty();
		BigInteger gid = list.get(0);
		
        JsonNode node = Json.parse(text);
        final Call call = routes.LocationServices.findByFeatureCollection(null, null, true);
		final Result postResult = request(call, node);
        assertThat(postResult.status()).isEqualTo(Status.OK);
        assertThat(postResult.contentType()).isEqualTo("application/vnd.geo+json");
        String jsonResult = contentAsString(postResult);
        JsonNode resultNode = Json.parse(jsonResult);
        assertThat(resultNode.get("type").asText()).isEqualTo("FeatureCollection");
        assertThat(jsonResult).contains("\"gid\":\""+gid+"\"");
	}

	private void testFindLocationsByPolygon() throws Exception {
		long supertTypeIdAdministrativeUnit = 3L;
		String text = KmlRule.getStringFromFile("test/circleCenteredAtDbmi.geojson");
		String geojson = toGeometryString(text);
		
		long superTypeIdComposite = 2L;
		List<BigInteger> list = GeometryRule.findGidsByGeometry(geojson, superTypeIdComposite, null);
		assertGids(list, 84687L);

		list = GeometryRule.findGidsByGeometry(geojson, 1L, null);
		assertGids(list);

		list = GeometryRule.findGidsByGeometry(geojson, supertTypeIdAdministrativeUnit, null);
		assertGids(list, new long[] {1169, 1213});

		list = GeometryRule.findGidsByGeometry(geojson, 4L, null);
		assertGids(list, new long[] {67079, 66676, 67136, 67173, 66735, 66822, 
				67019, 66820, 67081, 66664, 67117, 67111});

        JsonNode node = Json.parse(text);
        final Call call = routes.LocationServices.findByFeatureCollection(supertTypeIdAdministrativeUnit, null, true);
		Result postResult = request(call, node);
        assertThat(postResult.status()).isEqualTo(Status.OK);
        assertThat(postResult.contentType()).isEqualTo("application/vnd.geo+json");
        String jsonResult = contentAsString(postResult);
        JsonNode resultNode = Json.parse(jsonResult);
        assertThat(resultNode.get("type").asText()).isEqualTo("FeatureCollection");
        assertThat(jsonResult).contains("\"gid\":\"1169\"");
	}

	private String toGeometryString(String json) throws Exception {
		FeatureCollection fc = readFeatureCollection(json);
		Feature feature0 = fc.getFeatures().get(0);
		FeatureGeometry geometry = feature0.getGeometry();
		String geo = Json.toJson(geometry).toString();
		return geo;
	}

	private void assertGids(List<BigInteger> list, long... expects) {
		List<Long> actual = toList(list);
		for (long expect : expects)
			assertThat(actual).contains(expect);
	}

	private void testCreateEzFromAu() throws Exception {
		String fileName = "test/EzFromAu.geojson";
		FeatureCollection fc = readFeatureCollectionFromFile(fileName);
		Feature f0 = fc.getFeatures().get(0);
		assertThat(f0.getId()).isEqualTo("11");
		Location l = GeoJsonRule.asLocation(fc);
		Geometry geometry = l.getGeometry().getShapeGeom();
		String type = geometry.getGeometryType();
		assertThat(type).isEqualTo(MultiPolygon.class.getSimpleName());
		MultiPolygon mp = (MultiPolygon)geometry;
		int expectedNumGeometries = 80;
		assertThat(mp.getNumGeometries()).isEqualTo(expectedNumGeometries);
		
		String json = KmlRule.getStringFromFile(fileName);
        JsonNode node = Json.parse(json);
        String path = "/api/locations";

        Result result = request(routes.LocationServices.create(), node);
        assertThat(result.status()).isEqualTo(Status.CREATED);
        String location = result.header(LOCATION);
        assertThat(location).containsIgnoringCase(path);
        long gid = toGid(location);
        Location readLocation = LocationRule.read(gid);
        assertThat(readLocation.getGid()).isEqualTo(gid);
        MultiPolygon readMp = (MultiPolygon)readLocation.getGeometry().getShapeGeom();
		assertThat(readMp.getNumGeometries()).isEqualTo(expectedNumGeometries);
		assertThat(readLocation.getLocationsIncluded()).isEmpty();

		String deletePath = path + "/" + gid;
		Result deleteResult = request(routes.LocationServices.delete(gid));
        assertThat(deleteResult.status()).isEqualTo(Status.NO_CONTENT);
		assertThat(deleteResult.header(LOCATION)).endsWith(deletePath);
	}
	
	private long toGid(String url) {
		String[] tokens = url.split("/");
		String gid = tokens[tokens.length - 1];
		return Long.parseLong(gid);
	}

	private List<Long> toList(List<BigInteger> list) {
		List<Long> actual = new ArrayList<>();
		for (BigInteger bi : list){
			actual.add(bi.longValue());
		}
		return actual;
	}

	private FeatureCollection readFeatureCollection(String json) throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		JsonNode node = mapper.readTree(json);
		return GeoJSONParser.parse(node);
	}

	private void testLocationType_PumaComposedOfCensusTract() {
		String pumaName = "PUMA 2010";
		LocationType puma = LocationTypeRule.findByName(pumaName);
		assertThat(puma.getName()).isEqualToIgnoringCase(pumaName);
		assertThat(puma.getComposedOf().getName()).isEqualToIgnoringCase("Census Tract 2010");
	}
	
	private void testLocationTypes(TestBrowser browser) {
		String baseUrl = context + "/api/location-types";
		List<String> allTypes = getTypeNames(browser, baseUrl);
		assertThat(allTypes).doesNotHaveDuplicates();
		
		String queryUrl = baseUrl + "?superTypeId=";
		
		int ez = 1;
		String ez1 = "Epidemic Zone";
		int composite = 2;
		String composite1 = "PUMA 2010";
		int au = 3;
		String au1 = "Country";
		int census = 4;
		String census1 = "Census Tract 2010";

		List<String> ezTypes = getTypeNames(browser, queryUrl + ez);
		assertThat(ezTypes).contains(ez1);
		assertThat(ezTypes).excludes(au1, composite1, census1);
		assertContainsAll(allTypes, ezTypes);
		
		List<String> compositeTypes = getTypeNames(browser, queryUrl + composite);
		assertThat(compositeTypes).contains(composite1);
		assertThat(compositeTypes).excludes(au1, ez1, census1);
		assertContainsAll(allTypes, compositeTypes);
		
		List<String> auTypes = getTypeNames(browser, queryUrl + au);
		assertThat(auTypes).contains(au1);
		assertThat(auTypes).excludes(composite1, census1, ez1);
		assertContainsAll(allTypes, auTypes);
		
		List<String> censusTypes = getTypeNames(browser, queryUrl + census);
		assertThat(censusTypes).contains(census1);
		assertThat(censusTypes).excludes(composite1, au1, ez1);
		assertContainsAll(allTypes, censusTypes);
	}

	public List<String> getTypeNames(TestBrowser browser, String url) {
		String auJson = browser.goTo(url).pageSource();
		ArrayNode auArray = assertJsonArrayAsNonEmpty(auJson);
		List<String> auTypes = toListByName(auArray);
		return auTypes;
	}

	private void assertContainsAll(List<?> actual, List<?> expected) {
		for (Object obj: expected){
			assertThat(actual).contains(obj);
		}
	}

	public List<String> toListByName(ArrayNode array) {
		List<String> list = new ArrayList<>();
		for (JsonNode node : array){
			JsonNode nameNode = node.get("name");
			if (nameNode == null || nameNode.isNull())
				throw new RuntimeException("Unexpected null at key=name of " + node);
			list.add(nameNode.textValue());
		}
		return list;
	}

	private void testMaxExteriorRings(TestBrowser browser) {
		long gid = 11;
		
		Location location = LocationRule.simplifyToMaxExteriorRings(gid, null);
		int n = getNumExteriorRings(location);
		assertThat(n).isEqualTo(80);
		
		Location location1 = LocationRule.simplifyToMaxExteriorRings(gid, 100);
		assertThat(location1).isEqualTo(location);
		
		int maxExteriorRings = 78;
		int expectedN = assertMaxExteriorRings(gid, maxExteriorRings);
		assertMaxExteriorRings(gid, 2);
		assertMaxExteriorRings(gid, 1);
		
		String template = context+"/api/locations/%d.xml?maxExteriorRings=";
		String url = String.format(template, gid) + maxExteriorRings;
		String xml = browser.goTo(url).pageSource();
		assertXmlWithNumberOfTag(xml, expectedN);
	}

	private void assertXmlWithNumberOfTag(String xml, int expectedN) {
		String tag = "<linearRing>";
		String description = "the number of occurrences of '" + tag +"'";
		assertThat(count(xml, tag)).as(description).isEqualTo(expectedN);
	}

	private int count(String xml, String tag) {
		return xml.split(tag, -1).length-1;
	}

	private int assertMaxExteriorRings(long gid, int max) {
		Location location = LocationRule.simplifyToMaxExteriorRings(gid, max);
		int n = getNumExteriorRings(location);
		assertThat(n).isLessThanOrEqualTo(max);
		return n;
	}

	private int getNumExteriorRings(Location location) {
		return location.getGeometry().getShapeGeom().getNumGeometries();
	}

	private void testGeoMetadata(TestBrowser browser) {
		Result result1 = request(routes.LocationServices.getGeometryMetadata(11, null));
		assertThat(result1.status()).isEqualTo(Status.OK);
		String content = contentAsString(result1);
		JsonNode json = Json.parse(content);
		assertThat(json.findValue("tolerance").isNull()).isTrue();
		int nGeo1 = json.findValue("numGeometries").intValue();
		
		Result result2 = request(routes.LocationServices.getGeometryMetadata(1, 0.5));
		JsonNode json2 = Json.parse(contentAsString(result2));
		assertThat(json2.findValue("tolerance").doubleValue()).isPositive();
		int nGeo2 = json2.findValue("numGeometries").intValue();
		assertThat(nGeo2).isPositive();
		assertThat(nGeo2).isLessThan(nGeo1);
		
		double t = 0.001234567;
		String template = context+"/api/geometry-metadata/%d?tolerance=";
		String url = String.format(template, 1) + t;
		String jsonText = browser.goTo(url).pageSource();
		assertTolerance(jsonText, t);
	}

	private Result request(final Call call) {
		return request(call, null);
	}

	private void assertTolerance(String json, double expectedT) {
		String expected = "\"tolerance\":" + expectedT;
		StringAssert assertThat = assertThat(json);
		assertThat.contains(expected);
		assertThat.contains("\"numGeometries\":6,");
	}

	private void testApolloLocation() throws Exception {
		long gid = 3;
		Location location = LocationRule.read(gid);
		String xml = ApolloLocationServices.Wire.asXml(location);
		edu.pitt.apollo.types.v3_0_0.Location l = deserializeLocation(xml);
		assertThat(l.getApolloLocationCode()).isEqualTo("" + gid);
		
		String auXml = KmlRule.getStringFromFile("test/2.xml");
		l = deserializeLocation(auXml);
		assertThat(l.getApolloLocationCode()).isEqualTo("2");
	}

	private edu.pitt.apollo.types.v3_0_0.Location deserializeLocation(String xml) {
		return  (edu.pitt.apollo.types.v3_0_0.Location) XmlRule.toObject(xml);
	}
	
	//@Test
    public void testWithJpaThenCommit() {
		running(testServer, HTMLUNIT, new Callback<TestBrowser>() {
            public void invoke(TestBrowser browser) throws Exception {
        		EntityManager em = initEntityManager();
        		em.getTransaction().begin();
        		testDeleteWithGid();
        		em.getTransaction().commit();
            }
        });
    }
	
    @Test
    public void testBrowser() {
		running(testServer, HTMLUNIT, new Callback<TestBrowser>() {
            public void invoke(TestBrowser browser) {
        		browser.goTo(context + "/api/super-types");
                String expected = " ";//"apollo location services";
				assertThat(browser.pageSource()).contains(expected);
                renderTemplate();
            }
        });
    }
    
    private void renderTemplate() {
        Content html = views.html.create.render("ALS is ready.", "");
        assertThat(html.contentType()).isEqualTo("text/html");
        assertThat(contentAsString(html)).contains("ALS is ready.");
    }

	private void tesCrudEz() throws Exception {
		String fileName = "test/EzMaridi.geojson";
		JsonNode json = readJsonNodeFromFile(fileName);
    	
    	FeatureCollection fc = GeoJSONParser.parse(json);
    	long gid = LocationServices.Wire.create(fc);
		assertThat(gid).isPositive();
		Location readLocation = LocationRule.read(gid);
		Location expectedLocation = asLocation(fc, gid);
		assertISEqualTo(readLocation, expectedLocation);
		assertThat(readLocation.getData().getLocationType().getName()).isEqualTo("Epidemic Zone");
		testRead(gid, fc);
		Long deletedGid = deleteTogetherWithAllGeometries(gid);
		assertThat(deletedGid).isEqualTo(gid);
		Location deletedLocation = LocationRule.read(gid);
		assertThat(deletedLocation).isNull();
	}

	private FeatureCollection asFeatureCollection(long gid) throws Exception {
		Location location = LocationServices.Wire.read(gid);
		return LocationServices.Wire.asFeatureCollection(location);
	}

	private void tesCrudAu() throws Exception {
    	String fileName = "test/AuMaridiTown.geojson";
		testCrud(fileName);
		
		String json = KmlRule.getStringFromFile(fileName);
        JsonNode node = Json.parse(json);
        String path = "/api/locations";

        Result result = request(routes.LocationServices.create(), node);
        assertThat(result.status()).isEqualTo(Status.CREATED);
        String location = result.header(LOCATION);
        assertThat(location).containsIgnoringCase(path);
        long gid = toGid(location);
		testRead(gid, null);
        Result deleteResult = request(routes.LocationServices.delete(gid));
        assertThat(deleteResult.status()).isEqualTo(Status.NO_CONTENT);
        String deletePath = path + "/" + gid;
		assertThat(deleteResult.header(LOCATION)).endsWith(deletePath);
	}
	
	private void tesCreateAuWithInvalidGeom() throws Exception {
    	String fileName = "test/AuWithInvalidGeom.geojson";		
		String json = KmlRule.getStringFromFile(fileName);
        JsonNode node = Json.parse(json);

        Result result = request(routes.LocationServices.create(), node);
        assertThat(result.status()).isEqualTo(Status.BAD_REQUEST);
        String location = result.header(LOCATION);
        if(location != null){
        	long gid = toGid(location);
        	if(gid != 0L){
        		Result deleteResult = request(routes.LocationServices.delete(gid));
        		assertThat(deleteResult.status()).isEqualTo(Status.NO_CONTENT);
        	}
        }
	}
	
	private void tesCrudAuWithDuplication() throws Exception {
    	String fileName = "test/AuMaridiTown.geojson";
    	
		String json = KmlRule.getStringFromFile(fileName);
        JsonNode node = Json.parse(json);
        String path = "/api/locations";

        Result result = request(routes.LocationServices.create(), node);
        assertThat(result.status()).isEqualTo(Status.CREATED);
        Result duplication = request(routes.LocationServices.create(), node);
        assertThat(duplication.status()).isEqualTo(Status.FORBIDDEN);
        String location = result.header(LOCATION);
        assertThat(location).containsIgnoringCase(path);
        long gid = toGid(location);
		testRead(gid, null);
        Result deleteResult = request(routes.LocationServices.delete(gid));
        assertThat(deleteResult.status()).isEqualTo(Status.NO_CONTENT);
        String deletePath = path + "/" + gid;
		assertThat(deleteResult.header(LOCATION)).endsWith(deletePath);
	}


	private void testRead(long gid, FeatureCollection fc) throws Exception {
		FeatureCollection readFc = asFeatureCollection(gid);
		toExpected(readFc, gid);
		if (fc != null)
			assertISEqualTo(removeIrrelavantInfo(readFc), fc);
	}

	private void testCrud(String fileName) throws Exception {
		FeatureCollection fc = readFeatureCollectionFromFile(fileName);
    	
    	long gid = LocationServices.Wire.create(fc);
		assertThat(gid).isPositive();
		assertRead(fc, gid);
		
		Map<String, Object> properties = fc.getProperties();
		String name = "Upated " + properties.get("name").toString();
		properties.put("name", name);
    	long updatedGid = LocationServices.Wire.update(gid, fc);
		assertThat(updatedGid).isEqualTo(gid);
		Location updatedLocation = assertRead(fc, updatedGid);
		assertThat(updatedLocation.getData().getName()).isEqualTo(name);
		
		Long deletedGid = LocationServices.Wire.delete(gid);
		assertThat(deletedGid).isEqualTo(gid);
		Location deletedLocation = LocationRule.read(gid);
		assertThat(deletedLocation).isNull();
	}

	private FeatureCollection readFeatureCollectionFromFile(String fileName)
			throws Exception {
		JsonNode json = readJsonNodeFromFile(fileName);
		FeatureCollection fc = GeoJSONParser.parse(json);
		return fc;
	}

	private Location assertRead(FeatureCollection fc, long gid) {
		Location readLocation = LocationRule.read(gid);
		assertReadLocation(readLocation, fc, gid);
		FeatureCollection readFc = LocationServices.Wire.asFeatureCollection(readLocation);
		assertReadFc(readFc, fc, gid);
		return readLocation;
	}

	private void assertReadFc(FeatureCollection actual, FeatureCollection fc,
			long gid) {
		assertISEqualTo(removeIrrelavantInfo(actual), toExpected(fc, gid));
	}

	private void assertReadLocation(Location actual, FeatureCollection fc,
			long gid) {
		String typeName = actual.getData().getLocationType().getName();
		
		String expected = fc.getProperties().get("locationTypeName").toString();
		assertThat(typeName).isEqualTo(expected);
		Location expectedLocation = asLocation(fc, gid);
		assertISEqualTo(actual, expectedLocation);
	}

	private FeatureCollection removeIrrelavantInfo(FeatureCollection fc) {
		Map<String, Object> p = fc.getProperties();
		if (p == null)
			return fc;
		p.put("linage", null);
		p.put("parent", null);
		return fc;
	}

	private FeatureCollection toExpected(FeatureCollection fc, long gid) {
		fc.getFeatures().get(0).setId(gid + "");
		removeIrrelavantInfo(fc);
		return fc;
	}

	private void assertISEqualTo(Object actual, Object expected) {
		assertThat(actual).isNotNull();
		//TODO assertThat(actual).isEqualTo(expected);
	}

	private void testDeleteWithGid() throws Exception {
		//deleteTogetherWithAllGeometries(84952L);
	}
	
	private Long deleteTogetherWithAllGeometries(Long gid) {
		Long deletedGid = LocationRule.deleteTogetherWithAllGeometries(gid);
		return deletedGid;
	}

	private Location asLocation(FeatureCollection fc, long gid) {
		Location fcLocation = GeoJsonRule.asLocation(fc);
		fcLocation.setGid(gid);
		fcLocation.getGeometry().setGid(gid);
		return fcLocation;
	}
    
	private JsonNode readJsonNodeFromFile(String fileName) throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		String content = KmlRule.getStringFromFile(fileName);
		JsonNode actualObj = mapper.readTree(content);
		return actualObj;
	}
}