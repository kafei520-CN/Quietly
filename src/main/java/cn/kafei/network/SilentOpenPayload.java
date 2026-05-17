package cn.kafei.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record SilentOpenPayload(BlockPos pos, boolean active) implements CustomPacketPayload {
	public static final Type<SilentOpenPayload> TYPE = CustomPacketPayload.createType("silent_open");
	public static final StreamCodec<RegistryFriendlyByteBuf, SilentOpenPayload> CODEC = StreamCodec.of(
		(buffer, payload) -> {
			buffer.writeBlockPos(payload.pos());
			buffer.writeBoolean(payload.active());
		},
		buffer -> new SilentOpenPayload(buffer.readBlockPos(), buffer.readBoolean())
	);

	@Override
	public Type<SilentOpenPayload> type() {
		return TYPE;
	}
}
