package org.codedefenders.servlets.auth;

import java.util.Objects;

import javax.enterprise.inject.Typed;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.BearerToken;
import org.apache.shiro.session.SessionException;
import org.apache.shiro.subject.Subject;
import org.codedefenders.auth.APIBearerToken;
import org.codedefenders.auth.FrontendBearerToken;
import org.codedefenders.beans.message.MessagesBean;
import org.codedefenders.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@Typed(CodeDefendersFakeBearerHttpAuthenticationFilter.class)
public class CodeDefendersFakeBearerHttpAuthenticationFilter extends CodeDefendersBearerHttpAuthenticationFilter {
    private static final Logger logger = LoggerFactory.getLogger(CodeDefendersBearerHttpAuthenticationFilter.class);

    @Inject
    public CodeDefendersFakeBearerHttpAuthenticationFilter(MessagesBean messages, UserService userService) {
        super(messages, userService);
    }

    @Override
    protected AuthenticationToken createToken(ServletRequest request, ServletResponse response) {
        BearerToken bearerToken = (BearerToken) super.createToken(request, response);
        return new FrontendBearerToken(bearerToken.getToken(), bearerToken.getHost());
    }

    @Override
    protected String getAuthzHeader(ServletRequest request) {
        String token = null;
        if (request.getParameterMap().containsKey("token")) {
            token = request.getParameter("token");
        }
        if (Objects.isNull(token)) {
            return null;
        }
        return "Bearer " + token;
    }

    @Override
    public boolean onPreHandle(ServletRequest request, ServletResponse response, Object mappedValue) throws Exception {
        if (request.getParameterMap().containsKey("token")) {
            Subject subject = getSubject(request, response);
            try {
                subject.logout(); //logout if trying to log in with another token
            } catch (SessionException ignored) {
                //
            }
        }
        return super.onPreHandle(request, response, mappedValue);
    }
}
