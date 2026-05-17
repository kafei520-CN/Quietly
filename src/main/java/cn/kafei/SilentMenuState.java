package cn.kafei;

import net.minecraft.world.inventory.AbstractContainerMenu;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

public final class SilentMenuState {
	private static final ThreadLocal<Boolean> OPENING_SILENTLY = ThreadLocal.withInitial(() -> false);
	private static final Set<AbstractContainerMenu> SILENT_MENUS = Collections.newSetFromMap(new WeakHashMap<>());

	private SilentMenuState() {
	}

	public static void beginSilentOpen() {
		OPENING_SILENTLY.set(true);
	}

	public static void endSilentOpen() {
		OPENING_SILENTLY.remove();
	}

	public static boolean isOpeningSilently() {
		return OPENING_SILENTLY.get();
	}

	public static void markSilentMenu(AbstractContainerMenu menu) {
		if (menu != null) {
			SILENT_MENUS.add(menu);
		}
	}

	public static boolean isSilentMenu(AbstractContainerMenu menu) {
		return SILENT_MENUS.contains(menu);
	}

	public static void clearSilentMenu(AbstractContainerMenu menu) {
		SILENT_MENUS.remove(menu);
	}
}
