import java.util.Locale;
import java.util.Objects;

public final class ObsolescenceScoring {

    // --- API publique --------------------------------------------------------

    public static ScoreResult compute(DependencyRisk input) {
        Objects.requireNonNull(input, "input must not be null");

        // 1) Sous-score: écart de version (0..50)
        int versionScore = computeVersionGapScore(input.versionInUse(), input.latestVersion());

        // 2) Sous-score: risque opérationnel (0..30)
        int riskScore = computeRiskScore(input.risk());

        // 3) Sous-score: cadence (0..10)
        int cadenceScore = computeCadenceScore(input.cadence());

        // 4) Sous-score: EOL (0..10)
        int eolScore = computeEolScore(input.isEol());

        // Pondérations (comme proposé) : 50% version, 30% risque, 10% cadence, 10% EOL
        // On garde les sous-scores déjà sur leurs échelles maximales, puis on somme directement.
        int rawTotal = clamp(versionScore + riskScore + cadenceScore + eolScore, 1, 100);

        Reading reading = Reading.fromScore(rawTotal);

        return new ScoreResult(rawTotal, reading, versionScore, riskScore, cadenceScore, eolScore);
    }

    // --- Implémentation des sous-scores -------------------------------------

    /**
     * Calcule un score 0..50 basé sur l'écart de version MAJEURE et MINEURE.
     * Objectif: refléter que "1.x vs 4.x" est très obsolète, même si le champ released est récent.
     *
     * Stratégie:
     * - écart majeur: +15 points par major de retard (cap à 45)
     * - écart mineur: +1 point par minor de retard (cap à 5)
     * Total capé à 50.
     */
    static int computeVersionGapScore(String versionInUse, String latestVersion) {
        Version vInUse = Version.parse(versionInUse);
        Version vLatest = Version.parse(latestVersion);

        // Si on ne peut pas parser, on met un score neutre (ex: 25/50)
        if (vInUse == null || vLatest == null) return 25;

        int majorGap = Math.max(0, vLatest.major - vInUse.major);
        int minorGap = Math.max(0, vLatest.minor - vInUse.minor);

        int majorPart = Math.min(45, majorGap * 15);
        int minorPart = Math.min(5, minorGap);

        return clamp(majorPart + minorPart, 0, 50);
    }

    /**
     * Score 0..30 :
     * - HIGH => 30
     * - MEDIUM => 20
     * - LOW => 10
     * - inconnu/null => 15
     */
    static int computeRiskScore(String risk) {
        if (risk == null) return 15;
        String r = risk.trim().toUpperCase(Locale.ROOT);
        return switch (r) {
            case "HIGH" -> 30;
            case "MEDIUM" -> 20;
            case "LOW" -> 10;
            default -> 15;
        };
    }

    /**
     * Score 0..10 basé sur la cadence de release.
     * Interprétation simple: plus cadence est faible (peu de releases), plus c'est risqué.
     *
     * Ici on suppose que "cadence" = nombre de releases / an (ou une approximation).
     * - cadence >= 6 => 0 (très actif)
     * - cadence 3..5 => 2
     * - cadence 1..2 => 6
     * - cadence 0 => 10 (quasi mort)
     * - null => 5 (inconnu)
     */
    static int computeCadenceScore(Integer cadence) {
        if (cadence == null) return 5;
        if (cadence >= 6) return 0;
        if (cadence >= 3) return 2;
        if (cadence >= 1) return 6;
        return 10;
    }

    /**
     * Score 0..10 :
     * - isEol == TRUE => 10
     * - isEol == FALSE => 0
     * - isEol == null => 5 (inconnu)
     */
    static int computeEolScore(Boolean isEol) {
        if (isEol == null) return 5;
        return isEol ? 10 : 0;
    }

    // --- Helpers ------------------------------------------------------------

    static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    /**
     * Parser de version simple "major.minor.patch" (tolère des suffixes -SNAPSHOT etc.).
     * Retourne null si version est vide/null/non parseable.
     */
    static final class Version {
        final int major;
        final int minor;
        final int patch;

        Version(int major, int minor, int patch) {
            this.major = major;
            this.minor = minor;
            this.patch = patch;
        }

        static Version parse(String v) {
            if (v == null) return null;
            String s = v.trim();
            if (s.isEmpty()) return null;

            // Retire suffixes type "-SNAPSHOT", "+build", etc.
            s = s.split("[-+]", 2)[0];

            String[] parts = s.split("\\.");
            if (parts.length < 1) return null;

            try {
                int major = Integer.parseInt(parts[0]);
                int minor = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
                int patch = parts.length > 2 ? Integer.parseInt(parts[2]) : 0;
                return new Version(major, minor, patch);
            } catch (NumberFormatException e) {
                return null;
            }
        }
    }

    // --- Modèles de données (records Java 16+) -------------------------------

    public record DependencyRisk(
            String component,
            String versionInUse,
            String latestVersion,
            String risk,
            Integer cadence,
            Boolean isEol
    ) {}

    public enum Reading {
        OK, A_SURVEILLER, CRITIQUE, URGENT;

        public static Reading fromScore(int score) {
            if (score <= 30) return OK;
            if (score <= 60) return A_SURVEILLER;
            if (score <= 80) return CRITIQUE;
            return URGENT;
        }
    }

    public record ScoreResult(
            int score,            // 1..100
            Reading reading,      // lecture humaine
            int versionScore,     // 0..50
            int riskScore,        // 0..30
            int cadenceScore,     // 0..10
            int eolScore          // 0..10
    ) {}

    // --- Exemple d'utilisation ----------------------------------------------

    public static void main(String[] args) {
        DependencyRisk plexus = new DependencyRisk(
                "org.codehaus.plexus:plexus-utils",
                "1.5.6",
                "4.0.2",
                "High",
                1,
                null
        );

        ScoreResult result = compute(plexus);

        System.out.println("Component : " + plexus.component());
        System.out.println("Score     : " + result.score() + "/100");
        System.out.println("Reading   : " + result.reading());
        System.out.println("Details   : version=" + result.versionScore()
                + ", risk=" + result.riskScore()
                + ", cadence=" + result.cadenceScore()
                + ", eol=" + result.eolScore());
    }
}