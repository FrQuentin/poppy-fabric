package fr.quentin.poppy.data;

import com.google.gson.*;
import fr.quentin.poppy.Poppy;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class HomeDataStorage {
    private static final Path DATA_DIR = Path.of("poppy");
    private static final Path HOME_DATA_FILE = DATA_DIR.resolve("homes.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static Map<UUID, Map<String, HomeData>> loadHomes() {
        try {
            Files.createDirectories(DATA_DIR);
        } catch (IOException e) {
            Poppy.LOGGER.error("Error while creating data folder", e);
            return new HashMap<>();
        }

        if (!Files.exists(HOME_DATA_FILE)) {
            return new HashMap<>();
        }

        try (BufferedReader reader = Files.newBufferedReader(HOME_DATA_FILE)) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            Map<UUID, Map<String, HomeData>> homes = new HashMap<>();

            for (Map.Entry<String, JsonElement> entry : json.getAsJsonObject("homes").entrySet()) {
                UUID playerId = UUID.fromString(entry.getKey());
                Map<String, HomeData> playerHomes = new HashMap<>();

                for (JsonElement homeElement : entry.getValue().getAsJsonArray()) {
                    JsonObject homeJson = homeElement.getAsJsonObject();
                    String name = homeJson.get("name").getAsString();
                    String creationDate = homeJson.get("creation_date").getAsString();
                    String worldName = homeJson.get("world").getAsString();
                    JsonObject positionJson = homeJson.getAsJsonObject("position");

                    RegistryKey<World> dimension = RegistryKey.of(
                            RegistryKeys.WORLD,
                            Identifier.of(worldName)
                    );

                    Vec3d position = new Vec3d(
                            positionJson.get("x").getAsDouble(),
                            positionJson.get("y").getAsDouble(),
                            positionJson.get("z").getAsDouble()
                    );

                    playerHomes.put(name, new HomeData(name, dimension, position, creationDate));
                }

                homes.put(playerId, playerHomes);
            }

            return homes;
        } catch (IOException | JsonParseException e) {
            Poppy.LOGGER.error("Error while loading homes data", e);
            return new HashMap<>();
        }
    }

    public static void saveHomes(Map<UUID, Map<String, HomeData>> homes) {
        try {
            Files.createDirectories(DATA_DIR);
        } catch (IOException e) {
            Poppy.LOGGER.error("Error while creating data folder", e);
            return;
        }

        JsonObject json = new JsonObject();
        JsonObject homesJson = new JsonObject();

        for (Map.Entry<UUID, Map<String, HomeData>> entry : homes.entrySet()) {
            JsonArray playerHomesJson = new JsonArray();

            for (HomeData home : entry.getValue().values()) {
                JsonObject homeJson = createHomeJson(home);
                playerHomesJson.add(homeJson);
            }
            homesJson.add(entry.getKey().toString(), playerHomesJson);
        }

        json.add("homes", homesJson);

        try (BufferedWriter writer = Files.newBufferedWriter(HOME_DATA_FILE)) {
            GSON.toJson(json, writer);
        } catch (IOException e) {
            Poppy.LOGGER.error("Error while saving homes data", e);
        }
    }

    private static JsonObject createHomeJson(HomeData home) {
        JsonObject homeJson = new JsonObject();
        homeJson.addProperty("name", home.name());
        homeJson.addProperty("creation_date", home.creationDate());
        homeJson.addProperty("world", home.dimension().getValue().toString());

        JsonObject positionJson = new JsonObject();
        positionJson.addProperty("x", home.position().getX());
        positionJson.addProperty("y", home.position().getY());
        positionJson.addProperty("z", home.position().getZ());
        homeJson.add("position", positionJson);

        return homeJson;
    }
}