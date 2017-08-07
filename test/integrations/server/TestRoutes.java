package integrations.server;

import org.junit.Ignore;
import org.junit.Test;

public class TestRoutes {
	@Test
	public void allRoutes() {
		Runnable[] tests = { TestFindLocation.test() };
		Server.run(tests);
	}

	@Ignore("TestTopoJson may fail due to no command topojson")
	public void TestTopoJson() {
		Server.run(TestTopoJson.test());
	}
}
