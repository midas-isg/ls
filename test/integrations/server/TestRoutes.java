package integrations.server;

import org.junit.Ignore;
import org.junit.Test;

public class TestRoutes {
    @Test @Ignore("TestTopoJson always fails due to no command topojson")// @Ignore("to run test faster")
    public void allRoutes() {
    	Runnable[] tests = {
    			TestFindLocation.test(),
    			TestTopoJson.test()
    	};
		Server.run(tests);
    }
}
