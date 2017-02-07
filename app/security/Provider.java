package security;

import security.auth0.Auth0Aid;
import play.cache.CacheApi;
import play.mvc.Http.Session;

import javax.inject.Inject;

public class Provider {
    private final CacheApi cacheApi;

    @Inject
    public Provider(CacheApi cacheApi) {
        this.cacheApi = cacheApi;
    }

    public User getUser(Session session) {
        final String idToken = session.get(Auth0Aid.idTokenKey);
        return cacheApi.get(idToken + "profile");
    }
}
