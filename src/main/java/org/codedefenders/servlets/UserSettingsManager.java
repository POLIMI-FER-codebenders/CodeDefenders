/*
 * Copyright (C) 2016-2019 Code Defenders contributors
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
package org.codedefenders.servlets;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.codedefenders.auth.CodeDefendersAuth;
import org.codedefenders.beans.message.MessagesBean;
import org.codedefenders.database.AdminDAO;
import org.codedefenders.model.KeyMap;
import org.codedefenders.model.UserEntity;
import org.codedefenders.persistence.database.UserRepository;
import org.codedefenders.service.UserService;
import org.codedefenders.servlets.admin.AdminSystemSettings;
import org.codedefenders.servlets.util.Redirect;
import org.codedefenders.servlets.util.ServletUtils;
import org.codedefenders.util.Constants;
import org.codedefenders.util.Paths;
import org.codedefenders.validation.input.CodeDefendersValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This {@link HttpServlet} handles requests for managing the currently logged
 * in {@link UserEntity}. This functionality may be disabled, e.g. in a class room
 * setting. See {@link #checkEnabled()}.
 *
 * <p>Serves on path: {@code /account-settings}.
 *
 * @author <a href="https://github.com/werli">Phil Werli</a>
 */
@WebServlet(Paths.USER_SETTINGS)
public class UserSettingsManager extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(UserProfileManager.class);

    @Inject
    private MessagesBean messages;

    @Inject
    private UserService userService;

    @Inject
    private UserRepository userRepo;

    @Inject
    private CodeDefendersAuth login;

    private static final String DELETED_USER_NAME = "DELETED";
    private static final String DELETED_USER_EMAIL = "%s@deleted-code-defenders";
    private static final String DELETED_USER_PASSWORD = "DELETED";

    /**
     * Checks whether users can view and update their profile information.
     *
     * @return {@code true} when users can access their profile, {@code false} otherwise.
     */
    public static boolean checkEnabled() {
        // TODO: add ALLOW_USER_SETTINGS to DB and use it instead of ALLOW_USER_PROFILE.
        // return AdminDAO.getSystemSetting(AdminSystemSettings.SETTING_NAME.ALLOW_USER_SETTINGS).getBoolValue();
        return AdminDAO.getSystemSetting(AdminSystemSettings.SETTING_NAME.ALLOW_USER_PROFILE).getBoolValue();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (!checkEnabled()) {
            // Send users to the home page
            response.sendRedirect(ServletUtils.getBaseURL(request));
            return;
        }

        if (!userRepo.getUserById(login.getUserId()).isPresent()) {
            response.sendRedirect(request.getContextPath());
            return;
        }

        request.getRequestDispatcher(Constants.USER_SETTINGS_JSP).forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (!checkEnabled()) {
            // Send users to the home page
            response.sendRedirect(request.getContextPath());
            return;
        }

        String responsePath = request.getContextPath() + Paths.USER_SETTINGS;

        final String formType = ServletUtils.formType(request);
        switch (formType) {
            case "updateKeyMap": {
                final String parameter = ServletUtils.getStringParameter(request, "editorKeyMap").orElse(null);
                final KeyMap editorKeyMap = KeyMap.valueOrDefault(parameter);
                if (updateUserKeyMap(login.getUserEntity(), editorKeyMap)) {
                    login.getUserEntity().setKeyMap(editorKeyMap);
                    messages.add("Successfully updated editor preference.");
                } else {
                    logger.info("Failed to update editor preference for user {}.", login.getUserId());
                    messages.add("Failed to update editor preference.");
                }
                Redirect.redirectBack(request, response);
                return;
            }

            case "updateProfile": {
                final Optional<String> email = ServletUtils.getStringParameter(request, "updatedEmail");
                boolean allowContact = ServletUtils.parameterThenOrOther(request, "allowContact", true, false);
                final boolean success = updateUserInformation(login.getUserEntity(), email, allowContact);
                if (success) {
                    messages.add("Successfully updated profile information.");
                } else {
                    logger.info("Failed to update profile information for user {}.", login.getUserId());
                    messages.add("Failed to update profile information. Please contact the page administrator.");
                }
                response.sendRedirect(responsePath);
                return;
            }

            case "changePassword": {
                final Optional<String> password = ServletUtils.getStringParameter(request, "updatedPassword");

                if (isPasswordValid(password)) {
                    final boolean success = changeUserPassword(login.getUserEntity(), password.get());
                    if (success) {
                        messages.add("Successfully changed password.");
                    } else {
                        logger.info("Failed to change password for user {}.", login.getUserId());
                        messages.add("Failed to change password. Please contact the page administrator.");
                    }
                } else {
                    // Additional validation if frontend check is bypassed
                    messages.add("No or invalid password provided.");
                }
                response.sendRedirect(responsePath);
                return;
            }

            case "deleteAccount": {
                // Does not actually delete the account but pseudomizes it
                final boolean success = removeUserInformation(login.getUserEntity());
                if (success) {
                    logger.info("User {} successfully set themselves as inactive.", login.getUserId());
                    /*
                     * Send the user to Paths.LOGOUT so we can correctly clean up session
                     * information. Note that this will automatically take the user to LANDING_PAGE,
                     * hence no confirmation messages will be shown (unless we make LANDING_PAGE do
                     * so)
                     */
                    // messages.add("You successfully deleted your account. Sad to see you go. :(");
                    response.sendRedirect(request.getContextPath() + Paths.LOGOUT);
                } else {
                    logger.info("Failed to set user {} as inactive.", login.getUserId());
                    messages.add("Failed to set your account as inactive. Please contact the page administrator.");
                    response.sendRedirect(responsePath);
                }
                return;
            }

            default:
                logger.error("Action {" + formType + "} not recognised.");
                response.sendRedirect(responsePath);
        }
    }

    private boolean updateUserKeyMap(UserEntity user, KeyMap editorKeyMap) {
        if (user == null) {
            return false;
        }
        user.setKeyMap(editorKeyMap);
        return user.update();
    }

    private boolean updateUserInformation(UserEntity user, Optional<String> email, boolean allowContact) {
        if (user == null) {
            return false;
        }

        email.ifPresent(user::setEmail);
        user.setAllowContact(allowContact);

        return user.update();
    }

    private boolean isPasswordValid(Optional<String> password) {
        final CodeDefendersValidator validator = new CodeDefendersValidator();
        return password.isPresent() && validator.validPassword(password.get());
    }

    private boolean changeUserPassword(UserEntity user, String password) {
        if (user == null) {
            return false;
        }

        user.setEncodedPassword(UserEntity.encodePassword(password));
        return user.update();
    }

    private boolean removeUserInformation(UserEntity user) {
        if (user == null) {
            return false;
        }
        user.setActive(false);
        user.setUsername(DELETED_USER_NAME);
        user.setEmail(String.format(DELETED_USER_EMAIL, UUID.randomUUID()));
        user.setEncodedPassword(DELETED_USER_PASSWORD);
        return user.update() && userService.deleteRecordedSessions(user.getId());
    }
}
