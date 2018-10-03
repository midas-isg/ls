package security;

import com.auth0.authentication.result.UserProfile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyList;

public class User extends UserProfile {
    public User(Map<String, Object> values){
        super(values);
    }

    public List<String> getAuthorities(){
        return getRoles();
    }

    private List<String> getRoles() {
        final List<String> roles = getRoles(getExtraInfo());
        return roles == null ? emptyList() : roles;
    }

    private List<String> getRoles(Map<String, Object> extraInfo) {
        if (extraInfo == null)
            return null;
        final Object appMetadata = extraInfo.get("https://epimodels.org/app_metadata");
        if (appMetadata == null)
            return null;
        @SuppressWarnings("unchecked")
        final Map<String, Object> meta = (Map<String, Object>)appMetadata;
        @SuppressWarnings("unchecked")
        final List<String> roles = (List<String>)meta.get("roles");
        return roles;
    }
}
