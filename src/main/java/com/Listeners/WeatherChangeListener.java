package com.Listeners;

import com.SelfHome.Variable;
import com.Util.MySQL;
import java.io.File;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.weather.WeatherChangeEvent;

public class WeatherChangeListener implements Listener {
    @EventHandler
    public void onWeatherChange(WeatherChangeEvent e) {
        if (Variable.bungee) {
            String name = e.getWorld().getName().replace(Variable.world_prefix, "");
            if (MySQL.getlockweather(name).equalsIgnoreCase("true")) {
                e.setCancelled(true);
                e.getWorld().setWeatherDuration(0);
            }
        } else {
            File f2 = new File(Variable.Tempf, String.valueOf(e.getWorld().getName().replace(Variable.world_prefix, "")) + ".yml");
            if (f2.exists()) {
                YamlConfiguration yamlConfiguration = YamlConfiguration.loadConfiguration(f2);
                if (yamlConfiguration.getBoolean("lockweather")) {
                    e.setCancelled(true);
                    e.getWorld().setWeatherDuration(0);
                }
            }
        }
    }
}
