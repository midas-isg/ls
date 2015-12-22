package integrations.server;

import org.junit.Test;

public class TestRoutes {
    @Test // @Ignore("to run test faster")
    public void allRoutes() {
    	Runnable[] tests = {
    			TestFindBatchLocation.test()
    	};
		Server.run(tests);
    }
}
