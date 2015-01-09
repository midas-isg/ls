import static play.test.Helpers.HTMLUNIT;
import static play.test.Helpers.fakeApplication;
import static play.test.Helpers.running;
import static play.test.Helpers.testServer;

import java.util.List;

import javax.persistence.EntityManager;

import org.junit.Test;

import play.db.jpa.JPA;
import play.libs.F.Callback;
import play.test.TestBrowser;
import dao.AuDao;
import dao.entities.Location;

public class IntegrationTest {

    /**
     * add your integration test here
     * in this example we just check if the welcome page is being shown
     */
    @Test
    public void test() {
        running(testServer(3333, fakeApplication()), HTMLUNIT, new Callback<TestBrowser>() {
            public void invoke(TestBrowser browser) {
            	EntityManager em = JPA.em("default");
            	//JPA.
            	JPA.bindForCurrentThread(em);
            	em.getTransaction().begin();
            	AuDao dao = new AuDao();
            	List<Location> l = dao.findRoots();//.read(1);
            	System.out.println(l);
            	em.getTransaction().commit();
            	
        		/*browser.goTo("http://localhost:3333");
                assertThat(browser.pageSource()).contains("Your new application is ready.");*/

            }
        });
    }
}
