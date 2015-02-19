
import static org.fest.assertions.Assertions.assertThat;
import static play.test.Helpers.HTMLUNIT;
import static play.test.Helpers.contentAsString;
import static play.test.Helpers.contentType;
import static play.test.Helpers.fakeApplication;
import static play.test.Helpers.running;
import static play.test.Helpers.testServer;
import interactors.GeoJSONParser;
import interactors.GeoJsonRule;
import interactors.KmlRule;
import interactors.LocationRule;
import interactors.XmlRule;

import java.io.File;
import java.sql.Date;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;

import models.geo.FeatureCollection;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import play.Configuration;
import play.db.jpa.JPA;
import play.libs.F.Callback;
import play.test.TestBrowser;
import play.test.TestServer;
import play.twirl.api.Content;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import controllers.AdministrativeUnitServices;
import controllers.ApolloLocationServices;
import controllers.LocationServices;
import dao.CodeTypeDao;
import dao.GeometryDao;
import dao.LocationTypeDao;
import dao.entities.Data;
import dao.entities.Location;
import dao.entities.LocationGeometry;

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
	
	private Location createTestLocation(Location parent) {
		GeometryDao dao = new GeometryDao();
		Location l = new Location();
		
		LocationGeometry plg = dao.read(parent.getGid()); 
		LocationGeometry lg = new LocationGeometry();
		lg.setLocation(l);
		l.setGeometry(lg);
		
		lg.setMultiPolygonGeom(plg.getMultiPolygonGeom());
		
		Data data = new Data();
		data.setName("Test");
		data.setDescription("Test description");
		Date date = new Date(2014,1,1);
		data.setStartDate(date);
		data.setUpdateDate(date);
		data.setCodeType(new CodeTypeDao().read(LocationRule.ISG_CODE_TYPE_ID));
		data.setGisSource(LocationRule.getAlsGisSource());
		data.setLocationType(new LocationTypeDao().read(LocationRule.EPIDEMIC_ZONE_ID));
		l.setData(data);
		l.setParent(parent);
		return l;
	}

	@Test
    public void testWithJpaThenRollback() {
		running(testServer, HTMLUNIT, new Callback<TestBrowser>() {
            public void invoke(TestBrowser browser) throws Exception {
        		EntityManager em = initEntityManager();
        		EntityTransaction transaction = em.getTransaction();
				transaction.begin();
				transaction.setRollbackOnly();
				tesCrudAu();
				testApolloLocation();
				tesCrudEz();
        		transaction.rollback();
            }
        });
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
    	JsonNode requestJSON = readJsonNodeFromFile("test/EzMaridi.geojson");
    	FeatureCollection fc = GeoJSONParser.parse(requestJSON);
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
    	JsonNode requestJSON = readJsonNodeFromFile("test/AuMaridiTown.geojson");
    	FeatureCollection fc = GeoJSONParser.parse(requestJSON);
    	long gid = LocationServices.Wire.create(fc);
		assertThat(gid).isPositive();
		Location readLocation = LocationRule.read(gid);
		assertThat(readLocation.getData().getLocationType().getName()).isEqualTo("Town");
		Location expectedLocation = asLocation(fc, gid);
		assertISEqualTo(readLocation, expectedLocation);
		FeatureCollection readFc = AdministrativeUnitServices.Wire.read(gid);
		toExpected(fc, gid);
		assertISEqualTo(removeIrrelavantInfo(readFc), fc);
		Long deletedGid = deleteTogetherWithAllGeometries(gid);
		assertThat(deletedGid).isEqualTo(gid);
		Location deletedLocation = LocationRule.read(gid);
		assertThat(deletedLocation).isNull();
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
