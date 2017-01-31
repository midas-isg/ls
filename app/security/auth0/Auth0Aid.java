package security.auth0;

import com.auth0.authentication.result.Credentials;
import play.Configuration;
import play.mvc.Http;
import play.mvc.Http.Request;
import play.twirl.api.Html;
import security.Provider;
import security.User;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;

public class Auth0Aid {
    public static final String idTokenKey = "idToken";
    static final String accessTokenKey ="accessToken";

    public final String clientId;
    public final String domain;
    public final String clientSecret;
    public final String defaultTargetUrlPath;
    public final String hubWsUrl;

    @Inject
    public Auth0Aid(Configuration conf) {
        final String prefix = "auth0";
        clientId = conf.getString(prefix + ".clientId");
        domain = conf.getString(prefix + ".domain");
        clientSecret = conf.getString(prefix + ".clientSecret");
        defaultTargetUrlPath = conf.getString(prefix + ".defaultTargetUrlPath");
        hubWsUrl = conf.getString("app.servers.hub.ws.url");
    }

    Html renderLogin(Request req, String userId) {
        final String callbackUrl = toAbsoluteCallbackUrl(req);
        return security.auth0.views.html.login.render(this, callbackUrl, userId);
    }

    String toAuth0UserInfoUrl(Credentials credentials) {
        return toAuthorityUrl() + "/userinfo?access_token=" + credentials.getAccessToken();
    }

    String toAuth0TokenUrl() {
        return toAuthorityUrl() + "/oauth/token";
    }

    Map<String, Object> toTokenBody(String authorizationCode, Request request) {
        final String callbackUrl = toAbsoluteCallbackUrl(request);
        Map<String, Object> body = new HashMap<>();
        body.put("client_id", clientId);
        body.put("client_secret", clientSecret);
        body.put("redirect_uri", callbackUrl);
        body.put("grant_type", "authorization_code");
        body.put("code", authorizationCode);
        return body;
    }

    String toUserId(Provider provider, Http.Session session) {
        if (provider == null)
            return null;
        final User user = provider.getUser(session);
        if (user == null)
            return null;
        return user.getId();
    }

    String toAuth0AbsoluteGlobalLogoutUrl(String returnToAbsoluteUrl){
        return toAuthorityUrl() + "/v2/logout?returnTo=" + returnToAbsoluteUrl;
    }

    String toAbsoluteLoginUrl(Request req) {
        return security.auth0.routes.Auth0Controller.login(null).absoluteURL(req);
    }

    private String toAbsoluteCallbackUrl(Request req) {
        return security.auth0.routes.Auth0Controller.callback(null).absoluteURL(req);
    }

    private String toAuthorityUrl() {
        return "https://" + domain;
    }
}
