package integrations.server;

import org.junit.Test;

public class TestRoutes {
    @Test // @Ignore("to run test faster")
    public void allRoutes() {
    	Runnable[] tests = {
    			TestFindBulkLocation.test()
    	};
		Server.run(tests);
    }
}
