package security.auth0;

import play.libs.F.Promise;
import play.mvc.Controller;
import play.mvc.Http.Session;
import play.mvc.Result;
import security.Provider;

import javax.inject.Inject;

public class Auth0Controller extends Controller {
    private static final String targetRelativeUrlKey = "targetRelativeUrl";

    private final Auth0Aid aid;
    private final Callback callback;
    private final Provider provider;

    @Inject
    public Auth0Controller(Auth0Aid aid, Callback callback, Provider provider) {
        this.aid = aid;
        this.callback = callback;
        this.provider = provider;
    }

    public Result login(String targetRelativeUrl) {
        final Session session = session();
        putTargetRelativeUrl(session, targetRelativeUrl);
        return ok(aid.renderLogin(request(), aid.toUserId(provider, session)));
    }

    public Promise<Result> callback(String authorizationCode){
        final Session capturedSession = session();
        return callback.handle(authorizationCode, ctx())
                .map(user -> redirectBasingOnSession(capturedSession));
    }

    public Result logout() {
        session().clear();
        return redirect(aid.toAuth0AbsoluteGlobalLogoutUrl(aid.toAbsoluteLoginUrl(request()) + "#logout"));
    }

    private void putTargetRelativeUrl(Session session, String targetRelativeUrl) {
        session.put(targetRelativeUrlKey, targetRelativeUrl != null ? targetRelativeUrl: aid.defaultTargetUrlPath);
    }

    private Result redirectBasingOnSession(Session session) {
        return redirect(removeTargetRelativeUrl(session));
    }

    private String removeTargetRelativeUrl(Session session) {
        final String targetRelativeUrl = session.remove(targetRelativeUrlKey);
        if (targetRelativeUrl == null || targetRelativeUrl.isEmpty())
            return aid.defaultTargetUrlPath;
        return targetRelativeUrl;
    }
}
