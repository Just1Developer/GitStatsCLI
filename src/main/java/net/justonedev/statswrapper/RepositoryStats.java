package net.justonedev.statswrapper;

import java.util.Collection;

public record RepositoryStats(String name, UserStats fullBranchStats, UserStats mainBranchStats,
                              UserStats contributionStats, FileStats fileStats) {

    public static RepositoryStats accumulate(String name, Collection<RepositoryStats> repositories) {
        UserStats fullBranchStats = new UserStats();
        UserStats mainBranchStats = new UserStats();
        UserStats contributionStats = new UserStats();
        FileStats fileStats = new FileStats();
        for (RepositoryStats repo : repositories) {
            fullBranchStats.addAllChanges(repo.fullBranchStats);
            mainBranchStats.addAllChanges(repo.mainBranchStats);
            contributionStats.addAllChanges(repo.contributionStats);
            fileStats.addAllChanges(FileStats.withProjectName(repo.fileStats, repo.name()));
        }
        return new RepositoryStats(name, fullBranchStats, mainBranchStats, contributionStats, fileStats);
    }
}
