package fr.quentin.poppy.data;

import net.minecraft.registry.RegistryKey;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public record HomeData(String name, RegistryKey<World> dimension, Vec3d position, String creationDate) {}