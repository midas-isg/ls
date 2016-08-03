package integrations.app;

import static play.test.Helpers.fakeApplication;
import static play.test.Helpers.running;

import java.io.File;
import java.lang.reflect.Method;
import java.util.Map;

import play.Application;
import play.Configuration;
import play.GlobalSettings;
import play.Logger;
import play.libs.F.Callback0;
import play.libs.F.Function0;
import play.mvc.Action;
import play.mvc.Http.Request;
import play.test.FakeApplication;
import suites.Helper;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import gateways.configuration.Global;

public class App {
	private FakeApplication fakeApp = null;
	private static String IN_MEMO_DB_CONF_PATH = "test/resources/test_in_memory_DB.conf";
	private static String TEST_CONF_PATH = "test/resources/test.conf";

	public static App newWithTestDb() {
		return new App(TEST_CONF_PATH);
	}

	public static App newWithInMemoryDbWithDbOpen() {
		return newWithInMemoryDb("keep");
	}

	public static App newWithInMemoryDb(String uid) {
		return new App(IN_MEMO_DB_CONF_PATH, uid);
	}

	public static App doNotUseForBoostingupCoverageOnly(String path) {
		try {
			return new App(path);
		} catch (Exception e) {
			return null;
		}
	}

	private App(String testConfPathname) {
		this(testConfPathname, "");
	}

	@SuppressWarnings("unchecked")
	private App(String testConfPathname, String uid) {
		Map<String, Object> configurationMap = readConf(testConfPathname);
		if (testConfPathname.equals(IN_MEMO_DB_CONF_PATH))
			((Map<String, Object>) ((Map<String, Object>) configurationMap.get("db")).get("default")).put("initSQL",
					"");
		keepDatabaseOpen(configurationMap, uid);
		fakeApp = fakeApplication(configurationMap, new GlobalSettings() {
			@Override
			public void onStart(Application app) {
				Logger.info(" ls started as fakeApplication");
			}

			@Override
			public void onStop(Application app) {
				new Global().onStop(app);
			}

			@Override
			public Action<Void> onRequest(Request request, Method actionMethod) {
				return new Global().onRequest(request, actionMethod);
			}
		});
	}

	public FakeApplication getFakeApp() {
		return fakeApp;
	}

	private void keepDatabaseOpen(Map<String, Object> originalMap, String uid) {
		if (uid == null || uid.isEmpty())
			return;
		Map<String, Object> map = getMap(getMap(originalMap, "db"), "default");
		String url = (String) map.get("url");
		map.put("url", url + "-" + uid + ";DB_CLOSE_DELAY=-1");
	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> getMap(Map<String, Object> map, String key) {
		return (Map<String, Object>) map.get(key);
	}

	public void runWithTransaction(Callback0 callback) {
		Function0<Void> f = () -> {
			callback.invoke();
			return null;
		};

		running(fakeApp, () -> Helper.wrapTransaction(f));
	}

	private Map<String, Object> readConf(String pathname) {
		File file = new File(pathname);
		if (!file.exists())
			throw new RuntimeException(pathname + " is not found");
		Config config = ConfigFactory.parseFile(file).resolve();
		Configuration configuration = new Configuration(config);
		return configuration.asMap();
	}
}
