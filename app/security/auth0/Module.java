package security.auth0;

import com.google.inject.AbstractModule;
import play.Configuration;
import play.Environment;
import play.mvc.Security;
import security.Authentication;

import javax.inject.Inject;

public class Module extends AbstractModule {
    @Inject
    public Module(Environment environment, Configuration configuration) {
    }

    @Override
    public void configure() {
        bind(Security.Authenticator.class).to(Authentication.class);
    }
}