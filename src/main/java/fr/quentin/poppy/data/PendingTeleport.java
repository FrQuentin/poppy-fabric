package fr.quentin.poppy.data;

import net.minecraft.network.packet.s2c.play.PositionFlag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import net.minecraft.registry.RegistryKey;
import net.minecraft.world.World;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

public class PendingTeleport {
    public record TeleportTask(
            UUID playerId,
            RegistryKey<World> targetDimension,
            Vec3d targetPosition,
            Vec3d initialPosition,
            int remainingTicks,
            String homeName
    ) {}
    private static final List<TeleportTask> pendingTeleports = new ArrayList<>();

    public static void add(ServerPlayerEntity player, RegistryKey<World> dimension, Vec3d position, String homeName) {
        Vec3d initialPos = player.getPos();
        pendingTeleports.add(new TeleportTask(
                player.getUuid(),
                dimension,
                position,
                initialPos,
                60,
                homeName
        ));
    }

    public static void tick(MinecraftServer server) {
        for (int i = 0; i < pendingTeleports.size(); i++) {
            TeleportTask task = pendingTeleports.get(i);
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(task.playerId());

            if (player == null) {
                pendingTeleports.remove(i--);
                continue;
            }

            if (!player.getPos().equals(task.initialPosition())) {
                player.sendMessage(Text.translatable("commands.poppy.home.cancel_moved"));
                pendingTeleports.remove(i--);
                continue;
            }

            if (task.remainingTicks() > 0) {
                pendingTeleports.set(i, new TeleportTask(
                        task.playerId(),
                        task.targetDimension(),
                        task.targetPosition(),
                        task.initialPosition(),
                        task.remainingTicks() - 1,
                        task.homeName()
                ));
            } else {
                ServerWorld targetWorld = server.getWorld(task.targetDimension());
                if (targetWorld == null) {
                    player.sendMessage(Text.translatable("commands.poppy.home.dimension_not_found", task.targetDimension().getValue()));
                    pendingTeleports.remove(i--);
                    continue;
                }

                player.teleport(
                        targetWorld,
                        task.targetPosition().x,
                        task.targetPosition().y,
                        task.targetPosition().z,
                        EnumSet.noneOf(PositionFlag.class),
                        player.getYaw(),
                        player.getPitch(),
                        false
                );
                player.sendMessage(Text.translatable("commands.poppy.home.success", task.homeName()));
                pendingTeleports.remove(i--);
            }
        }
    }
}