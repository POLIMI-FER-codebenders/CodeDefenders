package org.codedefenders.servlets.util.api;

import org.codedefenders.beans.admin.StagedGameList;
import org.codedefenders.database.GameClassDAO;
import org.codedefenders.dto.api.APIGameSettings;
import org.codedefenders.dto.api.NewGameRequest;
import org.codedefenders.game.Role;

public class Transformers {
    public static StagedGameList.GameSettings NewGameRequestToGameSettings(NewGameRequest game) {
        StagedGameList.GameSettings gameSettings=new StagedGameList.GameSettings();
        APIGameSettings apiGameSettings=game.getSettings();
        gameSettings.setGameType(apiGameSettings.getGameType());
        gameSettings.setCut(GameClassDAO.getClassForId(game.getClassId()));
        gameSettings.setWithMutants(false);
        gameSettings.setWithTests(false);
        gameSettings.setMaxAssertionsPerTest(apiGameSettings.getMaxAssertionsPerTest());
        gameSettings.setMutantValidatorLevel(apiGameSettings.getMutantValidatorLevel());
        gameSettings.setChatEnabled(true);
        gameSettings.setCaptureIntentions(false);
        gameSettings.setEquivalenceThreshold(apiGameSettings.getAutoEquivalenceThreshold());
        gameSettings.setLevel(apiGameSettings.getGameLevel());
        gameSettings.setCreatorRole(Role.OBSERVER);
        gameSettings.setStartGame(false);
        return gameSettings;
    }
}
