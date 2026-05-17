package cn.kafei;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.PlayerEnderChestContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.EnderChestBlock;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.EnderChestBlockEntity;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class SilentOpenManager {
	private static final int BASE_OPEN_TICKS = 12;
	private static final int TICKS_PER_ITEM_KIND = 4;
	private static final Map<UUID, ActiveSilentOpen> ACTIVE_OPENS = new HashMap<>();
	private static final Map<AbstractContainerMenu, AnimationTarget> MENU_ANIMATIONS = new HashMap<>();
	private static final Map<AnimationKey, Integer> ANIMATION_REF_COUNTS = new HashMap<>();

	private SilentOpenManager() {
	}

	public static void startOrRefresh(ServerPlayer player, BlockPos pos) {
		OpenTarget target = resolveOpenTarget(player, pos);
		if (target == null) {
			cancel(player.getUUID());
			return;
		}

		ActiveSilentOpen existing = ACTIVE_OPENS.get(player.getUUID());
		ServerLevel level = QuietlyCommon.getServerLevel(player);
		if (existing != null && level != null && existing.matches(level, pos)) {
			existing.refresh(player, target.itemKinds());
			return;
		}

		cancel(player.getUUID());
		ACTIVE_OPENS.put(player.getUUID(), new ActiveSilentOpen(player, pos, target.itemKinds()));
	}

	public static void cancel(ServerPlayer player) {
		if (player != null) {
			cancel(player.getUUID());
		}
	}

	public static void tickActiveOpens() {
		Iterator<Map.Entry<UUID, ActiveSilentOpen>> iterator = ACTIVE_OPENS.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<UUID, ActiveSilentOpen> entry = iterator.next();
			if (!entry.getValue().tick()) {
				entry.getValue().close();
				iterator.remove();
			}
		}
	}

	private static void cancel(UUID playerId) {
		ActiveSilentOpen removed = ACTIVE_OPENS.remove(playerId);
		if (removed != null) {
			removed.close();
		}
	}

	private static OpenTarget resolveOpenTarget(ServerPlayer player, BlockPos pos) {
		ServerLevel level = QuietlyCommon.getServerLevel(player);
		if (level == null) {
			return null;
		}

		BlockState state = level.getBlockState(pos);
		if (!QuietlyCommon.isSupportedSilentContainer(level, pos) || !canKeepOpening(player, level, pos)) {
			return null;
		}

		MenuProvider genericMenuProvider = state.getMenuProvider(level, pos);

		if (state.getBlock() instanceof ChestBlock chestBlock) {
			Container container = ChestBlock.getContainer(chestBlock, state, level, pos, false);
			if (container == null || genericMenuProvider == null) {
				return null;
			}
			return new OpenTarget(genericMenuProvider, countKinds(container), AnimationTarget.forChest(level, pos));
		}

		BlockEntity blockEntity = level.getBlockEntity(pos);
		if (state.getBlock() instanceof BarrelBlock && blockEntity instanceof BarrelBlockEntity barrel) {
			return new OpenTarget(barrel, countKinds(barrel), AnimationTarget.forBarrel(level, pos));
		}

		if (state.getBlock() instanceof EnderChestBlock && blockEntity instanceof EnderChestBlockEntity enderChest) {
			PlayerEnderChestContainer enderInventory = player.getEnderChestInventory();
			if (enderInventory == null) {
				return null;
			}
			MenuProvider menuProvider = createEnderChestProvider(enderInventory, enderChest);
			return new OpenTarget(menuProvider, countKinds(enderInventory), AnimationTarget.forEnderChest(level, pos));
		}

		if (state.getBlock() instanceof ShulkerBoxBlock && blockEntity instanceof ShulkerBoxBlockEntity shulkerBox) {
			return new OpenTarget(shulkerBox, countKinds(shulkerBox), AnimationTarget.forShulkerBox(level, pos));
		}

		if (genericMenuProvider != null) {
			int itemKinds = blockEntity instanceof Container container ? countKinds(container) : 1;
			return new OpenTarget(genericMenuProvider, itemKinds, null);
		}

		return null;
	}

	private static MenuProvider createEnderChestProvider(PlayerEnderChestContainer enderInventory, EnderChestBlockEntity enderChest) {
		enderInventory.setActiveChest(enderChest);
		return new SimpleMenuProvider(
			(syncId, playerInventory, player) -> ChestMenu.threeRows(syncId, playerInventory, enderInventory),
			Component.translatable("container.enderchest")
		);
	}

	private static int countKinds(Container container) {
		Set<Item> kinds = new HashSet<>();
		int size = container.getContainerSize();
		for (int slot = 0; slot < size; slot++) {
			ItemStack stack = container.getItem(slot);
			if (!stack.isEmpty()) {
				kinds.add(stack.getItem());
			}
		}
		return Math.max(1, kinds.size());
	}

	private static boolean canKeepOpening(ServerPlayer player, ServerLevel level, BlockPos pos) {
		if (!player.isAlive() || !player.isShiftKeyDown()) {
			return false;
		}
		if (player.containerMenu != player.inventoryMenu) {
			return false;
		}
		double reach = player.blockInteractionRange();
		double maxDistanceSqr = reach * reach;
		return player.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) <= maxDistanceSqr;
	}

	private static void openSilently(ServerPlayer player, OpenTarget target) {
		SilentMenuState.beginSilentOpen();
		try {
			player.openMenu(target.menuProvider());
			AbstractContainerMenu menu = player.containerMenu;
			if (menu != null && menu != player.inventoryMenu) {
				SilentMenuState.markSilentMenu(menu);
				if (target.animationTarget() != null) {
					registerOpenAnimation(menu, target.animationTarget());
				}
			}
		} finally {
			SilentMenuState.endSilentOpen();
		}
	}

	private static void registerOpenAnimation(AbstractContainerMenu menu, AnimationTarget animationTarget) {
		MENU_ANIMATIONS.put(menu, animationTarget);
		AnimationKey key = animationTarget.key();
		int newCount = ANIMATION_REF_COUNTS.getOrDefault(key, 0) + 1;
		ANIMATION_REF_COUNTS.put(key, newCount);
		if (newCount == 1) {
			animationTarget.apply(true);
		}
	}

	public static void onSilentMenuClosed(AbstractContainerMenu menu) {
		AnimationTarget animationTarget = MENU_ANIMATIONS.remove(menu);
		if (animationTarget == null) {
			return;
		}

		AnimationKey key = animationTarget.key();
		Integer currentCount = ANIMATION_REF_COUNTS.get(key);
		if (currentCount == null) {
			return;
		}

		if (currentCount <= 1) {
			ANIMATION_REF_COUNTS.remove(key);
			animationTarget.apply(false);
			return;
		}

		ANIMATION_REF_COUNTS.put(key, currentCount - 1);
	}

	private record OpenTarget(MenuProvider menuProvider, int itemKinds, AnimationTarget animationTarget) {
	}

	private record AnimationKey(ServerLevel level, BlockPos pos, AnimationType type) {
	}

	private enum AnimationType {
		CHEST,
		ENDER_CHEST,
		BARREL,
		SHULKER_BOX
	}

	private record AnimationTarget(ServerLevel level, BlockPos pos, AnimationType type) {
		private static AnimationTarget forChest(ServerLevel level, BlockPos pos) {
			return new AnimationTarget(level, pos.immutable(), AnimationType.CHEST);
		}

		private static AnimationTarget forEnderChest(ServerLevel level, BlockPos pos) {
			return new AnimationTarget(level, pos.immutable(), AnimationType.ENDER_CHEST);
		}

		private static AnimationTarget forBarrel(ServerLevel level, BlockPos pos) {
			return new AnimationTarget(level, pos.immutable(), AnimationType.BARREL);
		}

		private static AnimationTarget forShulkerBox(ServerLevel level, BlockPos pos) {
			return new AnimationTarget(level, pos.immutable(), AnimationType.SHULKER_BOX);
		}

		private AnimationKey key() {
			return new AnimationKey(level, pos, type);
		}

		private void apply(boolean open) {
			BlockState state = level.getBlockState(pos);
			SilentMenuState.suppressContainerAt(level, pos);
			SilentMenuState.beginSuppressContainerSounds();
			try {
				if (type == AnimationType.BARREL) {
					if (state.getBlock() instanceof BarrelBlock) {
						level.setBlock(pos, QuietlyCommon.setBooleanProperty(state, "open", open), 3);
					}
					return;
				}

				if (type == AnimationType.CHEST && state.getBlock() instanceof ChestBlock) {
					level.blockEvent(pos, state.getBlock(), 1, open ? 1 : 0);
					return;
				}

				if (type == AnimationType.ENDER_CHEST && state.getBlock() instanceof EnderChestBlock) {
					level.blockEvent(pos, state.getBlock(), 1, open ? 1 : 0);
					return;
				}

				if (type == AnimationType.SHULKER_BOX && state.getBlock() instanceof ShulkerBoxBlock) {
					level.blockEvent(pos, state.getBlock(), 1, open ? 1 : 0);
				}
			} finally {
				SilentMenuState.endSuppressContainerSounds();
			}
		}
	}

	private static final class ActiveSilentOpen {
		private final ServerPlayer player;
		private final ServerLevel level;
		private final BlockPos pos;
		private final ServerBossEvent bossBar;
		private final int totalTicks;
		private int elapsedTicks;

		private ActiveSilentOpen(ServerPlayer player, BlockPos pos, int itemKinds) {
			ServerLevel level = QuietlyCommon.getServerLevel(player);
			if (level == null) {
				throw new IllegalStateException("Unable to resolve ServerLevel for silent open");
			}

			this.player = player;
			this.level = level;
			this.pos = pos.immutable();
			this.totalTicks = BASE_OPEN_TICKS + itemKinds * TICKS_PER_ITEM_KIND;
			this.bossBar = new ServerBossEvent(
				createBossBarName(itemKinds),
				QuietlyCommon.enumValue(BossEvent.BossBarColor.class, "BLUE"),
				QuietlyCommon.enumValue(BossEvent.BossBarOverlay.class, "PROGRESS")
			);
			this.bossBar.addPlayer(player);
			this.bossBar.setProgress(0.0F);
		}

		private boolean matches(ServerLevel level, BlockPos pos) {
			return this.level == level && this.pos.equals(pos);
		}

		private void refresh(ServerPlayer player, int itemKinds) {
			if (!canKeepOpening(player, this.level, this.pos)) {
				return;
			}
			this.bossBar.setName(createBossBarName(itemKinds));
		}

		private boolean tick() {
			ServerLevel currentLevel = QuietlyCommon.getServerLevel(player);
			if (currentLevel != level || !canKeepOpening(player, level, pos)) {
				return false;
			}

			OpenTarget target = resolveOpenTarget(player, pos);
			if (target == null) {
				return false;
			}

			elapsedTicks++;
			bossBar.setProgress(Math.min(1.0F, (float) elapsedTicks / (float) totalTicks));
			if (elapsedTicks < totalTicks) {
				return true;
			}

			openSilently(player, target);
			return false;
		}

		private void close() {
			bossBar.removePlayer(player);
		}

		private static Component createBossBarName(int itemKinds) {
			return Component.translatable("quietly.silent_open.progress", itemKinds);
		}
	}
}
