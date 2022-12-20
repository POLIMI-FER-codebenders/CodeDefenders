package org.codedefenders.auth;

import org.apache.shiro.authc.BearerToken;

public class APIBearerToken extends BearerToken {
    public APIBearerToken(String token) {
        super(token);
    }

    public APIBearerToken(String token, String host) {
        super(token, host);
    }
}
