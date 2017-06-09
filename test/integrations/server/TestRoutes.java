package integrations.server;

import org.junit.Ignore;
import org.junit.Test;

public class TestRoutes {
    @Test //@Ignore("TestTopoJson may fail due to no command topojson")
    public void allRoutes() {
    	Runnable[] tests = {
    			TestFindLocation.test(),
    			TestTopoJson.test()
    	};
		Server.run(tests);
    }
}
