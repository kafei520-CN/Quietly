package cn.kafei.neoforge;

import cn.kafei.QuietlyCommon;
import cn.kafei.SilentOpenManager;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@SuppressWarnings("null")
@Mod(QuietlyCommon.MOD_ID)
public final class QuietlyNeoForge {
	public QuietlyNeoForge(IEventBus modBus) {
		QuietlyCommon.initialize(FMLPaths.CONFIGDIR.get());
		NeoForge.EVENT_BUS.addListener(QuietlyNeoForge::onRightClickBlock);
		NeoForge.EVENT_BUS.addListener(QuietlyNeoForge::onServerTick);
		QuietlyCommon.LOGGER.info("Quietly NeoForge initialized");
	}

	private static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
		if (!(event.getEntity() instanceof ServerPlayer player)) {
			return;
		}

		if (!QuietlyCommon.shouldCancelVanillaUse(
			player.isShiftKeyDown(),
			event.getHand(),
			player.getItemInHand(event.getHand()),
			event.getLevel(),
			event.getPos()
		)) {
			return;
		}

		SilentOpenManager.queueOrStart(player, event.getPos());
		event.setCancellationResult(QuietlyCommon.interactionResultSuccess());
		event.setCanceled(true);
	}

	private static void onServerTick(ServerTickEvent.Post event) {
		SilentOpenManager.tickActiveOpens();
	}
}
