package fr.quentin.poppy.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import fr.quentin.poppy.Poppy;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

public class PoppyConfig {
    private static final Path CONFIG_DIR = Path.of("config", Poppy.MOD_ID);
    private static final Path CONFIG_FILE = CONFIG_DIR.resolve("config.json");

    private static final int DEFAULT_HOME_BACKUP_INTERVAL_MIN = 5;

    private static ConfigData config;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private record ConfigData(
            int backupIntervalMinutes
    ) {}

    public static void load() {
        try {
            Files.createDirectories(CONFIG_DIR);

            if (Files.exists(CONFIG_FILE)) {
                try (BufferedReader reader = Files.newBufferedReader(CONFIG_FILE)) {
                    config = GSON.fromJson(reader, ConfigData.class);
                }
            } else {
                config = new ConfigData(DEFAULT_HOME_BACKUP_INTERVAL_MIN);
                save();
            }
        } catch (IOException e) {
            Poppy.LOGGER.error("Error loading configuration", e);
            config = new ConfigData(DEFAULT_HOME_BACKUP_INTERVAL_MIN);
        }
    }

    public static void save() {
        try (BufferedWriter writer = Files.newBufferedWriter(CONFIG_FILE)) {
            GSON.toJson(config, writer);
        } catch (IOException e) {
            Poppy.LOGGER.error("Error saving configuration", e);
        }
    }

    public static int getBackupIntervalMinutes() {
        return config.backupIntervalMinutes();
    }
}