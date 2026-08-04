package dev.sahilbasumatary.tennisdataservice.provider;

import dev.sahilbasumatary.tennisdataservice.dto.PlayerData;
import dev.sahilbasumatary.tennisdataservice.dto.RankingData;
import dev.sahilbasumatary.tennisdataservice.dto.TournamentData;
import dev.sahilbasumatary.tennisdataservice.entity.Backhand;
import dev.sahilbasumatary.tennisdataservice.entity.Gender;
import dev.sahilbasumatary.tennisdataservice.entity.Hand;
import dev.sahilbasumatary.tennisdataservice.entity.RankingType;
import dev.sahilbasumatary.tennisdataservice.entity.Surface;
import dev.sahilbasumatary.tennisdataservice.entity.TournamentLevel;
import jakarta.annotation.PostConstruct;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "tennis.data.provider", havingValue = "mock", matchIfMissing = true)
public class MockTennisDataProvider implements TennisDataProvider {

    private static final Logger log = LoggerFactory.getLogger(MockTennisDataProvider.class);
    private List<PlayerData> players;
    private List<TournamentData> tournaments;
    private List<RankingData> rankings;

    @PostConstruct
    void init() {
        this.players = buildPlayers();
        this.tournaments = buildTournaments();
        this.rankings = buildRankings();
        log.info("MockTennisDataProvider initialized with {} players, {} tournaments, {} rankings",
                players.size(), tournaments.size(), rankings.size());
    }

    @Override
    public List<PlayerData> fetchPlayers() {
        return Collections.unmodifiableList(players);
    }

    @Override
    public Optional<PlayerData> fetchPlayerById(String externalId) {
        return players.stream()
                .filter(p -> p.externalId().equals(externalId))
                .findFirst();
    }

    @Override
    public List<RankingData> fetchRankings() {
        return Collections.unmodifiableList(rankings);
    }

    @Override
    public List<TournamentData> fetchTournaments() {
        return Collections.unmodifiableList(tournaments);
    }

    private List<PlayerData> buildPlayers() {
        List<PlayerData> list = new ArrayList<>();
        // ATP top 30
        list.add(atp("atp-001", "Jannik", "Sinner", "ITA", 2001, 8, 16, Hand.RIGHT, Backhand.TWO_HANDED, 188, 77, 2019, 1, 11830));
        list.add(atp("atp-002", "Alexander", "Zverev", "DEU", 1997, 4, 20, Hand.RIGHT, Backhand.TWO_HANDED, 198, 90, 2015, 2, 8135));
        list.add(atp("atp-003", "Carlos", "Alcaraz", "ESP", 2003, 5, 5, Hand.RIGHT, Backhand.TWO_HANDED, 183, 74, 2018, 3, 7010));
        list.add(atp("atp-004", "Taylor", "Fritz", "USA", 1997, 10, 28, Hand.RIGHT, Backhand.TWO_HANDED, 193, 86, 2015, 4, 5400));
        list.add(atp("atp-005", "Daniil", "Medvedev", "RUS", 1996, 2, 11, Hand.RIGHT, Backhand.TWO_HANDED, 198, 83, 2015, 5, 5190));
        list.add(atp("atp-006", "Casper", "Ruud", "NOR", 1998, 12, 22, Hand.RIGHT, Backhand.TWO_HANDED, 183, 77, 2017, 6, 4210));
        list.add(atp("atp-007", "Novak", "Djokovic", "SRB", 1987, 5, 22, Hand.RIGHT, Backhand.TWO_HANDED, 188, 77, 2003, 7, 3900));
        list.add(atp("atp-008", "Alex", "de Minaur", "AUS", 1999, 2, 17, Hand.RIGHT, Backhand.TWO_HANDED, 183, 76, 2018, 8, 3750));
        list.add(atp("atp-009", "Andrey", "Rublev", "RUS", 1997, 10, 20, Hand.RIGHT, Backhand.TWO_HANDED, 188, 75, 2014, 9, 3470));
        list.add(atp("atp-010", "Grigor", "Dimitrov", "BGR", 1991, 5, 16, Hand.RIGHT, Backhand.ONE_HANDED, 191, 80, 2008, 10, 3300));
        list.add(atp("atp-011", "Lorenzo", "Musetti", "ITA", 2002, 3, 3, Hand.RIGHT, Backhand.ONE_HANDED, 185, 76, 2019, 11, 3055));
        list.add(atp("atp-012", "Tommy", "Paul", "USA", 1997, 5, 17, Hand.RIGHT, Backhand.TWO_HANDED, 185, 84, 2018, 12, 2880));
        list.add(atp("atp-013", "Holger", "Rune", "DNK", 2003, 4, 29, Hand.RIGHT, Backhand.TWO_HANDED, 188, 78, 2020, 13, 2750));
        list.add(atp("atp-014", "Stefanos", "Tsitsipas", "GRC", 1998, 8, 12, Hand.RIGHT, Backhand.ONE_HANDED, 193, 89, 2017, 14, 2640));
        list.add(atp("atp-015", "Jack", "Draper", "GBR", 2001, 12, 22, Hand.LEFT, Backhand.TWO_HANDED, 193, 84, 2021, 15, 2510));
        list.add(atp("atp-016", "Frances", "Tiafoe", "USA", 1998, 1, 20, Hand.RIGHT, Backhand.TWO_HANDED, 188, 86, 2017, 16, 2340));
        list.add(atp("atp-017", "Hubert", "Hurkacz", "POL", 1997, 2, 11, Hand.RIGHT, Backhand.TWO_HANDED, 196, 83, 2018, 17, 2200));
        list.add(atp("atp-018", "Ben", "Shelton", "USA", 2002, 10, 9, Hand.LEFT, Backhand.TWO_HANDED, 193, 88, 2022, 18, 2080));
        list.add(atp("atp-019", "Sebastian", "Korda", "USA", 2000, 7, 5, Hand.RIGHT, Backhand.TWO_HANDED, 196, 84, 2020, 19, 1960));
        list.add(atp("atp-020", "Ugo", "Humbert", "FRA", 1998, 6, 26, Hand.LEFT, Backhand.TWO_HANDED, 188, 73, 2018, 20, 1850));
        list.add(atp("atp-021", "Felix", "Auger-Aliassime", "CAN", 2000, 8, 8, Hand.RIGHT, Backhand.TWO_HANDED, 193, 89, 2019, 21, 1730));
        list.add(atp("atp-022", "Karen", "Khachanov", "RUS", 1996, 5, 21, Hand.RIGHT, Backhand.TWO_HANDED, 198, 85, 2013, 22, 1650));
        list.add(atp("atp-023", "Arthur", "Fils", "FRA", 2004, 3, 13, Hand.LEFT, Backhand.TWO_HANDED, 188, 80, 2022, 23, 1540));
        list.add(atp("atp-024", "Alejandro", "Tabilo", "CHL", 1997, 6, 2, Hand.LEFT, Backhand.TWO_HANDED, 183, 77, 2019, 24, 1430));
        list.add(atp("atp-025", "Francisco", "Cerundolo", "ARG", 1998, 11, 16, Hand.RIGHT, Backhand.TWO_HANDED, 185, 81, 2018, 25, 1320));
        list.add(atp("atp-026", "Tomas", "Machac", "CZE", 2000, 10, 1, Hand.RIGHT, Backhand.TWO_HANDED, 188, 79, 2019, 26, 1280));
        list.add(atp("atp-027", "Matteo", "Berrettini", "ITA", 1996, 4, 12, Hand.RIGHT, Backhand.TWO_HANDED, 196, 95, 2015, 27, 1210));
        list.add(atp("atp-028", "Alexander", "Bublik", "KAZ", 1997, 6, 17, Hand.RIGHT, Backhand.TWO_HANDED, 196, 81, 2017, 28, 1140));
        list.add(atp("atp-029", "Nicolas", "Jarry", "CHL", 1995, 10, 11, Hand.RIGHT, Backhand.TWO_HANDED, 198, 96, 2016, 29, 1080));
        list.add(atp("atp-030", "Flavio", "Cobolli", "ITA", 2002, 5, 30, Hand.RIGHT, Backhand.TWO_HANDED, 183, 74, 2021, 30, 1010));
        list.add(atp("atp-031", "Joao", "Fonseca", "BRA", 2006, 8, 31, Hand.RIGHT, Backhand.TWO_HANDED, 185, 78, 2023, 45, 620));
        // WTA top 25
        list.add(wta("wta-001", "Aryna", "Sabalenka", "BLR", 1998, 5, 5, Hand.RIGHT, Backhand.TWO_HANDED, 182, 82, 2015, 1, 10585));
        list.add(wta("wta-002", "Iga", "Swiatek", "POL", 2001, 5, 31, Hand.RIGHT, Backhand.TWO_HANDED, 176, 57, 2019, 2, 8460));
        list.add(wta("wta-003", "Coco", "Gauff", "USA", 2004, 3, 13, Hand.RIGHT, Backhand.TWO_HANDED, 175, 68, 2019, 3, 7150));
        list.add(wta("wta-004", "Jasmine", "Paolini", "ITA", 1998, 1, 4, Hand.RIGHT, Backhand.TWO_HANDED, 163, 58, 2018, 4, 5590));
        list.add(wta("wta-005", "Qinwen", "Zheng", "CHN", 2002, 10, 8, Hand.RIGHT, Backhand.TWO_HANDED, 178, 65, 2019, 5, 5070));
        list.add(wta("wta-006", "Elena", "Rybakina", "KAZ", 1999, 6, 17, Hand.RIGHT, Backhand.TWO_HANDED, 184, 72, 2018, 6, 4320));
        list.add(wta("wta-007", "Jessica", "Pegula", "USA", 1994, 2, 24, Hand.RIGHT, Backhand.TWO_HANDED, 170, 65, 2015, 7, 4050));
        list.add(wta("wta-008", "Emma", "Navarro", "USA", 2001, 5, 18, Hand.RIGHT, Backhand.TWO_HANDED, 165, 57, 2021, 8, 3780));
        list.add(wta("wta-009", "Daria", "Kasatkina", "RUS", 1997, 5, 7, Hand.RIGHT, Backhand.TWO_HANDED, 170, 62, 2014, 9, 3420));
        list.add(wta("wta-010", "Danielle", "Collins", "USA", 1993, 12, 13, Hand.RIGHT, Backhand.TWO_HANDED, 175, 68, 2016, 10, 3180));
        list.add(wta("wta-011", "Barbora", "Krejcikova", "CZE", 1995, 12, 18, Hand.RIGHT, Backhand.TWO_HANDED, 180, 72, 2014, 11, 2990));
        list.add(wta("wta-012", "Beatriz", "Haddad Maia", "BRA", 1996, 5, 30, Hand.LEFT, Backhand.TWO_HANDED, 185, 78, 2015, 12, 2810));
        list.add(wta("wta-013", "Madison", "Keys", "USA", 1995, 2, 17, Hand.RIGHT, Backhand.TWO_HANDED, 178, 68, 2013, 13, 2650));
        list.add(wta("wta-014", "Mirra", "Andreeva", "RUS", 2007, 4, 27, Hand.RIGHT, Backhand.TWO_HANDED, 175, 60, 2023, 14, 2510));
        list.add(wta("wta-015", "Anna", "Kalinskaya", "RUS", 1998, 12, 2, Hand.RIGHT, Backhand.TWO_HANDED, 178, 70, 2016, 15, 2340));
        list.add(wta("wta-016", "Marta", "Kostyuk", "UKR", 2002, 6, 28, Hand.RIGHT, Backhand.TWO_HANDED, 174, 58, 2018, 16, 2190));
        list.add(wta("wta-017", "Paula", "Badosa", "ESP", 1997, 11, 15, Hand.RIGHT, Backhand.TWO_HANDED, 180, 70, 2018, 17, 2050));
        list.add(wta("wta-018", "Donna", "Vekic", "HRV", 1996, 6, 28, Hand.RIGHT, Backhand.TWO_HANDED, 180, 68, 2012, 18, 1910));
        list.add(wta("wta-019", "Liudmila", "Samsonova", "RUS", 1998, 11, 11, Hand.RIGHT, Backhand.TWO_HANDED, 180, 70, 2017, 19, 1780));
        list.add(wta("wta-020", "Diana", "Shnaider", "RUS", 2004, 10, 16, Hand.LEFT, Backhand.TWO_HANDED, 175, 63, 2022, 20, 1650));
        list.add(wta("wta-021", "Leylah", "Fernandez", "CAN", 2002, 9, 6, Hand.LEFT, Backhand.TWO_HANDED, 168, 59, 2019, 21, 1520));
        list.add(wta("wta-022", "Elina", "Svitolina", "UKR", 1994, 9, 12, Hand.RIGHT, Backhand.TWO_HANDED, 174, 60, 2013, 22, 1410));
        list.add(wta("wta-023", "Karolina", "Muchova", "CZE", 1996, 8, 21, Hand.RIGHT, Backhand.ONE_HANDED, 180, 68, 2018, 23, 1310));
        list.add(wta("wta-024", "Katie", "Boulter", "GBR", 1996, 8, 1, Hand.RIGHT, Backhand.TWO_HANDED, 183, 70, 2015, 24, 1200));
        list.add(wta("wta-025", "Linda", "Noskova", "CZE", 2004, 10, 15, Hand.RIGHT, Backhand.TWO_HANDED, 183, 68, 2021, 25, 1100));
        return list;
    }

    private PlayerData atp(String externalId, String firstName, String lastName,
                           String nationality, int birthYear, int birthMonth, int birthDay,
                           Hand hand, Backhand backhand, int heightCm, int weightKg,
                           int proYear, int ranking, int points) {
        return new PlayerData(externalId, firstName, lastName, nationality,
                LocalDate.of(birthYear, birthMonth, birthDay),
                hand, backhand, heightCm, weightKg, proYear, ranking, points, Gender.MALE);
    }

    private PlayerData wta(String externalId, String firstName, String lastName,
                           String nationality, int birthYear, int birthMonth, int birthDay,
                           Hand hand, Backhand backhand, int heightCm, int weightKg,
                           int proYear, int ranking, int points) {
        return new PlayerData(externalId, firstName, lastName, nationality,
                LocalDate.of(birthYear, birthMonth, birthDay),
                hand, backhand, heightCm, weightKg, proYear, ranking, points, Gender.FEMALE);
    }

    private List<TournamentData> buildTournaments() {
        List<TournamentData> list = new ArrayList<>();
        // Grand Slams
        list.add(tournament("gs-ao", "Australian Open", TournamentLevel.GRAND_SLAM, Surface.HARD, Gender.MALE, "Melbourne", "AUS", "Melbourne Park"));
        list.add(tournament("gs-rg", "Roland Garros", TournamentLevel.GRAND_SLAM, Surface.CLAY, Gender.MALE, "Paris", "FRA", "Stade Roland Garros"));
        list.add(tournament("gs-wim", "Wimbledon", TournamentLevel.GRAND_SLAM, Surface.GRASS, Gender.MALE, "London", "GBR", "All England Club"));
        list.add(tournament("gs-uso", "US Open", TournamentLevel.GRAND_SLAM, Surface.HARD, Gender.MALE, "New York", "USA", "USTA Billie Jean King National Tennis Center"));
        list.add(tournament("gs-ao-w", "Australian Open", TournamentLevel.GRAND_SLAM, Surface.HARD, Gender.FEMALE, "Melbourne", "AUS", "Melbourne Park"));
        list.add(tournament("gs-rg-w", "Roland Garros", TournamentLevel.GRAND_SLAM, Surface.CLAY, Gender.FEMALE, "Paris", "FRA", "Stade Roland Garros"));
        list.add(tournament("gs-wim-w", "Wimbledon", TournamentLevel.GRAND_SLAM, Surface.GRASS, Gender.FEMALE, "London", "GBR", "All England Club"));
        list.add(tournament("gs-uso-w", "US Open", TournamentLevel.GRAND_SLAM, Surface.HARD, Gender.FEMALE, "New York", "USA", "USTA Billie Jean King National Tennis Center"));
        // ATP Masters 1000
        list.add(tournament("m1000-iw", "Indian Wells Masters", TournamentLevel.ATP_1000, Surface.HARD, Gender.MALE, "Indian Wells", "USA", "Indian Wells Tennis Garden"));
        list.add(tournament("m1000-mia", "Miami Open", TournamentLevel.ATP_1000, Surface.HARD, Gender.MALE, "Miami", "USA", "Hard Rock Stadium"));
        list.add(tournament("m1000-mc", "Monte-Carlo Masters", TournamentLevel.ATP_1000, Surface.CLAY, Gender.MALE, "Monte Carlo", "MCO", "Monte-Carlo Country Club"));
        list.add(tournament("m1000-mad", "Madrid Open", TournamentLevel.ATP_1000, Surface.CLAY, Gender.MALE, "Madrid", "ESP", "Caja Magica"));
        list.add(tournament("m1000-rom", "Italian Open", TournamentLevel.ATP_1000, Surface.CLAY, Gender.MALE, "Rome", "ITA", "Foro Italico"));
        list.add(tournament("m1000-can", "Canadian Open", TournamentLevel.ATP_1000, Surface.HARD, Gender.MALE, "Montreal", "CAN", "IGA Stadium"));
        list.add(tournament("m1000-cin", "Cincinnati Masters", TournamentLevel.ATP_1000, Surface.HARD, Gender.MALE, "Cincinnati", "USA", "Lindner Family Tennis Center"));
        list.add(tournament("m1000-sha", "Shanghai Masters", TournamentLevel.ATP_1000, Surface.HARD, Gender.MALE, "Shanghai", "CHN", "Qizhong Forest Sports City Arena"));
        list.add(tournament("m1000-par", "Paris Masters", TournamentLevel.ATP_1000, Surface.HARD, Gender.MALE, "Paris", "FRA", "Accor Arena"));
        // WTA 1000
        list.add(tournament("w1000-iw", "Indian Wells Open", TournamentLevel.WTA_1000, Surface.HARD, Gender.FEMALE, "Indian Wells", "USA", "Indian Wells Tennis Garden"));
        list.add(tournament("w1000-mia", "Miami Open", TournamentLevel.WTA_1000, Surface.HARD, Gender.FEMALE, "Miami", "USA", "Hard Rock Stadium"));
        list.add(tournament("w1000-mad", "Madrid Open", TournamentLevel.WTA_1000, Surface.CLAY, Gender.FEMALE, "Madrid", "ESP", "Caja Magica"));
        list.add(tournament("w1000-rom", "Italian Open", TournamentLevel.WTA_1000, Surface.CLAY, Gender.FEMALE, "Rome", "ITA", "Foro Italico"));
        list.add(tournament("w1000-can", "Canadian Open", TournamentLevel.WTA_1000, Surface.HARD, Gender.FEMALE, "Montreal", "CAN", "IGA Stadium"));
        list.add(tournament("w1000-cin", "Cincinnati Open", TournamentLevel.WTA_1000, Surface.HARD, Gender.FEMALE, "Cincinnati", "USA", "Lindner Family Tennis Center"));
        list.add(tournament("w1000-bei", "China Open", TournamentLevel.WTA_1000, Surface.HARD, Gender.FEMALE, "Beijing", "CHN", "National Tennis Center"));
        list.add(tournament("w1000-wuh", "Wuhan Open", TournamentLevel.WTA_1000, Surface.HARD, Gender.FEMALE, "Wuhan", "CHN", "Optics Valley International Tennis Center"));
        // ATP 500
        list.add(tournament("a500-dub", "Dubai Tennis Championships", TournamentLevel.ATP_500, Surface.HARD, Gender.MALE, "Dubai", "ARE", "Dubai Duty Free Tennis Stadium"));
        list.add(tournament("a500-bar", "Barcelona Open", TournamentLevel.ATP_500, Surface.CLAY, Gender.MALE, "Barcelona", "ESP", "Real Club de Tenis Barcelona"));
        list.add(tournament("a500-que", "Queen's Club Championships", TournamentLevel.ATP_500, Surface.GRASS, Gender.MALE, "London", "GBR", "Queen's Club"));
        list.add(tournament("a500-hal", "Halle Open", TournamentLevel.ATP_500, Surface.GRASS, Gender.MALE, "Halle", "DEU", "OWL Arena"));
        list.add(tournament("a500-bas", "Swiss Indoors Basel", TournamentLevel.ATP_500, Surface.HARD, Gender.MALE, "Basel", "CHE", "St. Jakobshalle"));
        list.add(tournament("a500-vie", "Vienna Open", TournamentLevel.ATP_500, Surface.HARD, Gender.MALE, "Vienna", "AUT", "Wiener Stadthalle"));
        return list;
    }

    private TournamentData tournament(String externalId, String name, TournamentLevel level,
                                      Surface surface, Gender gender, String city,
                                      String countryCode, String venueName) {
        return new TournamentData(externalId, name, level, surface, gender, city, countryCode, venueName);
    }

    private List<RankingData> buildRankings() {
        LocalDate rankingDate = LocalDate.now().minusDays(1);
        List<RankingData> list = new ArrayList<>();
        for (PlayerData player : players) {
            list.add(new RankingData(
                    player.externalId(),
                    player.currentRanking(),
                    player.currentPoints(),
                    rankingDate,
                    RankingType.SINGLES,
                    player.gender()));
        }
        return list;
    }
}
