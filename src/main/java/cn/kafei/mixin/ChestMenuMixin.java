package cn.kafei.mixin;

import cn.kafei.SilentOpenManager;
import cn.kafei.SilentMenuState;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChestMenu.class)
public abstract class ChestMenuMixin {
	@Redirect(
		method = "<init>(Lnet/minecraft/world/inventory/MenuType;ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/Container;I)V",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/world/Container;startOpen(Lnet/minecraft/world/entity/player/Player;)V"
		)
	)
	private void quietly$skipStartOpen(Container container, Player player) {
		if (!SilentMenuState.isOpeningSilently()) {
			container.startOpen(player);
		}
	}

	@Inject(
		method = "<init>(Lnet/minecraft/world/inventory/MenuType;ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/Container;I)V",
		at = @At("TAIL")
	)
	private void quietly$markSilentMenu(MenuType<?> type, int syncId, Inventory inventory, Container container, int rows, CallbackInfo ci) {
		if (SilentMenuState.isOpeningSilently()) {
			SilentMenuState.markSilentMenu((AbstractContainerMenu) (Object) this);
		}
	}

	@Redirect(
		method = "removed",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/world/Container;stopOpen(Lnet/minecraft/world/entity/player/Player;)V"
		)
	)
	private void quietly$skipStopOpen(Container container, Player player) {
		if (!SilentMenuState.isSilentMenu((AbstractContainerMenu) (Object) this)) {
			container.stopOpen(player);
		}
	}

	@Inject(method = "removed", at = @At("TAIL"))
	private void quietly$clearSilentMenu(Player player, CallbackInfo ci) {
		SilentOpenManager.onSilentMenuClosed((AbstractContainerMenu) (Object) this);
		SilentMenuState.clearSilentMenu((AbstractContainerMenu) (Object) this);
	}
}
