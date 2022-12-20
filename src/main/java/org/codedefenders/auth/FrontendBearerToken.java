package org.codedefenders.auth;

import org.apache.shiro.authc.BearerToken;

public class FrontendBearerToken extends BearerToken {
    public FrontendBearerToken(String token) {
        super(token);
    }

    public FrontendBearerToken(String token, String host) {
        super(token, host);
    }
}
