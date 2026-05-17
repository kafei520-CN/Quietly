package cn.kafei.mixin;

import cn.kafei.SilentMenuState;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BarrelBlockEntity.class)
public abstract class BarrelBlockEntityMixin {
	@Inject(
		method = "startOpen(Lnet/minecraft/world/entity/player/Player;)V",
		at = @At("HEAD"),
		cancellable = true,
		require = 0
	)
	private void quietly$cancelStartOpenPlayer(CallbackInfo ci) {
		if (shouldMuteContainerEffects()) {
			ci.cancel();
		}
	}

	@Inject(
		method = "stopOpen(Lnet/minecraft/world/entity/player/Player;)V",
		at = @At("HEAD"),
		cancellable = true,
		require = 0
	)
	private void quietly$cancelStopOpenPlayer(CallbackInfo ci) {
		if (shouldMuteContainerEffects()) {
			ci.cancel();
		}
	}

	@Inject(method = "recheckOpen", at = @At("HEAD"), cancellable = true, require = 0)
	private void quietly$cancelRecheckOpen(CallbackInfo ci) {
		if (shouldMuteContainerEffects()) {
			ci.cancel();
		}
	}

	private static boolean shouldMuteContainerEffects() {
		return SilentMenuState.isOpeningSilently() || SilentMenuState.isSuppressingContainerSounds();
	}
}
