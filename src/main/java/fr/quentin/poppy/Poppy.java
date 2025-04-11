package fr.quentin.poppy;

import fr.quentin.poppy.commands.HomeCommands;
import fr.quentin.poppy.data.HomeDataManager;
import fr.quentin.poppy.data.PendingTeleport;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Poppy implements ModInitializer {
	public static final String MOD_ID = "poppy";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		HomeDataManager.load();
		HomeCommands.register();

		Runtime.getRuntime().addShutdownHook(new Thread(HomeDataManager::save));
		ServerTickEvents.END_SERVER_TICK.register(PendingTeleport::tick);
	}
}