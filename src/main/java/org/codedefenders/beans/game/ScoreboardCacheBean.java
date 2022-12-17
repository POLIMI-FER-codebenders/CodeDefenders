package org.codedefenders.beans.game;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.annotation.ManagedBean;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.codedefenders.database.EventDAO;
import org.codedefenders.database.GameDAO;
import org.codedefenders.database.MeleeGameDAO;
import org.codedefenders.database.MultiplayerGameDAO;
import org.codedefenders.database.MutantDAO;
import org.codedefenders.database.ScoreDAO;
import org.codedefenders.database.TestDAO;
import org.codedefenders.dto.api.AttackerScore;
import org.codedefenders.dto.api.DefenderScore;
import org.codedefenders.dto.api.DuelsCount;
import org.codedefenders.dto.api.MeleeScore;
import org.codedefenders.dto.api.MeleeScoreboard;
import org.codedefenders.dto.api.MultiplayerScoreboard;
import org.codedefenders.dto.api.MutantsCount;
import org.codedefenders.dto.api.Scoreboard;
import org.codedefenders.dto.api.TestsCount;
import org.codedefenders.game.AbstractGame;
import org.codedefenders.game.multiplayer.MeleeGame;
import org.codedefenders.game.multiplayer.MultiplayerGame;
import org.codedefenders.game.multiplayer.PlayerScore;
import org.codedefenders.game.scoring.ScoreCalculator;
import org.codedefenders.model.Player;
import org.codedefenders.model.UserEntity;
import org.codedefenders.util.Constants;

import com.google.gson.Gson;

@ManagedBean
@ApplicationScoped
public class ScoreboardCacheBean {
    private final HashMap<Integer, Scoreboard> scoreboards = new HashMap<>();
    private ScoreboardBean scoreboardBean;
    private ScoreCalculator scoreCalculator;
    private MeleeScoreboardBean meleeScoreboardBean;
    private EventDAO eventDAO;

    @Inject
    public ScoreboardCacheBean(ScoreboardBean scoreboardBean, ScoreCalculator scoreCalculator, MeleeScoreboardBean meleeScoreboardBean, EventDAO eventDAO) {
        this.scoreboardBean = scoreboardBean;
        this.scoreCalculator = scoreCalculator;
        this.meleeScoreboardBean = meleeScoreboardBean;
        this.eventDAO = eventDAO;
        scoreboards.putAll(MultiplayerGameDAO.getAvailableMultiplayerGames().stream().collect(Collectors.toMap(MultiplayerGame::getId, this::getMultiplayerScoreboard)));
        scoreboards.putAll(MeleeGameDAO.getAvailableMeleeGames().stream().collect(Collectors.toMap(MeleeGame::getId, this::getMeleeScoreboard)));
    }

    private static int[] slashStringToArray(String s) {
        return Arrays.stream(s.split(" / ")).mapToInt(Integer::valueOf).toArray();
    }

    public MeleeScoreboard getMeleeScoreboard(Integer gameId) {
        return getMeleeScoreboard((MeleeGame) Objects.requireNonNull(GameDAO.getGame(gameId)));
    }

    public MeleeScoreboard getMeleeScoreboard(MeleeGame game) {
        MeleeScoreboard scoreboard = new MeleeScoreboard();
        meleeScoreboardBean.setGameId(game.getId());
        meleeScoreboardBean.setScores(scoreCalculator.getMutantScores(game.getId()), scoreCalculator.getTestScores(game.getId()), scoreCalculator.getDuelScores(game.getId()));
        meleeScoreboardBean.setPlayers(game.getPlayers());
        for (ScoreItem scoreItem : meleeScoreboardBean.getSortedScoreItems()) {
            scoreboard.addPlayer(new MeleeScore(scoreItem.getUser().getName(), scoreItem.getUser().getId(), scoreItem.getAttackScore().getPlayerId(), scoreItem.getAttackScore().getTotalScore() + scoreItem.getDefenseScore().getTotalScore() + scoreItem.getDuelScore().getTotalScore(), scoreItem.getAttackScore().getTotalScore(), scoreItem.getDefenseScore().getTotalScore(), scoreItem.getDuelScore().getTotalScore()));
        }
        return scoreboard;
    }

    public MultiplayerScoreboard getMultiplayerScoreboard(Integer gameId) {
        return getMultiplayerScoreboard((MultiplayerGame) Objects.requireNonNull(GameDAO.getGame(gameId)));
    }

    public MultiplayerScoreboard getMultiplayerScoreboard(MultiplayerGame game) {
        MultiplayerScoreboard scoreboard = new MultiplayerScoreboard();
        scoreboardBean.setGameId(game.getId());
        scoreboardBean.setScores(game.getMutantScores(), game.getTestScores());
        scoreboardBean.setPlayers(game.getAttackerPlayers(), game.getDefenderPlayers());
        Map<Integer, PlayerScore> mutantScores = scoreboardBean.getMutantsScores();
        Map<Integer, PlayerScore> testScores = scoreboardBean.getTestScores();
        final List<Player> attackers = scoreboardBean.getAttackers();
        final List<Player> defenders = scoreboardBean.getDefenders();

        int[] mki;
        int[] mdi;
        //battleground attackers
        PlayerScore zeroDummyScore = new PlayerScore(-1);
        zeroDummyScore.setMutantKillInformation("0 / 0 / 0");
        zeroDummyScore.setDuelInformation("0 / 0 / 0");
        for (Player attacker : attackers) {
            int playerId = attacker.getId();
            UserEntity attackerUser = attacker.getUser();
            if (attackerUser.getId() == Constants.DUMMY_ATTACKER_USER_ID && MutantDAO.getMutantsByGameAndUser(scoreboardBean.getGameId(), attackerUser.getId()).isEmpty()) {
                continue;
            }
            PlayerScore mutantsScore = mutantScores.getOrDefault(playerId, zeroDummyScore);
            PlayerScore testsScore = testScores.getOrDefault(playerId, zeroDummyScore);
            mki = slashStringToArray(mutantsScore.getMutantKillInformation());
            mdi = slashStringToArray(mutantsScore.getDuelInformation());
            scoreboard.addAttacker(new AttackerScore(attackerUser.getUsername(), attackerUser.getId(), playerId, mutantsScore.getTotalScore() + testsScore.getTotalScore(), new DuelsCount(mdi[0], mdi[1], mdi[2]), new MutantsCount(mki[0], mki[1], mki[2])));
        }

        //battleground attacker total
        mki = slashStringToArray(mutantScores.getOrDefault(-1, zeroDummyScore).getMutantKillInformation());
        mdi = slashStringToArray(mutantScores.getOrDefault(-1, zeroDummyScore).getDuelInformation());
        scoreboard.setAttackersTotal(new AttackerScore("Total", -1, -1, mutantScores.getOrDefault(-1, zeroDummyScore).getTotalScore() + testScores.getOrDefault(-2, zeroDummyScore).getTotalScore(), new DuelsCount(mdi[0], mdi[1], mdi[2]), new MutantsCount(mki[0], mki[1], mki[2])));

        //battleground defenders
        for (Player defender : defenders) {
            int playerId = defender.getId();
            UserEntity defenderUser = defender.getUser();

            if (defenderUser.getId() == Constants.DUMMY_DEFENDER_USER_ID && TestDAO.getTestsForGameAndUser(scoreboardBean.getGameId(), defenderUser.getId()).isEmpty()) {
                continue;
            }

            PlayerScore testsScore = testScores.getOrDefault(playerId, zeroDummyScore);
            int killing = Integer.parseInt(testsScore.getMutantKillInformation());
            mdi = slashStringToArray(testsScore.getDuelInformation());
            scoreboard.addDefender(new DefenderScore(defenderUser.getUsername(), defenderUser.getId(), playerId, testsScore.getTotalScore(), new DuelsCount(mdi[0], mdi[1], mdi[2]), new TestsCount(killing, testsScore.getQuantity() - killing)));
        }

        //battleground defenders total
        int killing = Integer.parseInt(testScores.getOrDefault(-1, zeroDummyScore).getMutantKillInformation());
        mdi = slashStringToArray(testScores.getOrDefault(-1, zeroDummyScore).getDuelInformation());
        scoreboard.setDefendersTotal(new DefenderScore("Total", -1, -1, testScores.getOrDefault(-1, zeroDummyScore).getTotalScore(), new DuelsCount(mdi[0], mdi[1], mdi[2]), new TestsCount(killing, testScores.getOrDefault(-1, zeroDummyScore).getQuantity() - killing)));
        return scoreboard;
    }

    public void updateScoreboard(Integer eventId, Integer gameId) {
        AbstractGame game = GameDAO.getGame(gameId);
        String update = null;
        if (game instanceof MultiplayerGame) {
            update = updateMultiplayerScoreboard((MultiplayerGame) game);
        } else {
            //TODO never
        }
        if (update != null) {
            ScoreDAO.insert(eventId, update);
        }
    }

    public String updateMultiplayerScoreboard(MultiplayerGame game) {
        MultiplayerScoreboard newScoreboard = getMultiplayerScoreboard(game);
        System.out.println(new Gson().toJson(newScoreboard));
        System.out.println(newScoreboard != scoreboards.get(game.getId()));
        if (newScoreboard != scoreboards.get(game.getId())) {
            scoreboards.put(game.getId(), newScoreboard);
            return new Gson().toJson(newScoreboard);
        } else {
            return null;
        }
    }
}
