package fr.quentin.poppy.commands;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import fr.quentin.poppy.data.HomeData;
import fr.quentin.poppy.data.HomeDataManager;
import fr.quentin.poppy.data.PendingTeleport;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.*;

public class HomeCommands {
    private static final Map<UUID, Long> lastHomeCommandUsage = new HashMap<>();
    private static final long HOME_COMMAND_COOLDOWN = 5000;

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(CommandManager.literal("sethome")
                    .then(CommandManager.argument("home", StringArgumentType.string())
                            .executes(context -> setHome(context, StringArgumentType.getString(context, "home"))))
                    .executes(context -> {
                        context.getSource().sendError(Text.translatable("commands.poppy.sethome.usage"));
                        return 0;
                    }));

            dispatcher.register(CommandManager.literal("delhome")
                    .then(CommandManager.argument("home", StringArgumentType.string())
                            .suggests(new HomeSuggestionProvider())
                            .executes(context -> deleteHome(context, StringArgumentType.getString(context, "home"))))
                    .executes(context -> {
                        context.getSource().sendError(Text.translatable("commands.poppy.delhome.usage"));
                        return 0;
                    }));

            dispatcher.register(CommandManager.literal("home")
                    .then(CommandManager.argument("home", StringArgumentType.string())
                            .suggests(new HomeSuggestionProvider())
                            .executes(context -> teleportToHome(context, StringArgumentType.getString(context, "home"))))
                    .executes(context -> {
                        context.getSource().sendError(Text.translatable("commands.poppy.home.usage"));
                        return 0;
                    }));

            dispatcher.register(CommandManager.literal("homes")
                    .executes(context -> listHomes(context, 1))
                    .then(CommandManager.argument("page", IntegerArgumentType.integer(1))
                            .executes(context -> listHomes(context, IntegerArgumentType.getInteger(context, "page")))
                    )
            );
        });
    }

    private static int setHome(CommandContext<ServerCommandSource> context, String homeName) throws CommandSyntaxException {
        var source = context.getSource();
        var player = source.getPlayerOrThrow();
        var playerId = player.getUuid();

        if (!HomeDataManager.isValidHomeName(homeName)) {
            source.sendError(Text.translatable("commands.poppy.sethome.invalid_name", homeName, HomeDataManager.MAX_CHARACTER_FOR_HOME_NAME));
            return 0;
        }

        if (HomeDataManager.getHome(playerId, homeName) != null) {
            source.sendError(Text.translatable("commands.poppy.sethome.already_exists", homeName));
            return 0;
        }

        try {
            var pos = player.getPos();
            var dimension = player.getWorld().getRegistryKey();
            HomeDataManager.addHome(playerId, homeName, dimension, pos);
            source.sendFeedback(() -> Text.translatable("commands.poppy.sethome.success", homeName), false);
        } catch (IllegalStateException e) {
            source.sendError(Text.translatable("commands.poppy.sethome.limit_reached", HomeDataManager.MAX_HOMES_PER_PLAYER));
            return 0;
        }

        return 1;
    }

    private static int deleteHome(CommandContext<ServerCommandSource> context, String homeName) throws CommandSyntaxException {
        var source = context.getSource();
        var player = source.getPlayerOrThrow();

        if (HomeDataManager.removeHome(player.getUuid(), homeName)) {
            source.sendFeedback(() -> Text.translatable("commands.poppy.delhome.success", homeName), false);
        } else {
            source.sendError(Text.translatable("commands.poppy.delhome.not_found", homeName));
        }
        return 1;
    }

    private static int teleportToHome(CommandContext<ServerCommandSource> context, String homeName) throws CommandSyntaxException {
        var source = context.getSource();
        var player = source.getPlayerOrThrow();
        var playerId = player.getUuid();

        long currentTime = System.currentTimeMillis();
        if (lastHomeCommandUsage.containsKey(playerId)) {
            long lastUsage = lastHomeCommandUsage.get(playerId);
            if (currentTime - lastUsage < HOME_COMMAND_COOLDOWN) {
                long remainingCooldown = HOME_COMMAND_COOLDOWN - (currentTime - lastUsage);
                source.sendError(Text.translatable("commands.poppy.home.cooldown", remainingCooldown / 1000));
                return 0;
            }
        }

        HomeData home = HomeDataManager.getHome(playerId, homeName);
        if (home != null) {
            if (player.getWorld().getBlockState(player.getBlockPos()).isIn(BlockTags.PORTALS)) {
                source.sendError(Text.translatable("commands.poppy.home.portal_error"));
                return 0;
            }
            PendingTeleport.add(player, home.dimension(), home.position(), homeName);
            source.sendFeedback(() -> Text.translatable("commands.poppy.home.teleporting"), false);

            lastHomeCommandUsage.put(playerId, currentTime);
        } else {
            source.sendError(Text.translatable("commands.poppy.home.not_found", homeName));
        }
        return 1;
    }

    private static int listHomes(CommandContext<ServerCommandSource> context, int page) throws CommandSyntaxException {
        var source = context.getSource();
        var player = source.getPlayerOrThrow();
        var playerId = player.getUuid();

        Map<String, HomeData> homes = HomeDataManager.getPlayerHomes(playerId);
        if (homes.isEmpty()) {
            source.sendFeedback(() -> Text.translatable("commands.poppy.homes.empty"), false);
            return 0;
        }

        List<Text> homeList = new ArrayList<>();
        for (Map.Entry<String, HomeData> entry : homes.entrySet()) {
            String homeName = entry.getKey();
            HomeData home = entry.getValue();

            String worldName = Text.translatable("dimension." + home.dimension().getValue().getNamespace() + "." + home.dimension().getValue().getPath()).getString();
            String position = String.format("(%.0f, %.0f, %.0f)", home.position().x, home.position().y, home.position().z);

            Text homeEntry = Text.literal("- " + homeName + ": " + worldName + " " + position)
                    .styled(style -> style
                            .withColor(Formatting.WHITE)
                            .withHoverEvent(new HoverEvent.ShowText(Text.translatable("commands.poppy.homes.click_to_teleport", homeName)))
                            .withClickEvent(new ClickEvent.RunCommand("/home " + homeName))
                    );
            homeList.add(homeEntry);
        }

        int maxPerPage = 10;
        int totalPages = (int) Math.ceil(homeList.size() / (double) maxPerPage);

        if (page < 1 || page > totalPages) {
            source.sendError(Text.translatable("commands.poppy.homes.invalid_page", totalPages));
            return 0;
        }

        source.sendFeedback(() -> Text.translatable("commands.poppy.homes.header", homeList.size(), totalPages, page), false);

        for (int i = (page - 1) * maxPerPage; i < Math.min(page * maxPerPage, homeList.size()); i++) {
            final int index = i;
            source.sendFeedback(() -> homeList.get(index), false);
        }

        if (totalPages > 1) {
            source.sendFeedback(() -> Text.translatable("commands.poppy.homes.more_pages", totalPages), false);
        }

        return homeList.size();
    }
}