package security;

import play.libs.F.Promise;
import play.mvc.Action;
import play.mvc.Http.Context;
import play.mvc.Result;
import security.Secured.Authority;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.List;

import static common.RuntimeExceptionReThrower.tryTo;
import static java.util.Arrays.stream;
import static play.mvc.Security.Authenticator;

public class SecuredAction extends Action<Secured> {
    private final Provider provider;
    private final Authenticator authenticator;

    @Inject
    public SecuredAction(Provider provider, Authenticator authenticator) {
        this.provider = provider;
        this.authenticator = authenticator;
    }

    @Override
    public Promise<Result> call(Context ctx) {
        return tryTo(() -> authorizeTheCall(ctx));
    }

    private Promise<Result> authorizeTheCall(Context ctx) {
        final User user = provider.getUser(ctx.session());
        return isAuthenticated(user)
                ? authorizeUser(ctx, user)
                : theCallWasNotAuthenticated(ctx);
    }

    private Promise<Result> authorizeUser(Context ctx, User theUser) {
        final Authority[] requiredAuthorities = toRequiredAuthorities();
        return hasAuthority(theUser, requiredAuthorities)
                ? tryTo(()->delegate.call(ctx))
                : theUserHasNoAuthority(requiredAuthorities);
    }

    private boolean isAuthenticated(User user) {
        return user != null;
    }

    private Promise<Result> theCallWasNotAuthenticated(Context ctx) {
        return Promise.pure(authenticator.onUnauthorized(ctx));
    }

    private Authority[] toRequiredAuthorities() {
        return configuration.value();
    }

    private boolean hasAuthority(User user, Authority[] requiredAuthorities) {
        assert isAuthenticated(user);
        final List<String> roles = user.getAuthorities();
        return stream(requiredAuthorities)
                .map(Enum::name)
                .anyMatch(roles::contains);
    }

    private Promise<Result> theUserHasNoAuthority(Authority[] requiredAuthorities) {
        final String message = "You need at least one of " + Arrays.toString(requiredAuthorities);
        return Promise.pure(forbidden(message));
    }
}