package org.codedefenders.auth;

import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.shiro.authc.Account;
import org.apache.shiro.authc.IncorrectCredentialsException;
import org.apache.shiro.authc.LockedAccountException;
import org.codedefenders.configuration.Configuration;
import org.codedefenders.model.UserEntity;
import org.codedefenders.persistence.database.SettingsRepository;
import org.codedefenders.persistence.database.UserRepository;

@Singleton
public class CodeDefendersFrontendBearerHttpAuthenticatingRealm extends CodeDefendersBearerHttpAuthenticatingRealm {
    @Inject
    public CodeDefendersFrontendBearerHttpAuthenticatingRealm(CodeDefendersRealm.CodeDefendersCacheManager codeDefendersCacheManager,
                                                              CodeDefendersRealm.CodeDefendersCredentialsMatcher codeDefendersCredentialsMatcher, SettingsRepository settingsRepo,
                                                              UserRepository userRepo, Configuration config) {
        super(codeDefendersCacheManager, codeDefendersCredentialsMatcher, settingsRepo, userRepo, config);
        setAuthenticationTokenClass(FrontendBearerToken.class);
    }

    public Account checkToken(String token) {
        Optional<UserEntity> activeUser = userRepo.getUserByFrontendToken(token);

        if (!activeUser.isPresent()) {
            throw new IncorrectCredentialsException("Invalid token");
        }

        if (settingsRepo.isMailValidationRequired() && !activeUser.get().isValidated()) {
            throw new LockedAccountException("Account email is not validated.");
        }

        if (!activeUser.get().isActive()) {
            throw new LockedAccountException("Your account is inactive, login is only possible with an active account.");
        }

        return getAccount(activeUser.get());
    }

}
