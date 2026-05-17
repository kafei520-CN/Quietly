package cn.kafei;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.EnderChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Quietly implements ModInitializer {
	public static final String MOD_ID = "quietly";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		SilentOpenManager.initialize();
		UseBlockCallback.EVENT.register((player, level, hand, hitResult) -> shouldCancelVanillaUse(player.isShiftKeyDown(), hand, level.getBlockState(hitResult.getBlockPos()))
			? InteractionResult.FAIL
			: InteractionResult.PASS);
		LOGGER.info("Quietly initialized");
	}

	public static boolean isSupportedSilentContainer(BlockState state) {
		return state.getBlock() instanceof ChestBlock
			|| state.getBlock() instanceof BarrelBlock
			|| state.getBlock() instanceof EnderChestBlock;
	}

	private static boolean shouldCancelVanillaUse(boolean sneaking, InteractionHand hand, BlockState state) {
		return sneaking && hand == InteractionHand.MAIN_HAND && isSupportedSilentContainer(state);
	}
}
