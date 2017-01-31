package security;

import com.auth0.authentication.result.UserProfile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class User extends UserProfile {
    public User(Map<String, Object> values){
        super(values);
    }

    public List<String> getAuthorities(){
        final Object rolesObj = getExtraInfo().get("roles");
        if (rolesObj == null)
            return Collections.emptyList();
        @SuppressWarnings("unchecked")
        final List<String> roles = (List<String>) rolesObj;
        return new ArrayList<>(roles);
    }
}
