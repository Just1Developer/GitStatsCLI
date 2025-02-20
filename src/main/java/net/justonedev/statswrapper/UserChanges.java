package net.justonedev.statswrapper;

import lombok.Getter;

@Getter
public class UserChanges extends Changes {
    private final String author;
    private final int commits;

    public UserChanges(final String author, final int additions, final int deletions, final int commits) {
        super(additions, deletions);
        this.author = author;
        this.commits = commits;
    }
}
