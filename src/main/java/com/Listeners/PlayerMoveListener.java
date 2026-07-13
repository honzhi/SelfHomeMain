package com.Listeners;

import com.SelfHome.Main;
import com.SelfHome.Variable;
import com.Util.MySQL;
import com.Util.Util;
import java.io.File;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class PlayerMoveListener implements Listener {
    @EventHandler
    public void onMove(final PlayerMoveEvent event) {
        (new BukkitRunnable() {
                public void run() {
                    final Player p = event.getPlayer();
                    if (!p.isOp()) {
                        if (Util.CheckIsHome(p.getWorld().getName().replace(Variable.world_prefix, ""))) {
                            double set_x = 0.0;
                            double min_x = 0.0;
                            double set_z = 0.0;
                            double min_z = 0.0;
                            if (Variable.bungee) {
                                int level = Integer.valueOf(MySQL.getLevel(p.getWorld().getName().replace(Variable.world_prefix, "")));
                                set_x = p.getWorld().getSpawnLocation().getX()
                                    + Main.JavaPlugin.getConfig().getInt("WorldBoard") / 2
                                    + Main.JavaPlugin.getConfig().getInt("UpdateRadius") / 2 * (level - 1);
                                min_x = p.getWorld().getSpawnLocation().getX()
                                    - Main.JavaPlugin.getConfig().getInt("WorldBoard") / 2
                                    - Main.JavaPlugin.getConfig().getInt("UpdateRadius") / 2 * (level - 1);
                                set_z = p.getWorld().getSpawnLocation().getZ()
                                    + Main.JavaPlugin.getConfig().getInt("WorldBoard") / 2
                                    + Main.JavaPlugin.getConfig().getInt("UpdateRadius") / 2 * (level - 1);
                                min_z = p.getWorld().getSpawnLocation().getZ()
                                    - Main.JavaPlugin.getConfig().getInt("WorldBoard") / 2
                                    - Main.JavaPlugin.getConfig().getInt("UpdateRadius") / 2 * (level - 1);
                            } else {
                                File f2 = new File(Variable.Tempf, String.valueOf(p.getWorld().getName().replace(Variable.world_prefix, "")) + ".yml");
                                YamlConfiguration yamlConfiguration = YamlConfiguration.loadConfiguration(f2);
                                Location loc = p.getLocation();
                                set_x = p.getWorld().getSpawnLocation().getX()
                                    + Main.JavaPlugin.getConfig().getInt("WorldBoard") / 2
                                    + Main.JavaPlugin.getConfig().getInt("UpdateRadius") / 2 * (yamlConfiguration.getInt("Level") - 1);
                                min_x = p.getWorld().getSpawnLocation().getX()
                                    - Main.JavaPlugin.getConfig().getInt("WorldBoard") / 2
                                    - Main.JavaPlugin.getConfig().getInt("UpdateRadius") / 2 * (yamlConfiguration.getInt("Level") - 1);
                                set_z = p.getWorld().getSpawnLocation().getZ()
                                    + Main.JavaPlugin.getConfig().getInt("WorldBoard") / 2
                                    + Main.JavaPlugin.getConfig().getInt("UpdateRadius") / 2 * (yamlConfiguration.getInt("Level") - 1);
                                min_z = p.getWorld().getSpawnLocation().getZ()
                                    - Main.JavaPlugin.getConfig().getInt("WorldBoard") / 2
                                    - Main.JavaPlugin.getConfig().getInt("UpdateRadius") / 2 * (yamlConfiguration.getInt("Level") - 1);
                            }

                            if (set_x < min_x) {
                                double temp = set_x;
                                set_x = min_x;
                                min_x = temp;
                            }

                            if (set_z < min_z) {
                                double temp = set_z;
                                set_z = min_z;
                                min_z = temp;
                            }

                            if (!(p.getLocation().getX() <= min_x)
                                && !(p.getLocation().getX() >= set_x)
                                && !(p.getLocation().getZ() <= min_z)
                                && !(p.getLocation().getZ() >= set_z)) {
                                if (!(p.getLocation().getX() + 15.0 <= min_x)
                                    && !(p.getLocation().getX() - 15.0 >= set_x)
                                    && !(p.getLocation().getZ() + 15.0 <= min_z)
                                    && !(p.getLocation().getZ() - 15.0 >= set_z)) {
                                    if (p.getGameMode() == GameMode.ADVENTURE) {
                                        (new BukkitRunnable() {
                                            public void run() {
                                                p.setGameMode(GameMode.SURVIVAL);
                                            }
                                        }).runTask(Main.JavaPlugin);
                                    }
                                } else {
                                    if (Main.JavaPlugin.getConfig().getBoolean("PlayerMoveOverBorderBuff") && !Variable.AddDebuff.contains(p.getName())) {
                                        Variable.AddDebuff.add(p.getName());
                                    }

                                    if (Main.JavaPlugin.getConfig().getBoolean("PlayerMoveOverBorderHit")) {
                                        p.setVelocity(new Vector(0, 0, -3));
                                    }

                                    this.cancel();
                                }
                            } else {
                                if (Main.JavaPlugin.getConfig().getBoolean("EnableAdventureMode") && p.getGameMode() != GameMode.ADVENTURE) {
                                    p.setGameMode(GameMode.ADVENTURE);
                                    p.sendMessage(Variable.Lang_YML.getString("PlayerMoveOverBorderButAdventure"));
                                }

                                if (!Main.JavaPlugin.getConfig().getString("BorderCommand").equalsIgnoreCase("")
                                    && !Variable.DispathCommand.contains(p.getName())) {
                                    Variable.DispathCommand.add(p.getName());
                                }

                                this.cancel();
                            }
                        }
                    }
                }
            })
            .runTaskAsynchronously(Main.JavaPlugin);
    }
}
