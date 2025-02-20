package net.justonedev.statswrapper;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static net.justonedev.statswrapper.UserStats.WEIGHT_ADDITIONS;
import static net.justonedev.statswrapper.UserStats.WEIGHT_DELETIONS;

public class FileStats {
    private final Map<String, Integer> perFileAdditions;
    private final Map<String, Integer> perFileDeletions;

    public FileStats() {
        perFileAdditions = new HashMap<>();
        perFileDeletions = new HashMap<>();
    }

    public void addChanges(String file, int additions, int deletions) {
        perFileAdditions.put(file, perFileAdditions.getOrDefault(file, 0) + additions);
        perFileDeletions.put(file, perFileDeletions.getOrDefault(file, 0) + deletions);
    }

    public void addAllChanges(FileStats stats) {
        for (var entry : stats.perFileAdditions.entrySet()) {
            addChanges(entry.getKey(), entry.getValue(), stats.perFileDeletions.get(entry.getKey()));
        }
    }

    public Changes getChanges(String file) {
        return new Changes(perFileAdditions.getOrDefault(file, 0), perFileDeletions.getOrDefault(file, 0));
    }

    public List<FileChanges> getAllChangesSorted() {
        return perFileAdditions.entrySet().stream()
                .map((entry) -> new FileChanges(entry.getKey(), entry.getValue(), perFileDeletions.getOrDefault(entry.getKey(), 0)))
                .sorted(((Comparator<FileChanges>) (a, b) -> (int) Math.round((b.getAdditions() * WEIGHT_ADDITIONS + b.getDeletions() * WEIGHT_DELETIONS)
                        - (a.getAdditions() * WEIGHT_ADDITIONS + a.getDeletions() * WEIGHT_DELETIONS))).thenComparing(FileChanges::getFileName))
                .collect(Collectors.toList());
    }

    @Override
    public String toString() {
        List<FileChanges> allChangesPlusTotal = new ArrayList<>(getAllChangesSorted());
        int totalAdditions = 0, totalDeletions = 0;
        for (FileChanges change : allChangesPlusTotal) {
            totalAdditions += change.getAdditions();
            totalDeletions += change.getDeletions();
        }
        allChangesPlusTotal.add(new FileChanges("Total", totalAdditions, totalDeletions));
        return allChangesPlusTotal.stream().map((changes) -> "+%7d -%7d | %s".formatted(changes.getAdditions(), changes.getDeletions(), changes.getFileName())).collect(Collectors.joining("\n"));
    }

    public static FileStats withProjectName(FileStats stats, String projectName) {
        var newStats = new FileStats();
        for (var entry : stats.perFileAdditions.entrySet()) {
            newStats.addChanges("%s/%s".formatted(projectName, entry.getKey()), entry.getValue(), stats.perFileDeletions.get(entry.getKey()));
        }
        return newStats;
    }
}
