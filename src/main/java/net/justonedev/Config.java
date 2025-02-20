package net.justonedev;

public final class Config {
    public static final int SRC_FOLDER_MAX_DEPTH = 1;

    private Config() {}

    public static String getAlias(String author) {
        return switch (author) {
            // Add your aliases here!
            case "Just1Developer" -> "JustOneDeveloper";
            default -> author;
        };
    }
}
