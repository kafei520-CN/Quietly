package cn.kafei.mixin;

import cn.kafei.QuietlyCommon;
import cn.kafei.SilentMenuState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.gameevent.GameEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelAccessor.class)
public interface LevelAccessorMixin {
	@Inject(
		method = "gameEvent(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/core/Holder;Lnet/minecraft/core/BlockPos;)V",
		at = @At("HEAD"),
		cancellable = true,
		require = 0
	)
	private void quietly$muteSilentContainerGameEvent(Entity entity, Holder<?> gameEvent, BlockPos pos, CallbackInfo ci) {
		if (shouldMuteContainerGameEvent(gameEvent, "entity+holder", pos)) {
			ci.cancel();
		}
	}

	@Inject(
		method = "gameEvent(Lnet/minecraft/core/Holder;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/gameevent/GameEvent$Context;)V",
		at = @At("HEAD"),
		cancellable = true,
		require = 0
	)
	private void quietly$muteSilentContainerGameEventContext(Holder<?> gameEvent, BlockPos pos, GameEvent.Context context, CallbackInfo ci) {
		if (shouldMuteContainerGameEvent(gameEvent, "holder+context", pos)) {
			ci.cancel();
		}
	}

	private boolean shouldMuteContainerGameEvent(Holder<?> gameEvent, String overload, BlockPos pos) {
		if (!QuietlyCommon.isSilentContainerGameEvent(gameEvent)) {
			return false;
		}

		Level level = (Level) (Object) this;
		boolean shouldMute = SilentMenuState.shouldMuteContainerEffects(level, pos);
		if (shouldMute) {
			return true;
		}

		return false;
	}
}
