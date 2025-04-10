package fr.quentin.poppy.data;

import net.minecraft.registry.RegistryKey;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class HomeDataManager {
    private static final Map<UUID, Map<String, HomeData>> playerHomes = new HashMap<>();
    public static final int MAX_HOMES_PER_PLAYER = 24;
    public static final int MAX_CHARACTER_FOR_HOME_NAME = 48;

    private static String getFormattedDate() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMMM dd, yyyy", Locale.ENGLISH);
        return dateFormat.format(new Date());
    }

    public static void load() {
        playerHomes.clear();
        Map<UUID, Map<String, HomeData>> loadedHomes = HomeDataStorage.loadHomes();
        playerHomes.putAll(loadedHomes);
    }

    public static void save() {
        HomeDataStorage.saveHomes(playerHomes);
    }

    public static void addHome(UUID playerId, String homeName, RegistryKey<World> dimension, Vec3d position) {
        Map<String, HomeData> homes = playerHomes.getOrDefault(playerId, new HashMap<>());
        if (homes.size() >= MAX_HOMES_PER_PLAYER) {
            throw new IllegalStateException(Text.translatable("commands.poppy.sethome.limit_reached", MAX_HOMES_PER_PLAYER).getString());
        }

        String creationDate = getFormattedDate();
        playerHomes.computeIfAbsent(playerId, k -> new HashMap<>()).put(homeName, new HomeData(homeName, dimension, position, creationDate));
        save();
    }

    public static boolean removeHome(UUID playerId, String homeName) {
        Map<String, HomeData> homes = playerHomes.get(playerId);
        if (homes != null) {
            boolean removed = homes.remove(homeName) != null;
            if (removed) {
                save();
            }
            return removed;
        }
        return false;
    }

    public static HomeData getHome(UUID playerId, String homeName) {
        Map<String, HomeData> homes = playerHomes.get(playerId);
        return homes != null ? homes.get(homeName) : null;
    }

    public static Map<String, HomeData> getPlayerHomes(UUID playerId) {
        return playerHomes.getOrDefault(playerId, new HashMap<>());
    }

    public static boolean isValidHomeName(String homeName) {
        if (homeName.length() > MAX_CHARACTER_FOR_HOME_NAME) {
            return false;
        }
        return homeName.matches("[a-zA-Z0-9_-]+");
    }
}