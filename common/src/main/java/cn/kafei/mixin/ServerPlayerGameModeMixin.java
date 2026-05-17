package cn.kafei.mixin;

import cn.kafei.QuietlyCommon;
import cn.kafei.SilentOpenManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayerGameMode.class)
public abstract class ServerPlayerGameModeMixin {
	@Inject(method = "useItemOn", at = @At("HEAD"), cancellable = true)
	private void quietly$cancelVanillaSilentContainerUse(
		ServerPlayer player,
		Level level,
		ItemStack stack,
		InteractionHand hand,
		BlockHitResult hitResult,
		CallbackInfoReturnable<InteractionResult> cir
	) {
		if (player == null || level == null) {
			return;
		}

		if (QuietlyCommon.shouldCancelVanillaUse(player.isShiftKeyDown(), hand, stack, level, hitResult.getBlockPos())) {
			SilentOpenManager.queueOrStart(player, hitResult.getBlockPos());
			cir.setReturnValue(QuietlyCommon.interactionResultSuccess());
		}
	}
}
