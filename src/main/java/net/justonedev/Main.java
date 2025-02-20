package net.justonedev;

import net.justonedev.statswrapper.RepositoryStats;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
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

    private static final String SEND_MESSAGE = "Enter Project Path (leave empty to generate stats):";
    public static ConcurrentLinkedQueue<String> readInput() {
        ConcurrentLinkedQueue<String> repositories = new ConcurrentLinkedQueue<>();
        Scanner scanner = new Scanner(System.in);
        System.out.println(SEND_MESSAGE);
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine().trim();
            if (line.isEmpty()) break;
            if (line.startsWith("~")) {
                String home = System.getProperty("user.home");
                line = line.replace("~", home);
            }
            if (new File(line, ".git").exists()) {
                repositories.add(line);
                System.out.println("Added Project: " + new File(line).getName());
                System.out.println(SEND_MESSAGE);
                continue;
            }
            System.out.println("Folder does not have a Git Repository. Please try again.");
        }
        return repositories;
    }
}