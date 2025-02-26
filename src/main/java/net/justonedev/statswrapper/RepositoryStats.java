package net.justonedev.statswrapper;

import java.util.Collection;

public record RepositoryStats(String name, UserStats fullBranchStats, UserStats mainBranchStats,
                              UserStats contributionStats, UserStats contributionsCommentsStats, FileStats fileStats) {

    public static RepositoryStats accumulate(String name, Collection<RepositoryStats> repositories) {
        UserStats fullBranchStats = new UserStats();
        UserStats mainBranchStats = new UserStats();
        UserStats contributionStats = new UserStats();
        UserStats contributionsCommentsStats = new UserStats();
        FileStats fileStats = new FileStats();
        for (RepositoryStats repo : repositories) {
            fullBranchStats.addAllChanges(repo.fullBranchStats);
            mainBranchStats.addAllChanges(repo.mainBranchStats);
            contributionStats.addAllChanges(repo.contributionStats);
            contributionsCommentsStats.addAllChanges(repo.contributionsCommentsStats);
            fileStats.addAllChanges(FileStats.withProjectName(repo.fileStats, repo.name()));
        }
        return new RepositoryStats(name, fullBranchStats, mainBranchStats, contributionStats, contributionsCommentsStats, fileStats);
    }
}
