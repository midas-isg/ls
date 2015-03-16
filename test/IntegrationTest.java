
import static org.fest.assertions.Assertions.assertThat;
import static play.mvc.Http.HeaderNames.LOCATION;
import static play.test.Helpers.DELETE;
import static play.test.Helpers.HTMLUNIT;
import static play.test.Helpers.POST;
import static play.test.Helpers.callAction;
import static play.test.Helpers.contentAsString;
import static play.test.Helpers.contentType;
import static play.test.Helpers.fakeApplication;
import static play.test.Helpers.header;
import static play.test.Helpers.running;
import static play.test.Helpers.status;
import static play.test.Helpers.testServer;
import interactors.GeoJSONParser;
import interactors.GeoJsonRule;
import interactors.KmlRule;
import interactors.LocationRule;
import interactors.LocationTypeRule;
import interactors.XmlRule;

import java.io.File;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;

import models.geo.Feature;
import models.geo.FeatureCollection;

import org.fest.assertions.StringAssert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import play.Configuration;
import play.db.jpa.JPA;
import play.libs.F.Callback;
import play.libs.Json;
import play.mvc.Result;
import play.mvc.Http.Status;
import play.test.FakeRequest;
import play.test.TestBrowser;
import play.test.TestServer;
import play.twirl.api.Content;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.MultiPolygon;

import controllers.AdministrativeUnitServices;
import controllers.ApolloLocationServices;
import controllers.ListServices;
import controllers.LocationServices;
import dao.entities.Location;
import dao.entities.LocationType;

public class IntegrationTest {
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
    	JPA.bindForCurrentThread(em);
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
				
				testCreateEzFromAu();
				testLocationType_PumaComposedOfCensusTract();
				 testGetAuTypes();
				testMaxExteriorRings(browser);
				testGeoMetadata(browser);
				tesCrudAu();
				testApolloLocation();
				tesCrudEz();
				
				transaction.rollback();
            }
        });
    }

	private void testCreateEzFromAu() throws Exception {
		String fileName = "test/EzFromAu.geojson";
		FeatureCollection fc = readFeatureCollectionFromFile(fileName);
		Feature f0 = fc.getFeatures().get(0);
		assertThat(f0.getId()).isEqualTo("11");
		Location l = GeoJsonRule.asLocation(fc);
		Geometry geometry = l.getGeometry().getMultiPolygonGeom();
		String type = geometry.getGeometryType();
		assertThat(type).isEqualTo(MultiPolygon.class.getSimpleName());
		MultiPolygon mp = (MultiPolygon)geometry;
		int expectedNumGeometries = 80;
		assertThat(mp.getNumGeometries()).isEqualTo(expectedNumGeometries);
		
		String json = KmlRule.getStringFromFile(fileName);
        JsonNode node = Json.parse(json);
        String path = "/api/locations";
		FakeRequest request = new FakeRequest(POST, path).withJsonBody(node);

        Result result = callAction(controllers.routes.ref.LocationServices.create(), request);
        assertThat(status(result)).isEqualTo(Status.CREATED);
        String location = header(LOCATION, result);
        assertThat(location).containsIgnoringCase(path);
        long gid = toGid(location);
        Location readLocation = LocationRule.read(gid);
        assertThat(readLocation.getGid()).isEqualTo(gid);
        MultiPolygon readMp = (MultiPolygon)readLocation.getGeometry().getMultiPolygonGeom();
		assertThat(readMp.getNumGeometries()).isEqualTo(expectedNumGeometries);

        String deletePath = path + "/" + gid;
		FakeRequest deletRequest = new FakeRequest(DELETE, deletePath);
        Result deleteResult = callAction(controllers.routes.ref.LocationServices.delete(gid), deletRequest);
        assertThat(status(deleteResult)).isEqualTo(Status.NO_CONTENT);
		assertThat(header(LOCATION, deleteResult)).endsWith(deletePath);
	}
	
	private long toGid(String url) {
		String[] tokens = url.split("/");
		String gid = tokens[tokens.length - 1];
		return Long.parseLong(gid);
	}


	private void testLocationType_PumaComposedOfCensusTract() {
		String pumaName = "PUMA";
		LocationType puma = LocationTypeRule.findByName(pumaName);
		assertThat(puma.getName()).isEqualToIgnoringCase(pumaName);
		assertThat(puma.getComposedOf().getName()).isEqualToIgnoringCase("Census Tract");
	}
	
	private void testGetAuTypes() {
		List<String> auTypes = ListServices.Wire.getTypes("Administrative Unit");
		assertThat(auTypes).contains("Town", "Country");
		assertThat(auTypes).doesNotHaveDuplicates();
		assertThat(auTypes).excludes("PUMA", "Census Tract", "Epidemic Zone");
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
		return location.getGeometry().getMultiPolygonGeom().getNumGeometries();
	}

	private void testGeoMetadata(TestBrowser browser) {
		Result result1 = LocationServices.getGeometryMetadata(11, null);
		status(result1);
		String content = contentAsString(result1);
		JsonNode json = Json.parse(content);
		assertThat(json.findValue("tolerance").isNull()).isTrue();
		int nGeo1 = json.findValue("numGeometries").intValue();
		
		Result result2 = LocationServices.getGeometryMetadata(1, 0.5);
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
        		browser.goTo(context);
                String expected = "apollo location services";
				assertThat(browser.pageSource()).contains(expected);
                renderTemplate();
            }
        });
    }
    
    private void renderTemplate() {
        Content html = views.html.create.render("ALS is ready.", "");
        assertThat(contentType(html)).isEqualTo("text/html");
        assertThat(contentAsString(html)).contains("ALS is ready.");
    }

	private void tesCrudEz() throws Exception {
    	JsonNode json = readJsonNodeFromFile("test/EzMaridi.geojson");
    	FeatureCollection fc = GeoJSONParser.parse(json);
    	long gid = AdministrativeUnitServices.Wire.create(fc);
		assertThat(gid).isPositive();
		Location readLocation = LocationRule.read(gid);
		Location expectedLocation = asDeprecatedLocation(fc, gid);
		assertISEqualTo(readLocation, expectedLocation);
		assertThat(readLocation.getData().getLocationType().getName()).isEqualTo("Epidemic Zone");
		FeatureCollection readFc = AdministrativeUnitServices.Wire.read(gid);
		toExpected(fc, gid);
		assertISEqualTo(removeIrrelavantInfo(readFc), fc);
		Long deletedGid = deleteTogetherWithAllGeometries(gid);
		assertThat(deletedGid).isEqualTo(gid);
		Location deletedLocation = LocationRule.read(gid);
		assertThat(deletedLocation).isNull();
	}

	private void tesCrudAu() throws Exception {
    	testCrud("test/AuMaridiTown.geojson");
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
    
	private Location asDeprecatedLocation(FeatureCollection fc, long gid) {
		Location fcLocation = GeoJsonRule.asDeprecatedLocation(fc);
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
