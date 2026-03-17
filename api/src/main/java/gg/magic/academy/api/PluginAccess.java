package gg.magic.academy.api;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

public class PluginAccess {
    public static <T> T getPlugin(Class<T> pluginClass, String pluginName) {
        Plugin plugin = Bukkit.getPluginManager().getPlugin(pluginName);
        if (plugin == null) return null;
        return pluginClass.cast(plugin);
    }

    public static Object getPluginAPI(String pluginName, String methodName) {
        Plugin plugin = Bukkit.getPluginManager().getPlugin(pluginName);
        if (plugin == null) return null;
        try {
            return plugin.getClass().getMethod(methodName).invoke(plugin);
        } catch (Exception e) {
            return null;
        }
    }
}
