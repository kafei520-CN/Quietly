package cn.kafei.quietly;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.block.Barrel;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.block.Container;
import org.bukkit.block.ShulkerBox;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.event.Event.Result;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public final class SilentOpenService {
    private final JavaPlugin plugin;
    private final QuietlyConfig config;
    private final QuietlyMessages messages;
    private final Map<UUID, PendingClick> pendingClicks = new HashMap<>();
    private final Map<UUID, ActiveOpen> activeOpens = new HashMap<>();
    private final Map<UUID, OpenedSilentInventory> openedInventories = new HashMap<>();

    private BukkitTask tickTask;

    public SilentOpenService(JavaPlugin plugin, QuietlyConfig config, QuietlyMessages messages) {
        this.plugin = plugin;
        this.config = config;
        this.messages = messages;
    }

    public void start() {
        if (tickTask != null) {
            tickTask.cancel();
        }
        tickTask = plugin.getServer().getScheduler().runTaskTimer(plugin, (Runnable) this::tick, 1L, 1L);
    }

    public void shutdown() {
        if (tickTask != null) {
            tickTask.cancel();
            tickTask = null;
        }

        for (ActiveOpen activeOpen : activeOpens.values()) {
            activeOpen.close();
        }

        pendingClicks.clear();
        activeOpens.clear();
        openedInventories.clear();
    }

    public void handleInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        Player player = event.getPlayer();
        if (!player.isSneaking() || !player.getInventory().getItemInMainHand().getType().isAir()) {
            return;
        }

        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock == null) {
            return;
        }

        BlockKey blockKey = BlockKey.from(clickedBlock.getLocation());
        if (blockKey == null || resolveTarget(player, blockKey) == null) {
            return;
        }

        event.setUseInteractedBlock(Result.DENY);
        event.setUseItemInHand(Result.DENY);
        event.setCancelled(true);

        queueOrStart(player, blockKey);
    }

    public void handleInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }

        OpenedSilentInventory openedInventory = openedInventories.remove(player.getUniqueId());
        if (openedInventory == null || openedInventory.soundProfile() == null) {
            return;
        }

        suppressNearbySound(openedInventory.blockKey(), openedInventory.soundProfile().closeSound());
        plugin.getServer().getScheduler().runTaskLater(
            plugin,
            () -> suppressNearbySound(openedInventory.blockKey(), openedInventory.soundProfile().closeSound()),
            1L
        );
    }

    public void cancel(Player player) {
        if (player == null) {
            return;
        }

        UUID playerId = player.getUniqueId();
        pendingClicks.remove(playerId);
        ActiveOpen activeOpen = activeOpens.remove(playerId);
        if (activeOpen != null) {
            activeOpen.close();
        }
    }

    private void queueOrStart(Player player, BlockKey blockKey) {
        OpenTarget target = resolveTarget(player, blockKey);
        if (target == null) {
            cancel(player);
            return;
        }

        UUID playerId = player.getUniqueId();
        long now = System.currentTimeMillis();
        PendingClick pendingClick = pendingClicks.get(playerId);
        if (pendingClick != null && pendingClick.matches(blockKey) && pendingClick.expiresAtMillis() >= now) {
            pendingClicks.remove(playerId);
            startOrRefresh(player, target);
            return;
        }

        pendingClicks.put(playerId, new PendingClick(blockKey, now + config.doubleClickWindowMs()));
    }

    private void startOrRefresh(Player player, OpenTarget target) {
        UUID playerId = player.getUniqueId();
        ActiveOpen existing = activeOpens.get(playerId);
        if (existing != null && existing.matches(target.blockKey())) {
            existing.refresh();
            return;
        }

        cancel(player);
        activeOpens.put(playerId, new ActiveOpen(player, target));
    }

    private void tick() {
        long now = System.currentTimeMillis();
        pendingClicks.entrySet().removeIf(entry -> entry.getValue().expiresAtMillis() < now);

        Iterator<Map.Entry<UUID, ActiveOpen>> iterator = activeOpens.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, ActiveOpen> entry = iterator.next();
            ActiveOpen activeOpen = entry.getValue();
            if (tick(activeOpen)) {
                continue;
            }

            activeOpen.close();
            iterator.remove();
        }
    }

    private boolean tick(ActiveOpen activeOpen) {
        Player player = Bukkit.getPlayer(activeOpen.playerId());
        if (player == null) {
            return false;
        }
        if (!canKeepOpening(player, activeOpen.blockKey())) {
            return false;
        }

        OpenTarget target = resolveTarget(player, activeOpen.blockKey());
        if (target == null) {
            return false;
        }

        activeOpen.elapsedTicks++;
        activeOpen.update();
        if (activeOpen.elapsedTicks < activeOpen.totalTicks) {
            return true;
        }

        openSilently(player, target);
        return false;
    }

    private boolean canKeepOpening(Player player, BlockKey blockKey) {
        if (!player.isOnline() || player.isDead() || !player.isSneaking()) {
            return false;
        }
        if (hasNonPlayerInventoryOpen(player)) {
            return false;
        }

        Location center = blockKey.toCenterLocation();
        Location playerLocation = player.getLocation();
        if (center == null || playerLocation.getWorld() != center.getWorld()) {
            return false;
        }

        double maxDistanceSquared = config.interactionRange() * config.interactionRange();
        return playerLocation.distanceSquared(center) <= maxDistanceSquared;
    }

    private boolean hasNonPlayerInventoryOpen(Player player) {
        InventoryView view = player.getOpenInventory();
        if (view == null) {
            return false;
        }

        InventoryType type = view.getTopInventory().getType();
        return type != InventoryType.CRAFTING && type != InventoryType.CREATIVE;
    }

    private OpenTarget resolveTarget(Player player, BlockKey blockKey) {
        Block block = blockKey.resolveBlock();
        if (block == null) {
            return null;
        }

        Material type = block.getType();
        BlockState state = block.getState();

        if (state instanceof Chest chest) {
            return new OpenTarget(blockKey, countKinds(chest.getInventory()), SoundProfile.CHEST, OpenMode.INVENTORY, chest.getInventory());
        }
        if (state instanceof Barrel barrel) {
            return new OpenTarget(blockKey, countKinds(barrel.getInventory()), SoundProfile.BARREL, OpenMode.INVENTORY, barrel.getInventory());
        }
        if (type == Material.ENDER_CHEST) {
            return new OpenTarget(blockKey, countKinds(player.getEnderChest()), SoundProfile.ENDER_CHEST, OpenMode.INVENTORY, player.getEnderChest());
        }
        if (state instanceof ShulkerBox shulkerBox) {
            return new OpenTarget(blockKey, countKinds(shulkerBox.getInventory()), SoundProfile.SHULKER_BOX, OpenMode.INVENTORY, shulkerBox.getInventory());
        }
        if (state instanceof Container container) {
            return new OpenTarget(blockKey, countKinds(container.getInventory()), null, OpenMode.INVENTORY, container.getInventory());
        }
        if (type == Material.CRAFTING_TABLE) {
            return new OpenTarget(blockKey, 1, null, OpenMode.WORKBENCH, null);
        }
        if (type == Material.ENCHANTING_TABLE) {
            return new OpenTarget(blockKey, 1, null, OpenMode.ENCHANTING, null);
        }

        return null;
    }

    private void openSilently(Player player, OpenTarget target) {
        boolean opened = switch (target.openMode()) {
            case INVENTORY -> player.openInventory(target.inventory()) != null;
            case WORKBENCH -> player.openWorkbench(target.blockKey().toLocation(), true) != null;
            case ENCHANTING -> player.openEnchanting(target.blockKey().toLocation(), true) != null;
        };

        if (!opened || target.soundProfile() == null) {
            return;
        }

        openedInventories.put(player.getUniqueId(), new OpenedSilentInventory(target.blockKey(), target.soundProfile()));
        suppressNearbySound(target.blockKey(), target.soundProfile().openSound());
        plugin.getServer().getScheduler().runTaskLater(
            plugin,
            () -> suppressNearbySound(target.blockKey(), target.soundProfile().openSound()),
            1L
        );
    }

    private void suppressNearbySound(BlockKey blockKey, Sound sound) {
        if (sound == null) {
            return;
        }

        Location center = blockKey.toCenterLocation();
        if (center == null) {
            return;
        }

        World world = center.getWorld();
        double radiusSquared = config.soundSuppressionRadius() * config.soundSuppressionRadius();
        for (Player nearbyPlayer : world.getPlayers()) {
            Location playerLocation = nearbyPlayer.getLocation();
            if (playerLocation.getWorld() == world && playerLocation.distanceSquared(center) <= radiusSquared) {
                nearbyPlayer.stopSound(sound, SoundCategory.BLOCKS);
            }
        }
    }

    private int countKinds(Inventory inventory) {
        Set<Material> kinds = new HashSet<>();
        for (ItemStack itemStack : inventory.getContents()) {
            if (itemStack != null && !itemStack.getType().isAir()) {
                kinds.add(itemStack.getType());
            }
        }
        return Math.max(1, kinds.size());
    }

    private enum OpenMode {
        INVENTORY,
        WORKBENCH,
        ENCHANTING
    }

    private enum SoundProfile {
        CHEST(Sound.BLOCK_CHEST_OPEN, Sound.BLOCK_CHEST_CLOSE),
        BARREL(Sound.BLOCK_BARREL_OPEN, Sound.BLOCK_BARREL_CLOSE),
        ENDER_CHEST(Sound.BLOCK_ENDER_CHEST_OPEN, Sound.BLOCK_ENDER_CHEST_CLOSE),
        SHULKER_BOX(Sound.BLOCK_SHULKER_BOX_OPEN, Sound.BLOCK_SHULKER_BOX_CLOSE);

        private final Sound openSound;
        private final Sound closeSound;

        SoundProfile(Sound openSound, Sound closeSound) {
            this.openSound = openSound;
            this.closeSound = closeSound;
        }

        public Sound openSound() {
            return openSound;
        }

        public Sound closeSound() {
            return closeSound;
        }
    }

    private record PendingClick(BlockKey blockKey, long expiresAtMillis) {
        private boolean matches(BlockKey blockKey) {
            return this.blockKey.equals(blockKey);
        }
    }

    private record OpenedSilentInventory(BlockKey blockKey, SoundProfile soundProfile) {
    }

    private record OpenTarget(
        BlockKey blockKey,
        int itemKinds,
        SoundProfile soundProfile,
        OpenMode openMode,
        Inventory inventory
    ) {
    }

    private final class ActiveOpen {
        private final UUID playerId;
        private final BlockKey blockKey;
        private final BossBar bossBar;
        private final int totalTicks;
        private int elapsedTicks;

        private ActiveOpen(Player player, OpenTarget target) {
            this.playerId = player.getUniqueId();
            this.blockKey = target.blockKey();
            this.totalTicks = config.baseOpenTicks() + target.itemKinds() * config.ticksPerItemKind();
            this.bossBar = Bukkit.createBossBar(messages.progress(this.totalTicks / 20.0D), BarColor.BLUE, BarStyle.SOLID);
            this.bossBar.addPlayer(player);
            this.bossBar.setProgress(0.0D);
        }

        private boolean matches(BlockKey blockKey) {
            return this.blockKey.equals(blockKey);
        }

        private UUID playerId() {
            return playerId;
        }

        private BlockKey blockKey() {
            return blockKey;
        }

        private void refresh() {
            update();
        }

        private void update() {
            double remainingSeconds = Math.max(0.0D, (totalTicks - elapsedTicks) / 20.0D);
            bossBar.setTitle(messages.progress(remainingSeconds));
            bossBar.setProgress(Math.min(1.0D, (double) elapsedTicks / (double) totalTicks));
        }

        private void close() {
            bossBar.removeAll();
            bossBar.setVisible(false);
        }
    }

    private record BlockKey(UUID worldId, int x, int y, int z) {
        private static BlockKey from(Location location) {
            if (location == null || location.getWorld() == null) {
                return null;
            }
            return new BlockKey(location.getWorld().getUID(), location.getBlockX(), location.getBlockY(), location.getBlockZ());
        }

        private Block resolveBlock() {
            World world = Bukkit.getWorld(worldId);
            return world == null ? null : world.getBlockAt(x, y, z);
        }

        private Location toLocation() {
            World world = Bukkit.getWorld(worldId);
            return world == null ? null : new Location(world, x, y, z);
        }

        private Location toCenterLocation() {
            World world = Bukkit.getWorld(worldId);
            return world == null ? null : new Location(world, x + 0.5D, y + 0.5D, z + 0.5D);
        }
    }
}
