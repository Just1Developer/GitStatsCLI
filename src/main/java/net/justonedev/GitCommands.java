package net.justonedev;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public final class GitCommands {

    private GitCommands() {}

    public static List<String> runCommand(String directory, String command) {
        List<String> output = new ArrayList<>();
        try {
            ProcessBuilder builder = new ProcessBuilder();

            // Set platform-specific shell
            if (System.getProperty("os.name").toLowerCase().contains("win")) {
                builder.command("cmd.exe", "/c", command);
            } else {
                builder.command("sh", "-c", command);
            }

            builder.directory(new File(directory));
            builder.redirectErrorStream(true); // Merge stdout and stderr

            Process process = builder.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.add(line);
                }
            }

            process.waitFor();
        } catch (IOException | InterruptedException e) {
            output.add("ERROR: " + e.getMessage());
        }
        // Add empty line to trigger flush in processing
        output.add("");
        return output;
    }

}
