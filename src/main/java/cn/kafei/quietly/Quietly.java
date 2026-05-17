package cn.kafei.quietly;

import org.bukkit.plugin.java.JavaPlugin;

public final class Quietly extends JavaPlugin {
    private SilentOpenService silentOpenService;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveResource("messages/zh_cn.properties", false);
        saveResource("messages/en_us.properties", false);

        QuietlyConfig config = QuietlyConfig.load(this);
        QuietlyMessages messages = QuietlyMessages.load(this, config.language());
        silentOpenService = new SilentOpenService(this, config, messages);
        silentOpenService.start();

        getServer().getPluginManager().registerEvents(new QuietlyListener(silentOpenService), this);
    }

    @Override
    public void onDisable() {
        if (silentOpenService != null) {
            silentOpenService.shutdown();
            silentOpenService = null;
        }
    }
}
