package cn.kafei.mixin;

import cn.kafei.QuietlyCommon;
import cn.kafei.SilentMenuState;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerList.class)
public abstract class PlayerListMixin {
	@Inject(
		method = "broadcast(Lnet/minecraft/world/entity/player/Player;DDDDLnet/minecraft/resources/ResourceKey;Lnet/minecraft/network/protocol/Packet;)V",
		at = @At("HEAD"),
		cancellable = true,
		require = 0
	)
	private void quietly$muteContainerSoundBroadcast(
		@Nullable Player except,
		double x,
		double y,
		double z,
		double radius,
		ResourceKey<Level> dimension,
		Packet<?> packet,
		CallbackInfo ci
	) {
		if (!(packet instanceof ClientboundSoundPacket soundPacket)) {
			return;
		}

		if (!QuietlyCommon.isSilentContainerSound(soundPacket.getSound())) {
			return;
		}

		BlockPos pos = BlockPos.containing(soundPacket.getX(), soundPacket.getY(), soundPacket.getZ());
		if (SilentMenuState.shouldMuteContainerEffects(dimension, pos)) {
			ci.cancel();
		}
	}
}
