package net.justonedev;

import net.justonedev.statswrapper.FileStats;
import net.justonedev.statswrapper.MutableIntegerPair;
import net.justonedev.statswrapper.RepositoryStats;
import net.justonedev.statswrapper.UserStats;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.regex.Pattern;

public class StatsGetter {

    private static final Pattern REGEX_COMMIT_TITLE = Pattern.compile("^commit ([\\da-z]+) ([a-zA-Z\\d\\s]+)$");
    private static final Pattern REGEX_CHANGES = Pattern.compile("^(-?\\d+)\\s+(-?\\d+)\\s+([\\da-zA-Z/\\-._()\\[\\]{}=>\\s]+)$");
    private static final String REGEX_EXCLUDED_FILES = "(package\\.json|p?npm-lock\\.yaml|(\\.(py|xlsx|dot|svg)))$";

    private double secondsSince(long nanoTime) {
        return Math.round((System.nanoTime() - nanoTime) / 1000000d) / 1000d;
    }

    public RepositoryStats getAllGitStats(String repoPath) {
        String projectName = new File(repoPath).getName();
        String prefix = "[%s] ".formatted(projectName);
        String TIME_FORMAT = prefix + "%s   (%.3f s)%n";

        System.out.println(prefix + "Updating repository for all branches (fetch)...");
        long time = System.nanoTime();
        GitCommands.runCommand(repoPath, "git fetch --all");
        System.out.printf(TIME_FORMAT, "Updated repository for all branches", secondsSince(time));

        UserStats userStatsAllBranches = new UserStats();
        UserStats userStatsMainOnly = new UserStats();
        FileStats fileStats = new FileStats();
        UserStats finalCodeContributions = new UserStats();
        UserStats contributionsComments = new UserStats();

        System.out.println(prefix + "Fetching for all branches...");
        time = System.nanoTime();
        fillGitStatistics(repoPath, "git log --all --no-merges --numstat --pretty=format:\"commit %H %an\"", userStatsAllBranches, fileStats);
        System.out.printf(TIME_FORMAT, "Finished fetching for all branches", secondsSince(time));

        System.out.println(prefix + "Fetching for main branch...");
        time = System.nanoTime();
        fillGitStatistics(repoPath, "git log main --numstat --pretty=format:\"commit %H %an\"", userStatsMainOnly);
        System.out.printf(TIME_FORMAT, "Finished fetching for main branch", secondsSince(time));

        System.out.println(prefix + "Blaming current codebase...");
        time = System.nanoTime();
        fillGitBlameStatistics(repoPath, finalCodeContributions, contributionsComments, fileStats);
        System.out.printf(TIME_FORMAT, "Finished blaming", secondsSince(time));

        return new RepositoryStats(projectName, userStatsAllBranches, userStatsMainOnly, finalCodeContributions, contributionsComments, fileStats);
    }

    public void fillGitStatistics(String repoPath, String command, UserStats userStats) {
        fillGitStatistics(repoPath, command, userStats, null);
    }

    public void fillGitStatistics(String repoPath, String command, UserStats userStats, FileStats fileStats) {
        List<String> allStats = GitCommands.runCommand(repoPath, command);
        String currentAuthor = null;
        int currentAdditions = 0, currentDeletions = 0;
        Set<String> commits = new HashSet<>();

        for (String stat : allStats) {
            var matcher = REGEX_COMMIT_TITLE.matcher(stat);
            if (matcher.matches()) {
                String commit = matcher.group(1);
                if (!commits.add(commit)) {
                    // Skip commit as we already know it
                    currentAuthor = null;
                    continue;
                }
                if (currentAuthor != null) userStats.addChanges(currentAuthor, currentAdditions, currentDeletions);
                currentAuthor = Config.getAlias(matcher.group(2));
                currentAdditions = 0;
                currentDeletions = 0;
                userStats.addCommit(currentAuthor);
                continue;
            } else if (currentAuthor == null) continue;

            matcher = REGEX_CHANGES.matcher(stat);
            if (!matcher.matches()) continue;
            String file = matcher.group(3);
            if (file.matches(REGEX_EXCLUDED_FILES)) continue;
            int add = Integer.parseInt(matcher.group(1));
            int del = Integer.parseInt(matcher.group(2));
            currentAdditions += add;
            currentDeletions += del;
            if (fileStats != null) fileStats.addChanges(file, add, del);
        }

    }

    //region GIT BLAME

    private static final Pattern REGEX_BLAME_LINE = Pattern.compile("^[a-z\\d]+\\s(?:.*\"?\\s)?\\((.+)\\s\\d{4}-\\d{2}-\\d{2}\\s\\d{2}:\\d{2}:\\d{2}\\s[^)]+\\)\\s?(.*)$");
    private static final String REGEX_BLAME_COMMENT = "^((//|/?\\*.*)|$)";

    // Additions: All File Changes, Deletions: File Changes (no comments)
    public void fillGitBlameStatistics(final String repoPath, UserStats userStats, UserStats userStatsComments, FileStats fileStats) {
        var files = getAllBlamableFiles(repoPath);
        files.parallelStream().map((file) -> getBlame(repoPath, file)).forEach(e -> {
            userStats.addAllBlames(e.blame);
            userStatsComments.addAllBlames(e.commentBlame);
            fileStats.addAllBlames(e.file, e.blame);
        });
    }

    private ConcurrentLinkedQueue<String> getAllBlamableFiles(String repoPath) {
        ConcurrentLinkedQueue<String> files = new ConcurrentLinkedQueue<>();
        getAllBlamableFiles(new File(repoPath), files, false);
        return files;
    }

    private void getAllBlamableFiles(File currentFile, ConcurrentLinkedQueue<String> files, boolean insideSrcFolder) {
        if (currentFile.isDirectory()) {
            var allFiles = currentFile.listFiles();
            if (allFiles != null) {
                for (File file : allFiles) {
                    boolean insideSrc = insideSrcFolder || file.getName().equals("src");
                    if (insideSrcFolder || !currentFile.isDirectory() || file.getName().equals("src") || hasSrcChild(file, Config.SRC_FOLDER_MAX_DEPTH)) getAllBlamableFiles(file, files, insideSrc);
                }
            }
            return;
        }
        if (currentFile.getName().matches(REGEX_EXCLUDED_FILES)) return;
        files.add(currentFile.getAbsolutePath());
    }

    private static boolean hasSrcChild(final File file, int depth) {
        if (depth == 0) return false;
        File[] files = file.listFiles();
        if (files == null) return false;
        for (File child : files) {
            if (child.getName().equals("src")) return true;
            else if (depth > 1 && child.isDirectory()) hasSrcChild(child, depth - 1);
        }
        return false;
    }

    // Additions mark
    private StringMapWrapper getBlame(String repoPath, String filePath) {
        //System.out.println("Blaming " + filePath);
        List<String> allBlocks = GitCommands.runCommand(repoPath, "git blame --no-line-porcelain \"%s\"".formatted(filePath));
        Map<String, MutableIntegerPair> lineBlame = new HashMap<>();
        Map<String, MutableIntegerPair> commentBlame = new HashMap<>();

        for (String line : allBlocks) {
            var matcher = REGEX_BLAME_LINE.matcher(line);
            if (!matcher.matches()) {
                continue;
            }
            String author = Config.getAlias(matcher.group(1).trim());
            String code = matcher.group(2).trim();
            var current = lineBlame.get(author);
            if (current == null) current = new MutableIntegerPair();
            var currentComment = commentBlame.get(author);
            if (currentComment == null) currentComment = new MutableIntegerPair();
            current.incrementFirst();
            if (!code.matches(REGEX_BLAME_COMMENT)) current.incrementSecond();
            else currentComment.incrementFirst();
            if (code.trim().isEmpty()) currentComment.incrementSecond();
            lineBlame.put(author, current);
            commentBlame.put(author, currentComment);
        }

        return new StringMapWrapper(filePath.replaceAll("^(%s/?)".formatted(repoPath), ""), lineBlame, commentBlame);
    }

    private record StringMapWrapper(String file, Map<String, MutableIntegerPair> blame, Map<String, MutableIntegerPair> commentBlame) { }

    //endregion

}
