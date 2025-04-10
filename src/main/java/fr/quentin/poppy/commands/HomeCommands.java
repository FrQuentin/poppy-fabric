package fr.quentin.poppy.commands;

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
import net.minecraft.text.Text;

public class HomeCommands {
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

        HomeData home = HomeDataManager.getHome(playerId, homeName);
        if (home != null) {
            if (player.getWorld().getBlockState(player.getBlockPos()).isIn(BlockTags.PORTALS)) {
                source.sendError(Text.translatable("commands.poppy.home.portal_error"));
                return 0;
            }

            PendingTeleport.add(player, home.dimension(), home.position(), homeName);
            source.sendFeedback(() -> Text.translatable("commands.poppy.home.teleporting"), false);
        } else {
            source.sendError(Text.translatable("commands.poppy.home.not_found", homeName));
        }
        return 1;
    }
}