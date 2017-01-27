package security;

import security.auth0.Auth0Aid;
import play.mvc.Http;
import play.mvc.Http.Context;
import play.mvc.Result;
import play.mvc.Security;

public class Authentication extends Security.Authenticator {
	@Override
	public String getUsername(Context ctx) {
		return ctx.session().get(Auth0Aid.idTokenKey);
	}

	@Override
	public Result onUnauthorized(Context ctx) {
		return redirect(security.auth0.routes.Auth0Controller.login(toRelativeUrl(ctx)));
	}

	private String toRelativeUrl(Context ctx) {
		if (ctx == null)
			return null;
		final Http.Request request = ctx.request();
		if (request == null)
			return null;
		return request.uri();
	}
}