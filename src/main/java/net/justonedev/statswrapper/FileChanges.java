package net.justonedev.statswrapper;

import lombok.Getter;

@Getter
public class FileChanges extends Changes {
    private final String fileName;
    private final long lineCount;
    private final long lineCountLOC;

    public FileChanges(final String fileName, final int additions, final int deletions, final long lineCount, final long lineCountLOC) {
        super(additions, deletions);
        this.fileName = fileName;
        this.lineCount = lineCount;
        this.lineCountLOC = lineCountLOC;
    }
}
