package cn.kafei.client;

import cn.kafei.Quietly;
import cn.kafei.network.SilentOpenPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public class QuietlyClient implements ClientModInitializer {
	private static BlockPos activePos;

	@Override
	public void onInitializeClient() {
		ClientTickEvents.END_CLIENT_TICK.register(QuietlyClient::tickClient);
	}

	private static void tickClient(Minecraft minecraft) {
		if (minecraft.player == null || minecraft.level == null || minecraft.screen != null) {
			stopTracking();
			return;
		}

		if (!minecraft.options.keyUse.isDown() || !minecraft.options.keyShift.isDown()) {
			stopTracking();
			return;
		}

		if (!(minecraft.hitResult instanceof BlockHitResult blockHitResult) || minecraft.hitResult.getType() != HitResult.Type.BLOCK) {
			stopTracking();
			return;
		}

		BlockPos currentPos = blockHitResult.getBlockPos();
		if (!Quietly.isSupportedSilentContainer(minecraft.level.getBlockState(currentPos))) {
			stopTracking();
			return;
		}

		if (activePos == null || !activePos.equals(currentPos)) {
			stopTracking();
			activePos = currentPos.immutable();
			ClientPlayNetworking.send(new SilentOpenPayload(activePos, true));
		}
	}

	private static void stopTracking() {
		if (activePos != null) {
			ClientPlayNetworking.send(new SilentOpenPayload(activePos, false));
			activePos = null;
		}
	}
}
