package setting;

import java.lang.reflect.Method;

import play.Application;
import play.GlobalSettings;
import play.Logger;
import play.libs.F;
import play.mvc.Action;
import play.mvc.Http.Context;
import play.mvc.Http.Request;
import play.mvc.Result;

public class Global extends GlobalSettings {
	private static final String appName = "ALS";

	@Override
	public void onStart(Application app) {
		Logger.info(appName + " has started");
	}

	@Override
	public void onStop(Application app) {
		Logger.info(appName + " shutdown...");
	}

	/**
	 * Play 2.3.2 didn't catch Throwable and crashed so we catch them ourselves.
	 */
	@SuppressWarnings("rawtypes")
	@Override
	public Action onRequest(Request request, Method actionMethod) {
		return new Action.Simple() {
			public F.Promise<Result> call(Context ctx) throws Throwable {
				try {
					return delegate.call(ctx);
				} catch (Throwable e) {
					throw new RuntimeException(e);
				}
			}
		};
	}

}