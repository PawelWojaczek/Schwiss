package com.pwojaczek;

import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.*;
import java.util.stream.IntStream;

public class MatchService {
    private static final int LAST_MATCHES_TO_CHECK = 2;

    private int rounds;
    private boolean oddPlayers;
    private TournamentType tournamentType;
    private List<Player> players = new ArrayList<>();

    public void initTournament() {
        try {
            prepareData();
        } catch (IOException e) {
            throw new RuntimeException("Error while loading data from JSON", e);
        }

        if (players.size() % 2 == 1) {
            oddPlayers = true;
        }
        IntStream.rangeClosed(1, rounds).forEach(pairingNumber -> {
            List<Match> matches;
            if (pairingNumber == 1) {
                matches = createPairsRandom(players);
            } else {
                matches = createPairs(players);
            }
            if (tournamentType == TournamentType.AUTOMATIC) {
                createOutcomes(matches);
            } else {
                printOutcomes(pairingNumber, matches);
                enterOutcomes(matches);
            }
            printOutcomes(pairingNumber, matches);
            appendPointsForMatches(matches);
        });
        printResults(players);
    }

    /**
     * Method prepares data of the torunament from config file.
     * Config file contains numbers of rounds in the torunament and player names.
     */
    private void prepareData() throws IOException {
        InputStream is =
                new FileInputStream(new File("config.txt"));
        String jsonTxt = IOUtils.toString(is, Charset.forName("UTF-8"));
        JSONObject jsonObject = new JSONObject(new JSONTokener(jsonTxt));
        rounds = jsonObject.getInt("rounds");
        tournamentType = TournamentType.valueOf(jsonObject.getString("type"));
        JSONArray jsonPlayers = jsonObject.getJSONArray("players");

        if (jsonPlayers.length() <= rounds) {
            throw new RuntimeException("Players count is not enought to play a torunament with " + rounds + " rounds");
        }
        for (Object player : jsonPlayers) {
            players.add(new Player(player.toString()));
        }
    }

    /**
     * Method assigns free point if there is odd number of players.
     * Player gets free point if there is no lowest score than him or lower scores already received a free point.
     */
    private Player assignFreePoint(List<Player> players) {
        sortPlayersByScoreAscending(players);
        for (Player player : players) {
            if (!player.receivedFreePoint()) {
                player.setFreePoint(true);
                players.remove(player);
                return player;
            }
        }
        throw new RuntimeException("No player to assign free point to.");
    }

    private List<Match> createPairs(List<Player> players) {
        List<Match> matches = new ArrayList<>();
        List<Player> tempPlayers = new ArrayList<>(players);
        Player freePointPlayer = null;
        if (oddPlayers) {
            freePointPlayer = assignFreePoint(tempPlayers);
        }
        sortPlayersByScore(tempPlayers);
        int matchesCount = Math.floorDiv(players.size(), 2);
        for (int i = 0; i < matchesCount; i++) {
            matches.add(createPair(tempPlayers));
        }
        if (freePointPlayer != null) {
            matches.add(new Match(freePointPlayer, OutcomeEnum.FREE_POINT));
        }
        return matches;
    }

    private List<Match> createPairsRandom(List<Player> players) {
        sortPlayersByScore(players);
        List<Match> matches = new ArrayList<>();
        List<Player> tempPlayers = new ArrayList<>(players);
        Player freePointPlayer = null;
        if (oddPlayers) {
            freePointPlayer = assignFreePoint(tempPlayers);
        }
        int matchesCount = Math.floorDiv(players.size(), 2);
        for (int i = 0; i < matchesCount; i++) {
            matches.add(createPairRandom(tempPlayers));
        }
        if (freePointPlayer != null) {
            matches.add(new Match(freePointPlayer, OutcomeEnum.FREE_POINT));
        }
        return matches;
    }

    private Match createPairRandom(List<Player> players) {
        Random random = new Random();
        Player player1 = players.get(0);
        players.remove(player1);
        Player player2;

        player2 = players.get(random.nextInt(players.size()));
        int properSetup = getPlayersColorMatchup(player1, player2);
        players.remove(player2);
        Match match = createMatchBasedOnSetup(player1, player2, properSetup);
        player1.addMatch(match);
        player2.addMatch(match);
        return match;
    }

    private Match createPair(List<Player> players) {
        Player player1 = players.get(0);
        players.remove(player1);
        Player player2;

        for (int i = 0; i < players.size(); i++) {
            player2 = players.get(i);
            int properSetup = getPlayersColorMatchup(player1, player2);
            List<Player> playersToCheck = new ArrayList<>(players);
            playersToCheck.remove(player2);
            if (!player1.playedWith(player2) && properSetup != -1 && isPairingPossibleBruteForce(playersToCheck)) {
                players.remove(player2);
                Match match = createMatchBasedOnSetup(player1, player2, properSetup);
                player1.addMatch(match);
                player2.addMatch(match);
                return match;
            }
        }
        throw new RuntimeException("Could not create pair");
    }

    private boolean isPairingPossibleBruteForce(List<Player> players) {
        List<Player> tempPlayers = new ArrayList<>(players);
        int matchesCount = Math.floorDiv(players.size(), 2);
        return IntStream.range(0, matchesCount).allMatch(a -> {
            Player player1 = tempPlayers.get(0);
            tempPlayers.remove(player1);
            Player player2;
            for (int i = 0; i < tempPlayers.size(); i++) {
                player2 = tempPlayers.get(i);
                int properSetup = getPlayersColorMatchup(player1, player2);
                List<Player> playersToCheck = new ArrayList<>(tempPlayers);
                playersToCheck.remove(player2);
                if (!player1.playedWith(player2) && properSetup != -1 && isPairingPossibleBruteForce(playersToCheck)) {
                    tempPlayers.remove(player2);
                    return true;
                }
            }
            return false;
        });
    }

    /**
     * Create Match based on color Setup.
     * Create match with player1 as white, if colorSetup is 1.
     * Create match with player2 as white, if colorSetup is 2.
     * If colorSetup is 0, check playerScores -  player with highest score gets white color.
     * If both have the same amount of points, white goes for player1.
     */
    private Match createMatchBasedOnSetup(Player player1, Player player2, int colorSetup) {
        if (colorSetup == 1) {
            return new Match(player1, player2);
        } else if (colorSetup == 2) {
            return new Match(player2, player1);
        } else {
            if (player1.getScore() < player2.getScore()) {
                return createMatchBasedOnSetup(player1, player2, 2);
            } else {
                return createMatchBasedOnSetup(player1, player2, 1);
            }
        }
    }

    private void removeFreePointMatches(List<Match> playerMatches) {
        Optional<Match> freePointMatch = playerMatches.stream().filter(match -> match.getOutcome() == OutcomeEnum.FREE_POINT).findAny();
        freePointMatch.ifPresent(playerMatches::remove);
    }

    /**
     * Check if players can get proper colors and get best matching color setup.
     * Return -1, if there can't be proper color assigned
     * Return 1, if player1 should play white
     * Return 2, if player2 should play black
     * Return 0, if color doesn't matter
     */
    private int getPlayersColorMatchup(Player player1, Player player2) {
        boolean player1MustPlayBlack = false;
        boolean player2MustPlayBlack = false;
        boolean player1MustPlayWhite = false;
        boolean player2MustPlayWhite = false;

        List<Match> matchesPlayer1Copy = new ArrayList<>(player1.getMatchHistory());
        List<Match> matchesPlayer2Copy = new ArrayList<>(player2.getMatchHistory());
        removeFreePointMatches(matchesPlayer1Copy);
        removeFreePointMatches(matchesPlayer2Copy);

        int matchesPlayedSizePlayer1 = matchesPlayer1Copy.size();
        int matchesPlayedSizePlayer2 = matchesPlayer2Copy.size();
        long player1WhiteCount;
        long player2WhiteCount;

        if (matchesPlayedSizePlayer1 > 2) {
            player1WhiteCount = matchesPlayer1Copy.subList(matchesPlayedSizePlayer1 - LAST_MATCHES_TO_CHECK, matchesPlayedSizePlayer1).stream().filter(match -> match.getPlayer1().equals(player1)).count();
        } else {
            player1WhiteCount = matchesPlayer1Copy.stream().filter(match -> match.getPlayer1().equals(player1)).count();
        }
        if (matchesPlayedSizePlayer2 > 2) {
            player2WhiteCount = matchesPlayer2Copy.subList(matchesPlayedSizePlayer2 - LAST_MATCHES_TO_CHECK, matchesPlayedSizePlayer2).stream().filter(match -> match.getPlayer1().equals(player2)).count();
        } else {
            player2WhiteCount = matchesPlayer2Copy.stream().filter(match -> match.getPlayer1().equals(player2)).count();
        }

        // Players can't play the same color three times in a row.
        if (player1WhiteCount == 2) {
            player1MustPlayBlack = true;
        } else if (player1WhiteCount == 0 && matchesPlayedSizePlayer1 > 1) {
            player1MustPlayWhite = true;
        }
        if (player2WhiteCount == 2) {
            player2MustPlayBlack = true;
        } else if (player2WhiteCount == 0 && matchesPlayedSizePlayer1 > 1) {
            player2MustPlayWhite = true;
        }

        if (player2MustPlayBlack && player1MustPlayBlack || player2MustPlayWhite && player1MustPlayWhite) {
            return -1;
        }
        if (player1MustPlayBlack || player2MustPlayWhite) {
            return 2;
        }
        if (player1MustPlayWhite || player2MustPlayBlack) {
            return 1;
        }

        // if they didn't play 2 colors in a row, check which color was played more times.
        player1WhiteCount = matchesPlayer1Copy.stream().filter(match -> match.getPlayer1().equals(player1)).count();
        player2WhiteCount = matchesPlayer2Copy.stream().filter(match -> match.getPlayer1().equals(player2)).count();

        boolean player1ShouldPlayBlack = false;
        boolean player2ShouldPlayBlack = false;
        boolean player1ShouldPlayWhite = false;
        boolean player2ShouldPlayWhite = false;

        if (player1WhiteCount * 2 > matchesPlayedSizePlayer1) {
            player1ShouldPlayBlack = true;
        } else if (player1WhiteCount * 2 < matchesPlayedSizePlayer1) {
            player1ShouldPlayWhite = true;
        }
        if (player2WhiteCount * 2 > matchesPlayedSizePlayer1) {
            player2ShouldPlayBlack = true;
        } else if (player2WhiteCount * 2 < matchesPlayedSizePlayer1) {
            player2ShouldPlayWhite = true;
        }

        if (player1ShouldPlayBlack && player2ShouldPlayWhite) {
            return 2;
        } else if (player1ShouldPlayWhite && player2ShouldPlayBlack) {
            return 1;
        } else if (player1ShouldPlayBlack || player2ShouldPlayWhite) {
            return 2;
        } else if (player1ShouldPlayWhite || player2ShouldPlayBlack) {
            return 1;
        } else {
            return 0;
        }
    }

    /**
     * Method randomly creates outcomes for matches 1-3.
     * 1 - player 1 wins
     * 2 - player 2 wins
     * 3 - draw
     */
    private void createOutcomes(List<Match> matches) {
        Random random = new Random();
        for (Match match : matches) {
            if (match.getOutcome() != OutcomeEnum.FREE_POINT) {
                int outcome = random.nextInt(3) + 1;
                setOutcome(match, outcome);
            }
        }

    }

    /**
     * Method tells user to enter outcomes for matches.
     * User should enter:
     * 1 to set player 1 as winning
     * 2 to set player 2 as winning
     * 3 to set draw
     */
    private void enterOutcomes(List<Match> matches) {
        List<Match> matchesCopy = new ArrayList<>(matches);
        for (Match match : matchesCopy) {
            if (match.getOutcome() != OutcomeEnum.FREE_POINT) {
                printMatchup(match.getPlayer1(), match.getPlayer2());
                int outcome = getOutcomeFromUser();
                setOutcome(match, outcome);
            }
        }
        System.out.println("Save scores? y/n");
        Scanner scanner = new Scanner(System.in);
        String response = "";
        while (!response.equalsIgnoreCase("y") && !response.equalsIgnoreCase("n")) {
            response = scanner.next();
        }
        if (response.equalsIgnoreCase("y")) {
            IntStream.range(0, matchesCopy.size()).forEach(i -> {
                matches.get(i).setOutcome(matchesCopy.get(i).getOutcome());
            });
        } else {
            enterOutcomes(matches);
        }
    }

    private int getOutcomeFromUser() {
        Scanner reader = new Scanner(System.in);
        int outcome = -1;
        while (outcome != 1 && outcome != 2 && outcome != 3) {
            outcome = reader.nextInt();
        }
        return outcome;
    }

    private void appendPointsForMatches(List<Match> matches) {
        for (Match match : matches) {
            appendPoints(match);
        }
    }

    private void setOutcome(Match match, int number) {
        if (number == 3) {
            match.setOutcome(OutcomeEnum.DRAW);
        } else if (number == 1) {
            match.setOutcome(OutcomeEnum.WIN);
        } else if (number == 2) {
            match.setOutcome(OutcomeEnum.LOSS);
        }
    }

    private void appendPoints(Match match) {
        Player player1 = match.getPlayer1();
        Player player2 = match.getPlayer2();
        OutcomeEnum outcomeEnum = match.getOutcome();
        if (outcomeEnum == OutcomeEnum.FREE_POINT) {
            player1.win();
        } else if (outcomeEnum == OutcomeEnum.DRAW) {
            player1.draw();
            player2.draw();
        } else if (outcomeEnum == OutcomeEnum.WIN) {
            player1.win();
        } else if (outcomeEnum == OutcomeEnum.LOSS) {
            player2.win();
        }
    }

    /**
     * Method prints fancy outcome of a pairing round in console.
     */
    private void printOutcomes(int pairingNumber, List<Match> matches) {
        System.out.println("############################ PAIRING " + pairingNumber + " #############################");
        for (Match match : matches) {
            if (match.getOutcome() == OutcomeEnum.FREE_POINT) {
                System.out.format("Free point: " + match.getPlayer1().getName() + "(" + match.getPlayer1().getScore() + ")\n");
            } else {
                Player player1 = match.getPlayer1();
                Player player2 = match.getPlayer2();
                OutcomeEnum outcomeEnum = match.getOutcome();
                if (outcomeEnum != null) {
                    printMatchup(player1, player2, outcomeEnum.getOutcome());
                } else {
                    printMatchup(player1, player2);
                }
            }
        }
        System.out.println("####################################################################\n");
    }

    private void printMatchup(Player player1, Player player2) {
        printMatchup(player1, player2, " - ");
    }

    private void printMatchup(Player player1, Player player2, String outcome) {
        System.out.format("%-28s%8s%30s", player1.getName() + "(" + player1.getScore() + ")", outcome, player2.getName() + "(" + player2.getScore() + ")\n");
    }

    /**
     * Method sorts players by Score from biggest to lowest score.
     */
    private void sortPlayersByScore(List<Player> players) {
        players.sort(Comparator.comparing(Player::getScore).reversed());
    }

    private void sortPlayersByScoreAscending(List<Player> players) {
        players.sort(Comparator.comparing(Player::getScore));
    }

    /**
     * Method prints final results of a tournament.
     */
    private void printResults(List<Player> players) {
        sortPlayersByScore(players);
        System.out.println("#################### RESULTS ####################");
        for (Player player : players) {
            System.out.format("%-32s\t\t%.1f\n", player.getName(), player.getScore());
        }
        System.out.println("#################################################");
    }
}
