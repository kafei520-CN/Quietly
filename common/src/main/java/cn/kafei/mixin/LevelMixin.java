package cn.kafei.mixin;

import cn.kafei.QuietlyCommon;
import cn.kafei.SilentMenuState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Level.class)
public abstract class LevelMixin {
	@Inject(
		method = "playSound(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/core/BlockPos;Lnet/minecraft/sounds/SoundEvent;Lnet/minecraft/sounds/SoundSource;FF)V",
		at = @At("HEAD"),
		cancellable = true,
		require = 0
	)
	private void quietly$muteContainerSoundsEntityBlockPos(
		Entity entity,
		BlockPos pos,
		SoundEvent soundEvent,
		SoundSource soundSource,
		float volume,
		float pitch,
		CallbackInfo ci
	) {
		if (shouldMuteContainerSound(pos, soundEvent, "entity+blockPos")) {
			ci.cancel();
		}
	}

	@Inject(
		method = "playSound(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/core/BlockPos;Lnet/minecraft/sounds/SoundEvent;Lnet/minecraft/sounds/SoundSource;FF)V",
		at = @At("HEAD"),
		cancellable = true,
		require = 0
	)
	private void quietly$muteContainerSoundsPlayerBlockPos(
		Player player,
		BlockPos pos,
		SoundEvent soundEvent,
		SoundSource soundSource,
		float volume,
		float pitch,
		CallbackInfo ci
	) {
		if (shouldMuteContainerSound(pos, soundEvent, "player+blockPos")) {
			ci.cancel();
		}
	}

	@Inject(
		method = "playSound(Lnet/minecraft/world/entity/player/Player;DDDLnet/minecraft/sounds/SoundEvent;Lnet/minecraft/sounds/SoundSource;FF)V",
		at = @At("HEAD"),
		cancellable = true,
		require = 0
	)
	private void quietly$muteContainerSoundsPlayer(
		Player player,
		double x,
		double y,
		double z,
		SoundEvent soundEvent,
		SoundSource soundSource,
		float volume,
		float pitch,
		CallbackInfo ci
	) {
		if (shouldMuteContainerSound(BlockPos.containing(x, y, z), soundEvent, "player+xyz")) {
			ci.cancel();
		}
	}

	private boolean shouldMuteContainerSound(BlockPos pos, SoundEvent soundEvent, String overload) {
		if (!QuietlyCommon.isSilentContainerSound(soundEvent)) {
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
