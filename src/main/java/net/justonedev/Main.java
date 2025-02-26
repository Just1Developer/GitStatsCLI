package net.justonedev;

import net.justonedev.statswrapper.RepositoryStats;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Main {

    public static final String TITLE_ALL_PROJECTS = "All Projects";
    public static final String FILENAME = "repository-stats";

    public static void main(String[] args) {
        System.out.println("Hello, World!");

        List<RepositoryStats> repositories = readInput().parallelStream().map((path) -> new StatsGetter().getAllGitStats(path)).toList();
        if (repositories.isEmpty()) {
            System.out.println("No repositories given.");
            return;
        }

        if (repositories.size() > 1) {
            System.out.println("More than one repository found. Adding an \"All Projects\" sheet...\"");
            RepositoryStats total = RepositoryStats.accumulate(TITLE_ALL_PROJECTS, repositories);
            List<RepositoryStats> allRepos = new ArrayList<>(repositories);
            allRepos.add(total);
            repositories = allRepos;
        }
        System.out.println("Generating Excel File...");
        int index = 1;
        String filename = FILENAME;
        while (new File(filename + ".xlsx").exists()) {
            filename = "%s-%d".formatted(FILENAME, index++);
        }
        ExcelExport.writeExcel(filename, repositories);
        System.out.printf("Done! Exported Git Stats to %s.xlsx%n", filename);
    }

    private static final String SEND_MESSAGE = "Enter one or more project paths (leave empty to generate stats):";
    public static ConcurrentLinkedQueue<String> readInput() {
        ConcurrentLinkedQueue<String> repositories = new ConcurrentLinkedQueue<>();
        Set<String> usedDirs = new HashSet<>();
        Scanner scanner = new Scanner(System.in);
        System.out.println(SEND_MESSAGE);
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine().trim();
            if (line.isEmpty()) break;

            for (String dir : parseDirectories(line)) {
                dir = dir.trim();
                if (dir.isEmpty()) continue;
                if (dir.startsWith("~")) {
                    String home = System.getProperty("user.home");
                    dir = dir.replace("~", home);
                }
                if (!usedDirs.add(dir)) {
                    System.out.printf("Project \"%s\" is already registered, skipping...%n", dir);
                    continue;
                }
                File f = new File(dir);
                if (new File(f, ".git").exists()) {
                    repositories.add(dir);
                    System.out.println("Added Project: " + f.getName());
                } else System.out.printf("Folder \"%s\" does not have a Git Repository. Please try again.%n", dir);
            }
            System.out.println(SEND_MESSAGE);
        }
        return repositories;
    }

    private static List<String> parseDirectories(String path) {
        if (path.isEmpty()) return new ArrayList<>();
        List<String> directories = new ArrayList<>();
        char currentDelimiter = 0;
        boolean inDirectory = false;
        StringBuilder builder = new StringBuilder();
        for (char c : path.toCharArray()) {
            if (!inDirectory) {
                if (("" + c).matches("\\s")) continue;
                if (c == '"') {
                    currentDelimiter = '"';
                } else {
                    currentDelimiter = ' ';
                    builder.append(c);
                }
                inDirectory = true;
                continue;
            }
            if (c == currentDelimiter) {
                inDirectory = false;
                directories.add(builder.toString());
                builder = new StringBuilder();
                continue;
            }
            builder.append(c);
        }
        if (!builder.isEmpty()) {
            if (currentDelimiter == '"') {
                System.out.printf("Warning: Ignoring unclosed directory String \"%s\"%n", builder);
            } else {
                directories.add(builder.toString());
            }
        }
        return directories;
    }
}