package com.Listeners;

import com.SelfHome.Main;
import com.SelfHome.Variable;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scheduler.BukkitRunnable;

public class PlayerJoinListener implements Listener {
    @EventHandler
    public void onJoin(final PlayerJoinEvent event) {
        if (!Main.JavaPlugin.getConfig().getString("NormalJoinWorld").equalsIgnoreCase("")) {
            World world = Bukkit.getWorld(String.valueOf(Variable.world_prefix) + Main.JavaPlugin.getConfig().getString("NormalJoinWorld"));
            event.getPlayer().teleport(world.getSpawnLocation());
        }

        (new BukkitRunnable() {
            public void run() {
                if (Main.JavaPlugin.getConfig().getBoolean("Debug")) {
                    Main.JavaPlugin.getLogger().info("[调试]:当前跨服通信 - 传送回家 - 数据集合:" + Variable.wait_to_spawn_home.toString());
                    Main.JavaPlugin.getLogger().info("[调试]:当前跨服通信 - 延时指令 - 数据集合:" + Variable.wait_to_command.toString());
                }

                if (Variable.wait_to_spawn_home.containsKey(event.getPlayer().getName())) {
                    String command = Variable.wait_to_spawn_home.get(event.getPlayer().getName());
                    Bukkit.dispatchCommand(event.getPlayer(), command);
                    Variable.wait_to_spawn_home.remove(event.getPlayer().getName());
                    if (Main.JavaPlugin.getConfig().getBoolean("Debug")) {
                        Main.JavaPlugin.getLogger().info("[调试]:将玩家" + event.getPlayer().getName() + "传送至家园");
                    }
                }

                if (Variable.wait_to_command.containsKey(event.getPlayer().getName())) {
                    String command = Variable.wait_to_command.get(event.getPlayer().getName());
                    Bukkit.dispatchCommand(event.getPlayer(), command);
                    Variable.wait_to_command.remove(event.getPlayer().getName());
                    if (Main.JavaPlugin.getConfig().getBoolean("Debug")) {
                        Main.JavaPlugin.getLogger().info("[调试]:将玩家" + event.getPlayer().getName() + "延时执行指令");
                    }
                }

                if (Variable.has_already_move_world.contains(event.getPlayer().getName())) {
                    Variable.has_already_move_world.remove(event.getPlayer().getName());
                }
            }
        }).runTaskLater(Main.JavaPlugin, 10L);

    }
}
