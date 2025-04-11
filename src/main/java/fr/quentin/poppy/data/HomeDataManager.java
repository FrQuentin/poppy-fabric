package fr.quentin.poppy.data;

import fr.quentin.poppy.Poppy;
import net.minecraft.registry.RegistryKey;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import java.text.SimpleDateFormat;
import java.util.*;

public class HomeDataManager {
    private static final Map<UUID, Map<String, HomeData>> playerHomes = new HashMap<>();
    public static final int MAX_HOMES_PER_PLAYER = 24;
    public static final int MAX_CHARACTER_FOR_HOME_NAME = 48;

    private static String getFormattedDate() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMMM dd, yyyy", Locale.ENGLISH);
        return dateFormat.format(new Date());
    }

    public static void load() {
        try {
            synchronized (playerHomes) {
                playerHomes.clear();
                Map<UUID, Map<String, HomeData>> loadedHomes = HomeDataStorage.loadHomes();
                playerHomes.putAll(loadedHomes);
            }
        } catch (Exception e) {
            Poppy.LOGGER.error("An error occurred while loading homes.", e);
        }
    }

    public static void save() {
        try {
            synchronized (playerHomes) {
                HomeDataStorage.saveHomes(playerHomes);
            }
        } catch (Exception e) {
            Poppy.LOGGER.error("An error occurred while saving homes.", e);
        }
    }

    public static void addHome(UUID playerId, String homeName, RegistryKey<World> dimension, Vec3d position) {
        try {
            synchronized (playerHomes) {
                Map<String, HomeData> homes = playerHomes.getOrDefault(playerId, new HashMap<>());
                if (homes.size() >= MAX_HOMES_PER_PLAYER) {
                    throw new IllegalStateException(Text.translatable("commands.poppy.sethome.limit_reached", MAX_HOMES_PER_PLAYER).getString());
                }
                String creationDate = getFormattedDate();
                playerHomes.computeIfAbsent(playerId, k -> new HashMap<>()).put(homeName, new HomeData(homeName, dimension, position, creationDate));
                save();
            }
        } catch (Exception e) {
            Poppy.LOGGER.error("An error occurred while adding a home for player {}", playerId, e);
        }
    }

    public static boolean removeHome(UUID playerId, String homeName) {
        try {
            synchronized (playerHomes) {
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
        } catch (Exception e) {
            Poppy.LOGGER.error("An error occurred while removing a home for player {}", playerId, e);
            return false;
        }
    }

    public static HomeData getHome(UUID playerId, String homeName) {
        try {
            synchronized (playerHomes) {
                Map<String, HomeData> homes = playerHomes.get(playerId);
                return homes != null ? homes.get(homeName) : null;
            }
        } catch (Exception e) {
            Poppy.LOGGER.error("An error occurred while retrieving a home for player {}", playerId, e);
            return null;
        }
    }

    public static Map<String, HomeData> getPlayerHomes(UUID playerId) {
        try {
            synchronized (playerHomes) {
                return playerHomes.getOrDefault(playerId, new HashMap<>());
            }
        } catch (Exception e) {
            Poppy.LOGGER.error("An error occurred while retrieving homes for player {}", playerId, e);
            return new HashMap<>();
        }
    }

    public static boolean isValidHomeName(String homeName) {
        try {
            if (homeName.length() > MAX_CHARACTER_FOR_HOME_NAME) {
                return false;
            }
            return homeName.matches("[a-zA-Z0-9_-]+");
        } catch (Exception e) {
            Poppy.LOGGER.error("An error occurred while validating the home name: {}", homeName, e);
            return false;
        }
    }
}