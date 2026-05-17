package cn.kafei.fabric;

import cn.kafei.QuietlyCommon;
import cn.kafei.SilentOpenManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

public class QuietlyFabric implements ModInitializer {
	@Override
	public void onInitialize() {
		QuietlyCommon.initialize();
		ServerTickEvents.END_SERVER_TICK.register(server -> SilentOpenManager.tickActiveOpens());
		QuietlyCommon.LOGGER.info("Quietly Fabric initialized");
	}
}
