package integrations.server;

import static play.test.Helpers.running;
import static play.test.Helpers.testServer;
import integrations.app.App;
import suites.Helper;

public class Server {
	private static String context = null;
	
	public static String makeTestUrl(String path) {
		final String fullPath = getContext() + path;
		return "http://localhost:3333" + fullPath.replace("//", "/");
	}

	public static void run(Runnable... tests) {
		Runnable test = () -> runAll(tests);
		running(testServer(3333, App.newWithTestDb().getFakeApp()), test);
	}

	private static void runAll(Runnable... tests) {
		for (Runnable test : tests)
			test.run();
	}

	public static String getContext() {
		if (context == null)
			context = Helper.readContext();
		return context;
	}
}
