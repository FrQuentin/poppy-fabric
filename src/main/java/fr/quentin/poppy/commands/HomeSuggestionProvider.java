package fr.quentin.poppy.commands;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import fr.quentin.poppy.Poppy;
import fr.quentin.poppy.data.HomeDataManager;
import net.minecraft.server.command.ServerCommandSource;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class HomeSuggestionProvider implements SuggestionProvider<ServerCommandSource> {
    @Override
    public CompletableFuture<Suggestions> getSuggestions(CommandContext<ServerCommandSource> context, SuggestionsBuilder builder) {
        try {
            var source = context.getSource();
            var player = source.getPlayerOrThrow();
            var playerId = player.getUuid();

            synchronized (HomeDataManager.class) {
                Map<String, ?> homes = HomeDataManager.getPlayerHomes(playerId);
                if (homes != null) {
                    for (String homeName : homes.keySet()) {
                        builder.suggest(homeName);
                    }
                }
            }
        } catch (Exception e) {
            Poppy.LOGGER.error("An error occurred while fetching home suggestions for a player.", e);
        }
        return builder.buildFuture();
    }
}