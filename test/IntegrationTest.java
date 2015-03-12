
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

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import play.Configuration;
import play.db.jpa.JPA;
import play.libs.F.Callback;
import play.libs.Json;
import play.mvc.Http.Status;
import play.mvc.Result;
import play.test.FakeRequest;
import play.test.TestBrowser;
import play.test.TestServer;
import play.twirl.api.Content;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import controllers.AdministrativeUnitServices;
import controllers.ApolloLocationServices;
import controllers.ListServices;
import controllers.LocationServices;
import dao.entities.Location;
import dao.entities.LocationType;

public class IntegrationTest {
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
		EntityManager em = JPA.em("test");
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
				
				testFindLocationsByFeatureCollection();
				testLocationType_PumaComposedOfCensusTract();
				testGetAuTypes();
				tesCrudAu();
				testApolloLocation();
				tesCrudEz();
        		
				transaction.rollback();
            }

        });
    }

	private void testFindLocationsByFeatureCollection() throws Exception {
		long supertTypeIdAdministrativeUnit = 3L;
		String geojson = KmlRule.getStringFromFile("test/geometry.geojson");
		
		long superTypeIdComposite = 2L;
		List<BigInteger> list = GeometryRule.findGidsByGeometry(geojson, superTypeIdComposite, null);
		assertGids(list, 84687L);

		list = GeometryRule.findGidsByGeometry(geojson, 1L, null);
		assertGids(list);

		list = GeometryRule.findGidsByGeometry(geojson, supertTypeIdAdministrativeUnit, null);
		assertGids(list, new long[] {1234568, 123456, 1169, 1213});

		list = GeometryRule.findGidsByGeometry(geojson, 4L, null);
		assertGids(list, new long[] {67079, 66676, 67136, 67173, 66735, 66822, 67019, 66820, 67081, 66664, 67117, 67111});

		String json = KmlRule.getStringFromFile("test/AuMaridiTown.geojson");
		FeatureCollection fc = readFeatureCollection(json);
		Feature feature0 = fc.getFeatures().get(0);
		FeatureGeometry geometry = feature0.getGeometry();
		String geo = Json.toJson(geometry).toString();
		List<BigInteger> list2 = GeometryRule.findGidsByGeometry(geo, supertTypeIdAdministrativeUnit, null);
		assertGids(list2, new long[] {1417, 1563, 1449, 1375, 1427, 1365, 1421});

        JsonNode node = Json.parse(json);
        String path = "/api/locations"; // "/resources/aus";
		FakeRequest request = new FakeRequest(POST, path).withJsonBody(node);
		Result postResult = callAction(controllers.routes.ref.LocationServices.findByFeatureCollection(supertTypeIdAdministrativeUnit, 0L), request);
        assertThat(status(postResult)).isEqualTo(Status.OK);
        assertThat(contentType(postResult)).isEqualTo("application/vnd.geo+json");
        String jsonResult = contentAsString(postResult);
        JsonNode resultNode = Json.parse(jsonResult);
        assertThat(resultNode.get("type").asText()).isEqualTo("FeatureCollection");
        assertThat(jsonResult).contains("\"gid\":\"1417\"");
	}

	private void assertGids(List<BigInteger> list, long... expects) {
		List<Long> actual = toList(list);
		for (long expect : expects)
			assertThat(actual).contains(expect);
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
	
	private void testApolloLocation() throws Exception {
		long gid = 3;
		String xml = ApolloLocationServices.Wire.readAsXml(gid);
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
        		browser.goTo("http://localhost:3333/ls");
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
    	String fileName = "test/AuMaridiTown.geojson";
		testCrud(fileName);
		
		String json = KmlRule.getStringFromFile(fileName);
        JsonNode node = Json.parse(json);
        String path = "/api/locations";
		FakeRequest request = new FakeRequest(POST, path).withJsonBody(node);

        Result result = callAction(controllers.routes.ref.LocationServices.create(), request);
        assertThat(status(result)).isEqualTo(Status.CREATED);
        String location = header(LOCATION, result);
        assertThat(location).containsIgnoringCase(path);
        long gid = toGid(location);
        String deletePath = path + "/" + gid;
		FakeRequest deletRequest = new FakeRequest(DELETE, deletePath);
        Result deleteResult = callAction(controllers.routes.ref.LocationServices.delete(gid), deletRequest);
        assertThat(status(deleteResult)).isEqualTo(Status.NO_CONTENT);
		assertThat(header(LOCATION, deleteResult)).endsWith(deletePath);
	}

	private void testCrud(String fileName) throws Exception {
		JsonNode json = readJsonNodeFromFile(fileName);
		FeatureCollection fc = GeoJSONParser.parse(json);
    	
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

	private Location assertRead(FeatureCollection fc, long gid) {
		Location readLocation = LocationRule.read(gid);
		assertReadLocation(readLocation, fc, gid);
		FeatureCollection readFc = LocationServices.Wire.read(gid);
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
