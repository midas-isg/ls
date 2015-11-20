
import static org.fest.assertions.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import play.Logger;



/**
*
* Simple (JUnit) tests that can call all parts of a play app.
* If you are interested in mocking a whole application, see the wiki for more details.
*
*/
public class ApplicationTest {
    @Test
    public void simpleCheck() {
        int a = 1 + 1;
        assertThat(a).isEqualTo(2);
    }
    
    @Test
    public void checkPolygon()
	{
Logger.debug("===Begin Test===");
		List<List<double[]>> test = new ArrayList<List<double[]>>();
		test.add(new ArrayList<double[]>());
		double [] temp = {30, 10};
		test.get(0).add(temp.clone());
		temp[0] = 40;
		temp[1] = 40;
		test.get(0).add(temp.clone());
		temp[0] = 20;
		temp[1] = 40;
		test.get(0).add(temp.clone());
		temp[0] = 10;
		temp[1] = 20;
		test.get(0).add(temp.clone());
		temp[0] = 30;
		temp[1] = 10;
		test.get(0).add(temp.clone());
		models.geo.Polygon fake = new models.geo.Polygon();
		fake.setCoordinates(test);
		
		Logger.debug(fake.toString());
		List<List<double[]>> coordinates = fake.getCoordinates();
		assertThat(coordinates.size()).isEqualTo(1);
		assertThat(coordinates.get(0).size()).isEqualTo(5);
		assertThat(coordinates.get(0).get(0).length).isEqualTo(2);
Logger.debug("====End Test====");
		
		return;
	}

}
