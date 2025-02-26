package net.justonedev.statswrapper;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class UserStats {
    static final double WEIGHT_ADDITIONS = 2.5;
    static final double WEIGHT_DELETIONS = 1.1;

    private final Map<String, Integer> perUserAdditions;
    private final Map<String, Integer> perUserDeletions;
    private final Map<String, Integer> perUserCommits;

    public UserStats() {
        perUserAdditions = new HashMap<>();
        perUserDeletions = new HashMap<>();
        perUserCommits = new HashMap<>();
    }

    public void addChanges(String author, int additions, int deletions) {
        perUserAdditions.put(author, perUserAdditions.getOrDefault(author, 0) + additions);
        perUserDeletions.put(author, perUserDeletions.getOrDefault(author, 0) + deletions);
    }

    public void addCommit(String author) {
        perUserCommits.put(author, perUserCommits.getOrDefault(author, 0) + 1);
    }

    public void addCommits(String author, int commits) {
        perUserCommits.put(author, perUserCommits.getOrDefault(author, 0) + commits);
    }

    public void addAllChanges(UserStats stats) {
        for (var entry : stats.perUserAdditions.entrySet()) {
            addChanges(entry.getKey(), entry.getValue(), stats.perUserDeletions.get(entry.getKey()));
            addCommits(entry.getKey(), stats.perUserCommits.getOrDefault(entry.getKey(), 0));
        }
    }

    public void addAllAdditions(Map<String, Integer> stats) {
        for (var entry : stats.entrySet()) {
            addChanges(entry.getKey(), entry.getValue(), 0);
        }
    }

    public void addAllBlames(Map<String, MutableIntegerPair> stats) {
        for (var entry : stats.entrySet()) {
            addChanges(entry.getKey(), entry.getValue().getFirst(), entry.getValue().getSecond());
        }
    }

    public Changes getChanges(String file) {
        return new Changes(perUserAdditions.getOrDefault(file, 0), perUserDeletions.getOrDefault(file, 0));
    }

    public List<UserChanges> getAllChangesSorted() {
        return perUserAdditions.entrySet().stream()
                .map((entry) -> new UserChanges(entry.getKey(), entry.getValue(), perUserDeletions.getOrDefault(entry.getKey(), 0), perUserCommits.getOrDefault(entry.getKey(), 0)))
                .sorted(((Comparator<UserChanges>) (a, b) -> (int) Math.round((b.getAdditions() * WEIGHT_ADDITIONS + b.getDeletions() * WEIGHT_DELETIONS)
                        - (a.getAdditions() * WEIGHT_ADDITIONS + a.getDeletions() * WEIGHT_DELETIONS))).thenComparing(UserChanges::getAuthor))
                .collect(Collectors.toList());
    }

    public List<UserChanges> getAllChangesSortedBy(Comparator<UserChanges> comparator) {
        return perUserAdditions.entrySet().stream()
                .map((entry) -> new UserChanges(entry.getKey(), entry.getValue(), perUserDeletions.getOrDefault(entry.getKey(), 0), perUserCommits.getOrDefault(entry.getKey(), 0)))
                .sorted(comparator.thenComparing(UserChanges::getAuthor))
                .collect(Collectors.toList());
    }

    public List<UserChanges> getAllChangesSortedByCommit() {
        return perUserAdditions.entrySet().stream()
                .map((entry) -> new UserChanges(entry.getKey(), entry.getValue(), perUserDeletions.getOrDefault(entry.getKey(), 0), perUserCommits.getOrDefault(entry.getKey(), 0)))
                .sorted(Comparator.comparing(UserChanges::getCommits).reversed())
                .collect(Collectors.toList());
    }

    public static List<UserChanges> allChangesPlusTotal(List<UserChanges> sorted) {
        List<UserChanges> changes = new ArrayList<>(sorted);
        int totalAdditions = 0, totalDeletions = 0, totalCommits = 0;
        for (var change : changes) {
            totalAdditions += change.getAdditions();
            totalDeletions += change.getDeletions();
            totalCommits += change.getCommits();
        }
        changes.add(new UserChanges("Total", totalAdditions, totalDeletions, totalCommits));
        return changes;
    }

    @Override
    public String toString() {
        var changes = allChangesPlusTotal(getAllChangesSorted());
        int maxLength = 0;
        for (var change : changes) {
            if (change.getAuthor().length() > maxLength) maxLength = change.getAuthor().length();
        }
        String formatString = "%ss >> %s %s".formatted("%-" + (maxLength + 1), "%+7d", "%7d");
        return changes.stream().map((change) -> formatString.formatted(change.getAuthor(), change.getAdditions(), change.getDeletions() * -1)).collect(Collectors.joining("\n"));
    }

    public String toStringAdditionsOnly(boolean showPercentage) {
        var changes = allChangesPlusTotal(getAllChangesSorted());
        var totalChanges = changes.getLast().getAdditions();
        int maxLength = 0;
        for (var change : changes) {
            if (change.getAuthor().length() > maxLength) maxLength = change.getAuthor().length();
        }
        String formatString = "%ss >> %s".formatted("%-" + (maxLength + 1), "%7d" + (showPercentage ? "%10s" : "%s"));
        return changes.stream().map((change) -> formatString.formatted(change.getAuthor(), change.getAdditions(), showPercentage ? "(%.2f%s)".formatted(Math.round(((double) change.getAdditions()) / totalChanges * 10000d) / 100d, "%") : "")).collect(Collectors.joining("\n"));
    }

    public String toStringCommits(boolean showAveragePerCommit) {
        var changes = allChangesPlusTotal(getAllChangesSortedByCommit());
        int maxLength = 0;
        for (var change : changes) {
            if (change.getAuthor().length() > maxLength) maxLength = change.getAuthor().length();
        }
        String formatString = "%ss >> %s".formatted("%-" + (maxLength + 1), "%-" + ((int) Math.ceil(Math.log10(changes.getLast().getCommits())) + 3) + "s%s");
        return changes.stream().map((change) -> formatString.formatted(change.getAuthor(), change.getCommits(), showAveragePerCommit ? "|   Average: %+7.2f %7.2f".formatted((double) change.getAdditions() / change.getCommits(), (double) change.getDeletions() / -change.getCommits()) : "")).collect(Collectors.joining("\n"));
    }
}
