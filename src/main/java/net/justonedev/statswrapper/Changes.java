package net.justonedev.statswrapper;

import lombok.Getter;

@Getter
public class Changes {
    private final int additions;
    private final int deletions;

    public Changes(int additions, int deletions) {
        this.additions = additions;
        this.deletions = deletions;
    }
}
