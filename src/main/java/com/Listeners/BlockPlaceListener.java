package com.Listeners;

import com.SelfHome.Main;
import com.SelfHome.Variable;
import com.Util.Home;
import com.Util.HomeAPI;
import com.Util.MySQL;
import com.Util.Util;
import java.io.File;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.ClickEvent.Action;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;

public class BlockPlaceListener implements Listener {
    @EventHandler(priority = EventPriority.HIGHEST)
    public void BlockCanBuildEvent(BlockPlaceEvent event) {
        if (!event.getPlayer().getName().toUpperCase().contains("AS-FAKEPLAYER")
            && !event.getPlayer().getName().toUpperCase().contains("[MINECRAFT]")
            && !event.getPlayer().getName().toUpperCase().contains("[MEKANISM]")
            && !event.getPlayer().getName().toUpperCase().contains("[IF]")
            && !event.getPlayer().getName().toUpperCase().contains("[depolyer]".toUpperCase())
            && !event.getPlayer().getName().toUpperCase().contains("[XU2FakePlayer]".toUpperCase())
            && !event.getPlayer().getName().toUpperCase().contains("[Modular Routers]".toUpperCase())) {
            Player p = event.getPlayer();
            if (event.getBlock() != null) {
                if (Variable.Debug.contains(p.getName())) {
                    p.sendMessage("§e§l§m--------------§7[§eDeBug§7]§e§l§m--------------");
                    TextComponent Send_Block_Message = new TextComponent("§eGet-Returned:§d" + Util.getNBTString(event.getBlock().getState()) + " §b>> §dCopy");
                    Send_Block_Message.setClickEvent(new ClickEvent(Action.SUGGEST_COMMAND, Util.getNBTString(event.getBlock().getState())));
                    p.spigot().sendMessage(Send_Block_Message);
                    p.sendMessage("§e§l§m--------------§7[§eDeBug§7]§e§l§m--------------");
                }

                if (Util.CheckIsHome(event.getPlayer().getWorld().getName().replace(Variable.world_prefix, ""))) {
                    if (Main.JavaPlugin.getConfig().getBoolean("CustomTileMax")) {
                        Block block = event.getBlock();
                        String nbt = Util.getNBTString(block.getState());
                        boolean check_contain = false;
                        String contain_nbt = "";
                        int MaxThisTile = 0;

                        for (int d = 0; d < Main.JavaPlugin.getConfig().getStringList("TileList").size(); d++) {
                            String[] temp = ((String)Main.JavaPlugin.getConfig().getStringList("TileList").get(d)).split("\\|");
                            if (temp[0].equalsIgnoreCase("chunk") && nbt.toUpperCase().contains(temp[1].toUpperCase())) {
                                check_contain = true;
                                contain_nbt = temp[1];
                                MaxThisTile = Integer.valueOf(temp[2]);
                                break;
                            }
                        }

                        if (check_contain) {
                            int NowAmount = 0;
                            boolean extra_perm = false;
                            int extra_amount = MaxThisTile;

                            for (int i = 100; i > 0; i--) {
                                if (p.hasPermission("SelfHome.ChunkPlace." + contain_nbt + "." + i)) {
                                    extra_perm = true;
                                    if (extra_amount < i) {
                                        extra_amount = i;
                                        break;
                                    }
                                }
                            }

                            Home home = HomeAPI.getHome(event.getBlock().getWorld().getName().replace(Variable.world_prefix, ""));

                            for (String str : home.getLimitBlock()) {
                                String[] args = str.split("\\|");
                                if (args[0].equalsIgnoreCase("chunk") && nbt.toUpperCase().contains(args[1].toUpperCase())) {
                                    int amount = Integer.valueOf(args[2]);
                                    if (extra_amount < amount) {
                                        extra_perm = true;
                                        extra_amount = amount;
                                    }
                                }
                            }

                            BlockState[] arrayOfBlockState;
                            int j = (arrayOfBlockState = event.getBlock().getChunk().getTileEntities()).length;

                            for (int b = 0; b < j; b++) {
                                BlockState state = arrayOfBlockState[b];
                                if (Util.getNBTString(state).toUpperCase().contains(contain_nbt.toUpperCase())) {
                                    if (Main.JavaPlugin.getConfig().getBoolean("EnableClearExtraBlocks") && ++NowAmount > MaxThisTile) {
                                        event.setCancelled(true);
                                        state.getBlock().getLocation().getWorld().getBlockAt(state.getBlock().getLocation()).setType(Material.AIR);
                                        p.sendMessage(Variable.Lang_YML.getString("ClearExtraBlocks"));
                                    }
                                }
                            }

                            NowAmount--;
                            if (extra_perm) {
                                MaxThisTile = extra_amount;
                            }

                            if (NowAmount + 1 <= MaxThisTile) {
                                String temp = Variable.Lang_YML.getString("PlaceMaxTile");
                                if (temp.contains("<Now>")) {
                                    temp = temp.replace("<Now>", String.valueOf(NowAmount + 1));
                                }

                                if (temp.contains("<Max>")) {
                                    temp = temp.replace("<Max>", String.valueOf(MaxThisTile));
                                }

                                if (temp.contains("<NBT>")) {
                                    temp = temp.replace("<NBT>", String.valueOf(contain_nbt));
                                }

                                p.sendMessage(temp);
                            } else {
                                String temp = Variable.Lang_YML.getString("PlaceReachMaxTile");
                                if (temp.contains("<Now>")) {
                                    temp = temp.replace("<Now>", String.valueOf(NowAmount));
                                }

                                if (temp.contains("<Max>")) {
                                    temp = temp.replace("<Max>", String.valueOf(MaxThisTile));
                                }

                                if (temp.contains("<NBT>")) {
                                    temp = temp.replace("<NBT>", String.valueOf(contain_nbt));
                                }

                                event.setCancelled(true);
                                p.sendMessage(temp);
                            }
                        }
                    }

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

                    if (event.getBlock().getLocation().getX() <= min_x
                        || event.getBlock().getLocation().getX() >= set_x
                        || event.getBlock().getLocation().getZ() <= min_z
                        || event.getBlock().getLocation().getZ() >= set_z) {
                        event.setCancelled(true);
                    }
                }
            }
        }
    }
}
