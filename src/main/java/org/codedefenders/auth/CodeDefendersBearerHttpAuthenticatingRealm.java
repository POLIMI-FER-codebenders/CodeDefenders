/*
 * Copyright (C) 2020 Code Defenders contributors
 *
 * This file is part of Code Defenders.
 *
 * Code Defenders is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Code Defenders is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Code Defenders. If not, see <http://www.gnu.org/licenses/>.
 */

package org.codedefenders.auth;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.apache.catalina.User;
import org.apache.catalina.UserDatabase;
import org.apache.shiro.authc.Account;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.BearerToken;
import org.apache.shiro.authc.SimpleAccount;
import org.apache.shiro.authc.credential.AllowAllCredentialsMatcher;
import org.apache.shiro.realm.AuthenticatingRealm;
import org.codedefenders.configuration.Configuration;
import org.codedefenders.model.UserEntity;
import org.codedefenders.persistence.database.SettingsRepository;
import org.codedefenders.persistence.database.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implements a Realm that uses the {@link UserRepository} for authenticating users. The logic
 * to record the start of a session and the like has been moved inside the
 * {@link org.codedefenders.servlets.auth.CodeDefendersBearerHttpAuthenticationFilter#onLoginSuccess} method
 *
 * @author gambi
 */
public abstract class CodeDefendersBearerHttpAuthenticatingRealm extends AuthenticatingRealm {

    private static final Logger logger = LoggerFactory.getLogger(CodeDefendersRealm.class);

    private final String adminRole;

    protected final SettingsRepository settingsRepo;
    protected final UserRepository userRepo;

    private final UserDatabase userDatabase;

    @Inject
    public CodeDefendersBearerHttpAuthenticatingRealm(CodeDefendersRealm.CodeDefendersCacheManager codeDefendersCacheManager, CodeDefendersRealm.CodeDefendersCredentialsMatcher codeDefendersCredentialsMatcher, SettingsRepository settingsRepo, UserRepository userRepo, @SuppressWarnings("CdiInjectionPointsInspection") Configuration config) {
        super(codeDefendersCacheManager, codeDefendersCredentialsMatcher);
        this.settingsRepo = settingsRepo;
        this.userRepo = userRepo;
        UserDatabase userDatabase;
        try {
            userDatabase = (UserDatabase) new InitialContext().lookup("java:comp/env/auth/UserDatabase");
        } catch (NamingException e) {
            logger.error("Exception looking up user database", e);
            userDatabase = null;
            // TODO(Alex): Should we really continue here?!
        }
        this.userDatabase = userDatabase;
        this.adminRole = config.getAuthAdminRole();
        setCredentialsMatcher(new AllowAllCredentialsMatcher());
    }

    protected Account getAccount(UserEntity userEntity) {

        Collection<Object> principals = new ArrayList<>();
        principals.add(new CodeDefendersRealm.LocalUserId(userEntity.getId()));

        Set<String> roles = new java.util.HashSet<>();
        roles.add("user");

        User tomcatUser = userDatabase.findUser(userEntity.getUsername());
        if (tomcatUser != null && tomcatUser.isInRole(userDatabase.findRole(adminRole))) {
            roles.add("admin");
        }

        return new SimpleAccount(principals, userEntity.getEncodedPassword(), getName(), roles, new HashSet<>());
    }

    @Override
    protected AuthenticationInfo doGetAuthenticationInfo(final AuthenticationToken token) throws AuthenticationException {

        BearerToken bearerToken = (BearerToken) token;
        return checkToken(bearerToken.getToken());
    }

    public abstract Account checkToken(String token);
}
