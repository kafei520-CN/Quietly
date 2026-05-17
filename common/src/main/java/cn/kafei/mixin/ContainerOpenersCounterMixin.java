package cn.kafei.mixin;

import cn.kafei.SilentMenuState;
import net.minecraft.world.level.block.entity.ContainerOpenersCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ContainerOpenersCounter.class)
public abstract class ContainerOpenersCounterMixin {
	@Inject(
		method = "incrementOpeners(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)V",
		at = @At("HEAD"),
		cancellable = true,
		require = 0
	)
	private void quietly$cancelIncrementOpenersPlayer(CallbackInfo ci) {
		if (shouldMuteContainerEffects()) {
			ci.cancel();
		}
	}

	@Inject(
		method = "decrementOpeners(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)V",
		at = @At("HEAD"),
		cancellable = true,
		require = 0
	)
	private void quietly$cancelDecrementOpenersPlayer(CallbackInfo ci) {
		if (shouldMuteContainerEffects()) {
			ci.cancel();
		}
	}

	@Inject(method = "recheckOpeners", at = @At("HEAD"), cancellable = true, require = 0)
	private void quietly$cancelRecheckOpeners(CallbackInfo ci) {
		if (shouldMuteContainerEffects()) {
			ci.cancel();
		}
	}

	private static boolean shouldMuteContainerEffects() {
		return SilentMenuState.isOpeningSilently() || SilentMenuState.isSuppressingContainerSounds();
	}
}
