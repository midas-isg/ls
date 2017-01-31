package security.auth0;

import com.auth0.authentication.result.Credentials;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import play.cache.CacheApi;
import play.libs.F.Promise;
import play.libs.Json;
import play.libs.ws.WSClient;
import play.libs.ws.WSRequest;
import play.libs.ws.WSResponse;
import play.mvc.Http.Context;
import play.mvc.Http.Session;
import security.User;

import javax.inject.Inject;
import java.util.Map;

class Callback {
    private final WSClient ws;
    private final Auth0Aid aid;
    private final CacheApi cache;
    private Session currentSession;

    @Inject
    Callback(WSClient ws, Auth0Aid aid, CacheApi cache) {
        this.ws = ws;
        this.aid = aid;
        this.cache = cache;
    }

    Promise<User> handle(String authorizationCode, Context ctx){
        currentSession = ctx.session();
        final Map<String, Object> body = aid.toTokenBody(authorizationCode, ctx.request());
        return fetchToken(body)
                .map(this::sessionCredentials)
                .flatMap(this::fetchUser)
                .map(this::cacheUser);
    }


    private Promise<Credentials> fetchToken(Map<String, Object> body) {
        final WSRequest request = ws.url(aid.toAuth0TokenUrl());
        return request.post(Json.toJson(body))
                .map(WSResponse::asJson)
                .map(this::jsonToCredentials);
    }

    private Credentials sessionCredentials(Credentials credentials){
        currentSession.put(Auth0Aid.idTokenKey, credentials.getIdToken());
        currentSession.put(Auth0Aid.accessTokenKey, credentials.getAccessToken());
        return credentials;
    }

    private Promise<User> fetchUser(Credentials credentials) {
        final WSRequest request = ws.url(aid.toAuth0UserInfoUrl(credentials));
        return  request.get()
                .map(WSResponse::asJson)
                .map(this::jsonToMap)
                .map(User::new);
    }

    private Credentials jsonToCredentials(JsonNode json) {
        return Json.fromJson(json, Credentials.class);
    }

    private Map jsonToMap(JsonNode json) {
        return new ObjectMapper().convertValue(json, Map.class);
    }

    private User cacheUser(User user) {
        final String idToken = currentSession.get(Auth0Aid.idTokenKey);
        cache.set(idToken + "profile", user);
        return user;
    }
}
