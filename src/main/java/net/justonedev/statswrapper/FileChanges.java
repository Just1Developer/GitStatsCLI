package net.justonedev.statswrapper;

import lombok.Getter;

@Getter
public class FileChanges extends Changes {
    private final String fileName;

    public FileChanges(final String fileName, final int additions, final int deletions) {
        super(additions, deletions);
        this.fileName = fileName;
    }
}
