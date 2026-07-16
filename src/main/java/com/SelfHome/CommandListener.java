package com.SelfHome;

import com.GUI.CheckGui;
import com.GUI.CreateGui;
import com.GUI.DenyGui;
import com.GUI.InviteGui;
import com.GUI.MainGui;
import com.GUI.ManageGui;
import com.GUI.ManageGui2;
import com.GUI.TrustGui;
import com.GUI.VisitGui;
import com.Util.Home;
import com.Util.HomeAPI;
import com.Util.MySQL;
import com.Util.StaticsTick;
import com.Util.Util;
import com.comphenix.protocol.utility.StreamSerializer;
import org.mvplugins.multiverse.core.MultiverseCore;
import org.mvplugins.multiverse.core.MultiverseCoreApi;
import org.mvplugins.multiverse.core.world.WorldManager;
import org.mvplugins.multiverse.core.world.MultiverseWorld;
import org.mvplugins.multiverse.core.world.options.ImportWorldOptions;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;
import me.clip.placeholderapi.PlaceholderAPI;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.ClickEvent.Action;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Difficulty;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.World.Environment;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

public class CommandListener implements CommandExecutor, TabExecutor {
    private Location configureTemplateSpawn(String templateName, World world) {
        if (world == null) {
            return null;
        }

        Location originalSpawn = world.getSpawnLocation();
        String path = "TemplateSpawn." + templateName;
        if (!Main.JavaPlugin.getConfig().isConfigurationSection(path)) {
            return originalSpawn;
        }

        Location templateSpawn = new Location(
            world,
            Main.JavaPlugin.getConfig().getDouble(path + ".X", originalSpawn.getX()),
            Main.JavaPlugin.getConfig().getDouble(path + ".Y", originalSpawn.getY()),
            Main.JavaPlugin.getConfig().getDouble(path + ".Z", originalSpawn.getZ()),
            (float)Main.JavaPlugin.getConfig().getDouble(path + ".Yaw", originalSpawn.getYaw()),
            (float)Main.JavaPlugin.getConfig().getDouble(path + ".Pitch", originalSpawn.getPitch())
        );
        templateSpawn.getChunk().load();

        boolean outsideWorld = templateSpawn.getY() < world.getMinHeight() || templateSpawn.getY() + 1 >= world.getMaxHeight();
        boolean blocked = !outsideWorld
            && (!templateSpawn.getBlock().isPassable()
                || !templateSpawn.clone().add(0.0, 1.0, 0.0).getBlock().isPassable()
                || templateSpawn.clone().subtract(0.0, 1.0, 0.0).getBlock().isPassable());
        if (outsideWorld || blocked) {
            Main.JavaPlugin
                .getLogger()
                .warning("Unsafe template spawn for " + templateName + ", using level.dat spawn instead: " + templateSpawn);
            return originalSpawn;
        }

        world.setSpawnLocation(templateSpawn);
        return templateSpawn;
    }

    private void configureMultiverseWorld(String worldName) {
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            Main.JavaPlugin.getLogger().warning("Cannot configure missing Bukkit world in Multiverse-Core: " + worldName);
            return;
        }

        WorldManager worldManager = MultiverseCoreApi.get().getWorldManager();
        MultiverseWorld multiverseWorld = worldManager.getWorld(worldName).getOrNull();
        if (multiverseWorld == null) {
            multiverseWorld = worldManager
                .importWorld(ImportWorldOptions.worldName(worldName).environment(world.getEnvironment()))
                .getOrNull();
        }

        if (multiverseWorld == null) {
            Main.JavaPlugin.getLogger().warning("Cannot register Bukkit world in Multiverse-Core: " + worldName);
            return;
        }

        if (Main.JavaPlugin.getConfig().getBoolean("EnableChatPrefix")) {
            String alias = Variable.Lang_YML.getString("PlaceHolders.WorldName");
            if (alias != null) {
                alias = alias.replace("<PlayerName>", worldName.replace(Variable.world_prefix, ""));
                alias = alias.replace("<WorldName>", worldName.replace(Variable.world_prefix, ""));
                multiverseWorld.setAlias(alias);
            }
        }

        multiverseWorld.setAutoLoad(false);
    }

    public void invite_guoqi(final Player p) {
        (new BukkitRunnable() {
            public void run() {
                if (Variable.invite_list.containsKey(p.getName())) {
                    if (p != null) {
                        String temp2 = Variable.Lang_YML.getString("InviteOtherHasBeenOutDated");
                        if (temp2.contains("<Name>")) {
                            temp2 = temp2.replace("<Name>", Variable.invite_list.get(p.getName()));
                        }

                        p.sendMessage(temp2);
                    }

                    Player beinvite = Bukkit.getPlayer(Variable.invite_list.get(p.getName()));
                    if (beinvite != null) {
                        String temp = Variable.Lang_YML.getString("InviteHasBeenOutDated");
                        if (temp.contains("<Name>")) {
                            temp = temp.replace("<Name>", p.getName());
                        }

                        beinvite.sendMessage(temp);
                    }

                    Variable.invite_list.remove(p.getName());
                }
            }
        }).runTaskLater(Main.JavaPlugin, 600L);
    }

    @EventHandler
    public boolean onCommand(final CommandSender sender, Command cmd, String Label, final String[] args) {
        if (!cmd.getName().equalsIgnoreCase("sh")) {
            return false;
        }

        if (Main.JavaPlugin.getConfig().getBoolean("DisableFunctionButTeleport") && Variable.bungee) {
            if (sender instanceof Player) {
                final Player p = (Player)sender;
                if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
                    if (sender instanceof Player) {
                        Player temp = (Player)sender;
                        if (!temp.isOp()) {
                            sender.sendMessage(Variable.Lang_YML.getString("PlayerIsNotOperator"));
                            return false;
                        }
                    }

                    for (Player ps : Bukkit.getOnlinePlayers()) {
                        if (ps.getOpenInventory() != null) {
                            InventoryHolder inv = ps.getOpenInventory().getTopInventory().getHolder();
                            if (inv instanceof CheckGui
                                || inv instanceof CreateGui
                                || inv instanceof DenyGui
                                || inv instanceof InviteGui
                                || inv instanceof MainGui
                                || inv instanceof ManageGui
                                || inv instanceof ManageGui2
                                || inv instanceof TrustGui
                                || inv instanceof VisitGui) {
                                sender.sendMessage(Variable.Lang_YML.getString("HeadLineTtitle"));
                                sender.sendMessage(Variable.Lang_YML.getString("CloseGuiWhenPluginReload"));
                                sender.sendMessage(Variable.Lang_YML.getString("BottomLineTtitle"));
                                ps.closeInventory();
                            }
                        }
                    }

                    for (World temp : Bukkit.getWorlds()) {
                        if (Variable.hololist.containsKey(temp.getName())) {
                        }
                    }

                    Main.init();
                    sender.sendMessage(Variable.Lang_YML.getString("HeadLineTtitle"));
                    sender.sendMessage(Variable.Lang_YML.getString("ReloadSuccess"));
                    sender.sendMessage(Variable.Lang_YML.getString("BottomLineTtitle"));
                    return false;
                }

                if (args.length == 1 && (args[0].equalsIgnoreCase("home") || args[0].equalsIgnoreCase("h"))) {
                    if (!MySQL.alreadyhastheplayerjoin(p.getName()) && !MySQL.alreadyhastheplayerhome(p.getName())) {
                        String temp = Variable.Lang_YML.getString("NoCreateOrJoin");
                        sender.sendMessage(Variable.Lang_YML.getString("HeadLineTtitle"));
                        sender.sendMessage(temp);
                        sender.sendMessage(Variable.Lang_YML.getString("BottomLineTtitle"));
                        return false;
                    }

                    if (MySQL.alreadyhastheplayerjoin(p.getName())
                        && !MySQL.getJoinServer(p.getName()).equalsIgnoreCase(Main.JavaPlugin.getConfig().getString("Server"))) {
                        try {
                            if (Main.JavaPlugin.getConfig().getBoolean("Debug")) {
                                Main.JavaPlugin.getLogger().info("[调试]:跨服传送信号已发送给" + MySQL.getJoinServer(p.getName()) + "服务器");
                            }

                            /* excluded Channel */
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        (new BukkitRunnable() {
                            public void run() {
                                if (Main.JavaPlugin.getConfig().getBoolean("Debug")) {
                                    Main.JavaPlugin.getLogger().info("[调试]:传送玩家" + p.getName() + "服务器" + MySQL.getJoinServer(p.getName()));
                                }

                                /* excluded Channel */
                            }
                        }).runTaskLater(Main.JavaPlugin, 20L);
                        return false;
                    }

                    if (MySQL.alreadyhastheplayerhome(p.getName())
                        && !MySQL.getServer(p.getName()).equalsIgnoreCase(Main.JavaPlugin.getConfig().getString("Server"))) {
                        try {
                            if (Main.JavaPlugin.getConfig().getBoolean("Debug")) {
                                Main.JavaPlugin.getLogger().info("[调试]:跨服传送信号已发送给" + MySQL.getServer(p.getName()) + "服务器");
                            }

                            /* excluded Channel */
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        (new BukkitRunnable() {
                            public void run() {
                                if (Main.JavaPlugin.getConfig().getBoolean("Debug")) {
                                    Main.JavaPlugin.getLogger().info("[调试]:跨服传送信号已发送给" + MySQL.getServer(p.getName()) + "服务器");
                                }

                                /* excluded Channel */
                            }
                        }).runTaskLater(Main.JavaPlugin, 20L);
                        return false;
                    }
                }

                if (args.length == 4 && args[0].equalsIgnoreCase("admin") && args[1].equalsIgnoreCase("setlevel")) {
                    if (sender instanceof Player) {
                        Player temp = (Player)sender;
                        if (!temp.isOp()) {
                            sender.sendMessage(Variable.Lang_YML.getString("PlayerIsNotOperator"));
                            return false;
                        }
                    }

                    if (!Util.CheckIsHome(args[2])) {
                        String tip = Variable.Lang_YML.getString("NowIsNotHome");
                        sender.sendMessage(tip);
                        return false;
                    }

                    if (Variable.bungee) {
                        MySQL.setLevel(args[2], String.valueOf(Integer.valueOf(args[3])));
                    } else {
                        File f2 = new File(Variable.Tempf, String.valueOf(args[2]) + ".yml");
                        YamlConfiguration yamlConfiguration = YamlConfiguration.loadConfiguration(f2);
                        yamlConfiguration.set("Level", Integer.valueOf(args[3]));

                        try {
                            yamlConfiguration.save(f2);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    /* excluded FirstBorderShaped */
                    sender.sendMessage(Variable.Lang_YML.getString("AdminSetLevelSuccess"));
                    return false;
                }

                if (args.length == 4 && args[0].equalsIgnoreCase("admin") && args[1].equalsIgnoreCase("addlevel")) {
                    if (sender instanceof Player) {
                        Player temp = (Player)sender;
                        if (!temp.isOp()) {
                            sender.sendMessage(Variable.Lang_YML.getString("PlayerIsNotOperator"));
                            return false;
                        }
                    }

                    if (!Util.CheckIsHome(args[2])) {
                        String tip = Variable.Lang_YML.getString("NowIsNotHome");
                        sender.sendMessage(tip);
                        return false;
                    }

                    if (Variable.bungee) {
                        MySQL.setLevel(args[2], String.valueOf(Integer.valueOf(MySQL.getLevel(args[2])) + Integer.valueOf(args[3])));
                    } else {
                        File f2 = new File(Variable.Tempf, String.valueOf(args[2]) + ".yml");
                        YamlConfiguration yamlConfiguration = YamlConfiguration.loadConfiguration(f2);
                        yamlConfiguration.set("Level", yamlConfiguration.getInt("Level") + Integer.valueOf(args[3]));

                        try {
                            yamlConfiguration.save(f2);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    /* excluded FirstBorderShaped */
                    sender.sendMessage(Variable.Lang_YML.getString("AdminAddLevelSuccess"));
                    return false;
                }

                if (args.length == 2 && args[0].equalsIgnoreCase("create") && Main.JavaPlugin.getConfig().getBoolean("BungeeCord")) {
                    if (MySQL.alreadyhastheplayerjoin(p.getName())) {
                        String temp_BungeeCord = Variable.Lang_YML.getString("HasBeenJoin");
                        if (temp_BungeeCord.contains("<ServerName>")) {
                            temp_BungeeCord = temp_BungeeCord.replace("<ServerName>", MySQL.getJoinServer(p.getName()));
                        }

                        sender.sendMessage(Variable.Lang_YML.getString("HeadLineTtitle"));
                        sender.sendMessage(temp_BungeeCord);
                        sender.sendMessage(Variable.Lang_YML.getString("BottomLineTtitle"));
                        return false;
                    }

                    if (MySQL.alreadyhastheplayerhome(p.getName())) {
                        String temp_BungeeCord = Variable.Lang_YML.getString("HasBeenCreate");
                        if (temp_BungeeCord.contains("<ServerName>")) {
                            temp_BungeeCord = temp_BungeeCord.replace("<ServerName>", MySQL.getServer(p.getName()));
                        }

                        sender.sendMessage(Variable.Lang_YML.getString("HeadLineTtitle"));
                        sender.sendMessage(temp_BungeeCord);
                        sender.sendMessage(Variable.Lang_YML.getString("BottomLineTtitle"));
                        return false;
                    }

                    if (Main.JavaPlugin.getConfig().getBoolean("AutoReCreateInLowerLagHome")
                        && !Variable.wait_to_command.containsKey(p.getName())
                        && Main.JavaPlugin.getConfig().getBoolean("BungeeCord")) {
                        if (Main.JavaPlugin.getConfig().getString("DecideBy").equalsIgnoreCase("Player")) {
                            if (!MySQL.getLowerstLagServer().equalsIgnoreCase(Main.JavaPlugin.getConfig().getString("Server"))) {
                                try {
                                    /* excluded Channel */
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }

                                p.sendMessage(Variable.Lang_YML.getString("StartLowestLagServer"));
                                /* excluded Channel */
                                return false;
                            }
                        } else if (!MySQL.getHighestTPSServer().equalsIgnoreCase(Main.JavaPlugin.getConfig().getString("Server"))) {
                            double now = 0.0;
                            if (Bukkit.getVersion().contains("1.7.10")) {
                                now = Bukkit.getTPS()[0];
                            } else {
                                double se1 = Double.valueOf(PlaceholderAPI.setPlaceholders(null, "%server_tps_1%").replace("*", ""));
                                double se2 = Double.valueOf(PlaceholderAPI.setPlaceholders(null, "%server_tps_5%").replace("*", ""));
                                double se3 = Double.valueOf(PlaceholderAPI.setPlaceholders(null, "%server_tps_15%").replace("*", ""));
                                now = (se1 + se2 + se3) / 3.0;
                            }

                            if (MySQL.getServerAmount(MySQL.getLowerstLagServer()) != now) {
                                try {
                                    /* excluded Channel */
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }

                                p.sendMessage(Variable.Lang_YML.getString("StartLowestLagServer"));
                                /* excluded Channel */
                                return false;
                            }
                        }

                        return false;
                    }
                }

                if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
                    if (sender instanceof Player) {
                        Player temp = (Player)sender;
                        if (!temp.isOp()) {
                            sender.sendMessage(Variable.Lang_YML.getString("PlayerIsNotOperator"));
                            return false;
                        }
                    }

                    for (Player pe : Bukkit.getOnlinePlayers()) {
                        if (pe.getOpenInventory() != null) {
                            InventoryHolder inv = pe.getOpenInventory().getTopInventory().getHolder();
                            if (inv instanceof CheckGui
                                || inv instanceof CreateGui
                                || inv instanceof DenyGui
                                || inv instanceof InviteGui
                                || inv instanceof MainGui
                                || inv instanceof ManageGui
                                || inv instanceof ManageGui2
                                || inv instanceof TrustGui
                                || inv instanceof VisitGui) {
                                sender.sendMessage(Variable.Lang_YML.getString("HeadLineTtitle"));
                                sender.sendMessage(Variable.Lang_YML.getString("CloseGuiWhenPluginReload"));
                                sender.sendMessage(Variable.Lang_YML.getString("BottomLineTtitle"));
                                pe.closeInventory();
                            }
                        }
                    }

                    for (World temp : Bukkit.getWorlds()) {
                        if (Variable.hololist.containsKey(temp.getName())) {
                        }
                    }

                    Main.init();
                    sender.sendMessage(Variable.Lang_YML.getString("HeadLineTtitle"));
                    sender.sendMessage(Variable.Lang_YML.getString("ReloadSuccess"));
                    sender.sendMessage(Variable.Lang_YML.getString("BottomLineTtitle"));
                    return false;
                }

                if (args.length == 2 && (args[0].equalsIgnoreCase("tp") || args[0].equalsIgnoreCase("visit") || args[0].equalsIgnoreCase("v"))) {
                    if (!Main.JavaPlugin.getConfig().getBoolean("Permission.Visit") && !p.hasPermission("SelfHome.Visit")) {
                        String tip = Variable.Lang_YML.getString("NoPermissionCheck");
                        if (tip.contains("<Permission>")) {
                            tip = tip.replace("<Permission>", "SelfHome.Visit");
                        }

                        p.sendMessage(tip);
                        return false;
                    }

                    if (!Util.CheckIsHome(args[1])) {
                        String temp = Variable.Lang_YML.getString("TpNotExist");
                        sender.sendMessage(Variable.Lang_YML.getString("HeadLineTtitle"));
                        sender.sendMessage(temp);
                        sender.sendMessage(Variable.Lang_YML.getString("BottomLineTtitle"));
                        return false;
                    }

                    if (!Util.Check(p, args[1]) && !p.hasPermission("SelfHome.forcetp") && !HomeAPI.getHome(args[1]).isAllowStranger()) {
                        p.sendMessage(Variable.Lang_YML.getString("HeadLineTtitle"));
                        String temp = Variable.Lang_YML.getString("TeleportStranger");
                        p.sendMessage(temp);
                        p.sendMessage(Variable.Lang_YML.getString("BottomLineTtitle"));
                        return false;
                    }

                    List<String> blacklist = HomeAPI.getHome(args[1]).getDenys();
                    if (blacklist == null) {
                        blacklist = new ArrayList<>();
                    }

                    for (int i = 0; i < blacklist.size(); i++) {
                        if (blacklist.get(i).equalsIgnoreCase(p.getName())) {
                            String temp = Variable.Lang_YML.getString("TeleportInBlack");
                            sender.sendMessage(Variable.Lang_YML.getString("HeadLineTtitle"));
                            sender.sendMessage(temp);
                            sender.sendMessage(Variable.Lang_YML.getString("BottomLineTtitle"));
                            return false;
                        }
                    }

                    if (Variable.bungee) {
                        if (Util.CheckIsHome(args[1])) {
                            if (!MySQL.getServer(args[1]).equalsIgnoreCase(Main.JavaPlugin.getConfig().getString("Server"))) {
                                try {
                                    /* excluded Channel */
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }

                                /* excluded Channel */
                                return false;
                            }
                        } else {
                            String temp = Variable.Lang_YML.getString("TpNotExist");
                            sender.sendMessage(Variable.Lang_YML.getString("HeadLineTtitle"));
                            sender.sendMessage(temp);
                            sender.sendMessage(Variable.Lang_YML.getString("BottomLineTtitle"));
                        }
                    }

                    return false;
                }

                if (args.length == 1 && (args[0].equalsIgnoreCase("Open") || args[0].equalsIgnoreCase("Menu"))) {
                    MainGui gui = new MainGui(p);
                    p.openInventory(gui.getInventory());
                    return false;
                }

                if (args.length == 2 && args[0].equalsIgnoreCase("Open") && args[1].equalsIgnoreCase("Main")) {
                    MainGui gui = new MainGui(p);
                    p.openInventory(gui.getInventory());
                    return false;
                }

                if (args.length == 2 && args[0].equalsIgnoreCase("Open") && args[1].equalsIgnoreCase("Check")) {
                    CheckGui gui = new CheckGui(p);
                    p.openInventory(gui.getInventory());
                    return false;
                }

                if (args.length == 2 && args[0].equalsIgnoreCase("Open") && args[1].equalsIgnoreCase("Create")) {
                    CreateGui gui = new CreateGui(p);
                    p.openInventory(gui.getInventory());
                    return false;
                }

                if (args.length == 2 && args[0].equalsIgnoreCase("Open") && args[1].equalsIgnoreCase("Manage")) {
                    ManageGui gui = new ManageGui(p);
                    p.openInventory(gui.getInventory());
                    return false;
                }

                if (args.length == 2 && args[0].equalsIgnoreCase("Open") && args[1].equalsIgnoreCase("Manage2")) {
                    ManageGui2 gui = new ManageGui2(p);
                    p.openInventory(gui.getInventory());
                    return false;
                }

                if (args.length == 2 && args[0].equalsIgnoreCase("Open") && args[1].equalsIgnoreCase("Visit")) {
                    VisitGui gui = new VisitGui();
                    p.openInventory(gui.getInventory());
                    return false;
                }

                if (args.length == 2 && args[0].equalsIgnoreCase("Open") && args[1].equalsIgnoreCase("Invite")) {
                    InviteGui gui = new InviteGui();
                    p.openInventory(gui.getInventory());
                    return false;
                }

                if (args.length == 2 && args[0].equalsIgnoreCase("Open") && args[1].equalsIgnoreCase("Trust")) {
                    TrustGui gui = new TrustGui();
                    p.openInventory(gui.getInventory());
                    return false;
                }

                if (args.length == 2 && args[0].equalsIgnoreCase("Open") && args[1].equalsIgnoreCase("Deny")) {
                    DenyGui gui = new DenyGui();
                    p.openInventory(gui.getInventory());
                    return false;
                }

                if (args.length == 1 && args[0].equalsIgnoreCase("close")) {
                    if (p.getOpenInventory() != null) {
                        p.closeInventory();
                    }

                    return false;
                }
            }

            sender.sendMessage(Variable.Lang_YML.getString("DisableFunctionTip"));
            return false;
        } else if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            if (sender instanceof Player) {
                Player temp = (Player)sender;
                if (!temp.isOp()) {
                    sender.sendMessage(Variable.Lang_YML.getString("PlayerIsNotOperator"));
                    return false;
                }
            }

            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getOpenInventory() != null) {
                    InventoryHolder inv = p.getOpenInventory().getTopInventory().getHolder();
                    if (inv instanceof CheckGui
                        || inv instanceof CreateGui
                        || inv instanceof DenyGui
                        || inv instanceof InviteGui
                        || inv instanceof MainGui
                        || inv instanceof ManageGui
                        || inv instanceof ManageGui2
                        || inv instanceof TrustGui
                        || inv instanceof VisitGui) {
                        sender.sendMessage(Variable.Lang_YML.getString("HeadLineTtitle"));
                        sender.sendMessage(Variable.Lang_YML.getString("CloseGuiWhenPluginReload"));
                        sender.sendMessage(Variable.Lang_YML.getString("BottomLineTtitle"));
                        p.closeInventory();
                    }
                }
            }

            for (World temp : Bukkit.getWorlds()) {
                if (Variable.hololist.containsKey(temp.getName())) {
                }
            }

            Main.init();
            sender.sendMessage(Variable.Lang_YML.getString("HeadLineTtitle"));
            sender.sendMessage(Variable.Lang_YML.getString("ReloadSuccess"));
            sender.sendMessage(Variable.Lang_YML.getString("BottomLineTtitle"));
            return false;
        } else if (args.length >= 2 && args[0].equalsIgnoreCase("admin") && args[1].equalsIgnoreCase("resetdelete")) {
            if (!sender.hasPermission("SelfHome.Admin.ResetDelete")) {
                sender.sendMessage(Variable.Lang_YML.getString("PlayerIsNotOperator"));
                return false;
            }

            if (args.length != 3) {
                sender.sendMessage(Variable.Lang_YML.getString("ResetDeleteUsage"));
                return false;
            }

            String playerName = args[2];
            YamlConfiguration yamlConfiguration = YamlConfiguration.loadConfiguration(Variable.f_log);
            List<String> deleteTimes = new ArrayList<>(yamlConfiguration.getStringList("DeleteTimes"));
            boolean removed = false;
            for (int i = deleteTimes.size() - 1; i >= 0; i--) {
                String[] entry = deleteTimes.get(i).split(",", 2);
                if (entry.length > 0 && entry[0].equalsIgnoreCase(playerName)) {
                    deleteTimes.remove(i);
                    removed = true;
                }
            }

            String messageKey = "ResetDeleteNotFound";
            if (removed) {
                yamlConfiguration.set("DeleteTimes", deleteTimes);
                try {
                    yamlConfiguration.save(Variable.f_log);
                    messageKey = "ResetDeleteSuccess";
                } catch (Exception e) {
                    e.printStackTrace();
                    messageKey = "ResetDeleteSaveFailed";
                }
            }

            String message = Variable.Lang_YML.getString(messageKey);
            if (message != null) {
                sender.sendMessage(message.replace("<PlayerName>", playerName));
            }
            return false;
        } else if (args.length == 2 && args[0].equalsIgnoreCase("admin") && args[1].equalsIgnoreCase("setspawn")) {
            if (sender instanceof Player) {
                Player temp = (Player)sender;
                if (!temp.isOp()) {
                    sender.sendMessage(Variable.Lang_YML.getString("PlayerIsNotOperator"));
                    return false;
                }

                temp = (Player)sender;
                World world = temp.getWorld();
                if (!Bukkit.getVersion().contains("1.7.10") && !Bukkit.getVersion().contains("1.7.2")) {
                    world.setSpawnLocation(temp.getLocation());
                } else {
                    world.setSpawnLocation((int)temp.getLocation().getX(), (int)temp.getLocation().getY(), (int)temp.getLocation().getZ());
                }

                if (Variable.hook_multiverseCore) {
                    WorldManager mv_m = MultiverseCoreApi.get().getWorldManager();
                    MultiverseWorld mv = mv_m.getWorld(temp.getLocation().getWorld().getName()).getOrNull();
                    mv.setSpawnLocation(temp.getLocation());
                }

                sender.sendMessage(Variable.Lang_YML.getString("AdminSetSpawnSuccess"));
                return false;
            } else {
                sender.sendMessage(Variable.Lang_YML.getString("CommandSenderIsNotAllowToUseTheCommand"));
                return false;
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("admin") && args[1].equalsIgnoreCase("clearout")) {
            if (sender instanceof Player) {
                Player temp = (Player)sender;
                if (!temp.isOp()) {
                    sender.sendMessage(Variable.Lang_YML.getString("PlayerIsNotOperator"));
                    return false;
                }
            }

            if (!Variable.wait_to_confirm_command.contains(sender.getName())) {
                Variable.wait_to_confirm_command.add(sender.getName());
                String message2 = Variable.Lang_YML.getString("OutdateWorldConfirm");
                if (message2.contains("<Day>")) {
                    message2 = message2.replace("<Day>", args[2]);
                }

                sender.sendMessage(message2);
                (new BukkitRunnable() {
                    public void run() {
                        if (Variable.wait_to_confirm_command.contains(sender.getName())) {
                            Variable.wait_to_confirm_command.remove(sender.getName());
                        }
                    }
                }).runTaskLater(Main.JavaPlugin, 100L);
                return false;
            } else {
                Variable.wait_to_confirm_command.remove(sender.getName());
                long now = System.currentTimeMillis();
                int amount = 0;
                List<String> who_has_been_delete = new ArrayList<>();
                if (Variable.bungee) {
                    for (String worldname : MySQL.getAllWorlds()) {
                        long before_time = Long.valueOf(MySQL.getVisitTime(worldname));
                        long distance = (now - before_time) / 86400000L;
                        if (distance > Long.valueOf(args[2])) {
                            HomeAPI.delHome(worldname);
                            who_has_been_delete.add(worldname);
                            amount++;
                        }
                    }
                } else {
                    File folder = new File(Variable.Tempf);
                    File[] arrayOfFile;
                    int j = (arrayOfFile = folder.listFiles()).length;

                    for (int b = 0; b < j; b++) {
                        File temp = arrayOfFile[b];
                        long lastModified = temp.lastModified();
                        long distance = (now - lastModified) / 86400000L;
                        if (distance > Long.valueOf(args[2])) {
                            String want_to = temp.getPath().replace(Variable.Tempf, "").replace(".yml", "").replace(Variable.file_loc_prefix, "");
                            HomeAPI.delHome(want_to);
                            who_has_been_delete.add(want_to);
                            amount++;
                        }
                    }
                }

                String message = Variable.Lang_YML.getString("OutdatedWorldHasBeenDeleted");
                if (message.contains("<Amount>")) {
                    message = message.replace("<Amount>", String.valueOf(amount));
                }

                if (message.contains("<List>")) {
                    message = message.replace("<List>", who_has_been_delete.toString());
                }

                sender.sendMessage(message);
                return false;
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("admin") && args[1].equalsIgnoreCase("load")) {
            if (sender instanceof Player) {
                Player p2 = (Player)sender;
                if (!p2.isOp()) {
                    sender.sendMessage(Variable.Lang_YML.getString("PlayerIsNotOperator"));
                    return false;
                }
            }

            File newf;
            if (Variable.world_prefix.equalsIgnoreCase("")) {
                if (Bukkit.getVersion().toString().toUpperCase().contains("ARCLIGHT")) {
                    newf = new File(Variable.single_server_gen + Variable.world_prefix + Variable.file_loc_prefix + args[2]);
                } else {
                    newf = new File(Variable.single_server_gen + "world" + Variable.file_loc_prefix + args[2]);
                }
            } else {
                newf = new File(Variable.single_server_gen + Variable.world_prefix + Variable.file_loc_prefix + args[2]);
            }

            if (!newf.exists() && !newf.isDirectory() && !args[2].equalsIgnoreCase("world")) {
                sender.sendMessage(Variable.Lang_YML.getString("WorldIsNotExist"));
                return false;
            }

            WorldCreator creator = null;
            creator = new WorldCreator(Variable.world_prefix + args[2]);
            Variable.create_list_home.add(Variable.world_prefix + args[2]);
            Bukkit.createWorld(creator);
            sender.sendMessage("Loaded the World: " + args[2]);
            World world = Bukkit.getWorld(args[2]);
            if (sender instanceof Player) {
                Player p = (Player)sender;
                p.teleport(world.getSpawnLocation());
                p.sendMessage(Variable.Lang_YML.getString("WorldTeleport"));
            }

            return false;
        } else if (args.length == 3 && args[0].equalsIgnoreCase("admin") && args[1].equalsIgnoreCase("tp")) {
            if (sender instanceof Player) {
                Player p3 = (Player)sender;
                if (!p3.isOp() && !p3.hasPermission("SelfHome.Admin.TP." + args[2]) && !p3.hasPermission("SelfHome.Admin.TP.*")) {
                    sender.sendMessage(Variable.Lang_YML.getString("PlayerIsNotOperator"));
                    return false;
                }
            }

            File newf;
            if (Variable.world_prefix.equalsIgnoreCase("")) {
                if (Bukkit.getVersion().toString().toUpperCase().contains("ARCLIGHT")) {
                    newf = new File(Variable.single_server_gen + Variable.world_prefix + Variable.file_loc_prefix + args[2]);
                } else {
                    newf = new File(Variable.single_server_gen + "world" + Variable.file_loc_prefix + args[2]);
                }
            } else {
                newf = new File(Variable.single_server_gen + Variable.world_prefix + Variable.file_loc_prefix + args[2]);
            }

            if (!newf.exists() && !newf.isDirectory() && !args[2].equalsIgnoreCase("world")) {
                sender.sendMessage(Variable.Lang_YML.getString("WorldIsNotExist"));
                return false;
            }

            WorldCreator creator = null;
            Variable.create_list_home.add(Variable.world_prefix + args[2]);
            creator = new WorldCreator(Variable.world_prefix + args[2]);
            Bukkit.createWorld(creator);
            if (sender instanceof Player) {
                Player p = (Player)sender;
                World world = Bukkit.getWorld(args[2]);
                p.teleport(world.getSpawnLocation());
                p.sendMessage(Variable.Lang_YML.getString("WorldTeleport"));
            }

            return false;
        } else if (args.length == 4 && args[0].equalsIgnoreCase("admin") && args[1].equalsIgnoreCase("addlevel")) {
            if (sender instanceof Player) {
                Player temp = (Player)sender;
                if (!temp.isOp()) {
                    sender.sendMessage(Variable.Lang_YML.getString("PlayerIsNotOperator"));
                    return false;
                }
            }

            if (!Util.CheckIsHome(args[2])) {
                String tip = Variable.Lang_YML.getString("NowIsNotHome");
                sender.sendMessage(tip);
                return false;
            }

            if (Variable.bungee) {
                MySQL.setLevel(args[2], String.valueOf(Integer.valueOf(MySQL.getLevel(args[2])) + Integer.valueOf(args[3])));
            } else {
                File f2 = new File(Variable.Tempf, String.valueOf(args[2]) + ".yml");
                YamlConfiguration yamlConfiguration = YamlConfiguration.loadConfiguration(f2);
                yamlConfiguration.set("Level", yamlConfiguration.getInt("Level") + Integer.valueOf(args[3]));

                try {
                    yamlConfiguration.save(f2);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            /* excluded FirstBorderShaped */
            sender.sendMessage(Variable.Lang_YML.getString("AdminAddLevelSuccess"));
            return false;
        } else if (args.length == 4 && args[0].equalsIgnoreCase("admin") && args[1].equalsIgnoreCase("setlevel")) {
            if (sender instanceof Player) {
                Player temp = (Player)sender;
                if (!temp.isOp()) {
                    sender.sendMessage(Variable.Lang_YML.getString("PlayerIsNotOperator"));
                    return false;
                }
            }

            if (!Util.CheckIsHome(args[2])) {
                String tip = Variable.Lang_YML.getString("NowIsNotHome");
                sender.sendMessage(tip);
                return false;
            }

            if (Variable.bungee) {
                MySQL.setLevel(args[2], String.valueOf(Integer.valueOf(args[3])));
            } else {
                File f2 = new File(Variable.Tempf, String.valueOf(args[2]) + ".yml");
                YamlConfiguration yamlConfiguration = YamlConfiguration.loadConfiguration(f2);
                yamlConfiguration.set("Level", Integer.valueOf(args[3]));

                try {
                    yamlConfiguration.save(f2);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            /* excluded FirstBorderShaped */
            sender.sendMessage(Variable.Lang_YML.getString("AdminSetLevelSuccess"));
            return false;
        } else if (args.length == 2 && args[0].equalsIgnoreCase("admin") && args[1].equalsIgnoreCase("export")) {
            if (sender instanceof Player) {
                Player temp = (Player)sender;
                if (!temp.isOp()) {
                    sender.sendMessage(Variable.Lang_YML.getString("PlayerIsNotOperator"));
                    return false;
                }
            }

            if (!Variable.bungee) {
                sender.sendMessage(Variable.Lang_YML.getString("ExportOrImportButBungeeCordHasBeenDisabled"));
                return false;
            } else {
                MySQL.data_export(sender);
                return false;
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("admin") && args[1].equalsIgnoreCase("import")) {
            if (sender instanceof Player) {
                Player temp = (Player)sender;
                if (!temp.isOp()) {
                    sender.sendMessage(Variable.Lang_YML.getString("PlayerIsNotOperator"));
                    return false;
                }
            }

            if (!Variable.bungee) {
                sender.sendMessage(Variable.Lang_YML.getString("ExportOrImportButBungeeCordHasBeenDisabled"));
                return false;
            } else {
                MySQL.data_import(sender);
                return false;
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("admin") && args[1].equalsIgnoreCase("pwp")) {
            if (sender instanceof Player) {
                Player temp = (Player)sender;
                if (!temp.isOp()) {
                    sender.sendMessage(Variable.Lang_YML.getString("PlayerIsNotOperator"));
                    return false;
                }
            }

            String gen_mdk = "plugins\\PlayerWorldsPro";
            YamlConfiguration yamlConfiguration1 = YamlConfiguration.loadConfiguration(new File(gen_mdk, "config.yml"));
            String prefix = yamlConfiguration1.getString("Basic.World-Prefix");
            HashMap<String, String> map = new HashMap<>();
            YamlConfiguration yamlConfiguration2 = YamlConfiguration.loadConfiguration(new File(gen_mdk, "players.yml"));

            for (String key : yamlConfiguration2.getKeys(false)) {
                map.put(key, yamlConfiguration2.getString(String.valueOf(key) + ".Name"));
            }

            YamlConfiguration yamlConfiguration3 = YamlConfiguration.loadConfiguration(new File(gen_mdk, "data.yml"));

            for (String key : yamlConfiguration3.getKeys(true)) {
                if (key.split("\\.").length == 2) {
                    String uuid = key.split("\\.")[1];
                    boolean lockweather = !yamlConfiguration3.getBoolean("Worlds." + uuid + ".1.WeatherCycle");
                    boolean pvp = !yamlConfiguration3.getBoolean("Worlds." + uuid + ".1.PvP");
                    boolean pickup = !yamlConfiguration3.getBoolean("Worlds." + uuid + ".1.Item-Pickup");
                    boolean drop = !yamlConfiguration3.getBoolean("Worlds." + uuid + ".1.Drop-Item");
                    boolean publicAccess = false;
                    boolean has_set_spawn = false;
                    double X = 0.0;
                    double Y = 0.0;
                    double Z = 0.0;
                    boolean has_set_Members = false;
                    new ArrayList();
                    String publicswitch = yamlConfiguration3.getString("Worlds." + uuid + ".1.Access");
                    if (publicswitch.equalsIgnoreCase("Public")) {
                        publicAccess = true;
                    }

                    if (yamlConfiguration3.getString("Worlds." + uuid + ".1.Spawn") != null) {
                        has_set_spawn = true;
                        String[] temp = yamlConfiguration3.getString("Worlds." + uuid + ".1.Spawn").split(";");
                        X = Double.valueOf(temp[0]);
                        Y = Double.valueOf(temp[1]);
                        Z = Double.valueOf(temp[2]);
                    }

                    List<String> Trustlist = new ArrayList<>();
                    if (yamlConfiguration3.getStringList("Worlds." + uuid + ".1.Members") != null) {
                        List<String> list = yamlConfiguration3.getStringList("Worlds." + uuid + ".1.Members");

                        for (int i = 0; i < list.size(); i++) {
                            if (map.get(list.get(i)) != null && !map.get(list.get(i)).contains("\\-")) {
                                Trustlist.add(map.get(list.get(i)));
                                has_set_Members = true;
                            }
                        }
                    }

                    String name = map.get(uuid);
                    File f2 = new File(Variable.Tempf, name + ".yml");
                    if (f2.exists()) {
                        return false;
                    }

                    try {
                        f2.createNewFile();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    YamlConfiguration yamlConfiguration = YamlConfiguration.loadConfiguration(f2);
                    yamlConfiguration.createSection("Members");
                    yamlConfiguration.createSection("OP");
                    yamlConfiguration.createSection("Denys");
                    yamlConfiguration.createSection("Public");
                    yamlConfiguration.createSection("Level");
                    yamlConfiguration.createSection("pvp");
                    yamlConfiguration.createSection("pickup");
                    yamlConfiguration.createSection("drop");
                    yamlConfiguration.createSection("Server");
                    yamlConfiguration.createSection("locktime");
                    yamlConfiguration.createSection("lockweather");
                    yamlConfiguration.createSection("time");
                    yamlConfiguration.createSection("icon");
                    yamlConfiguration.createSection("advertisement");
                    yamlConfiguration.createSection("limitblock");
                    yamlConfiguration.set("Public", publicAccess);
                    yamlConfiguration.set("pickup", pickup);
                    yamlConfiguration.set("drop", drop);
                    yamlConfiguration.set("pvp", pvp);
                    yamlConfiguration.set("locktime", false);
                    yamlConfiguration.set("time", 0);
                    yamlConfiguration.set("lockweather", lockweather);
                    int set_level = 1;
                    yamlConfiguration.set("Level", set_level);
                    yamlConfiguration.set("Server", Main.JavaPlugin.getConfig().getString("Server"));
                    yamlConfiguration.createSection("flowers");
                    yamlConfiguration.createSection("popularity");
                    yamlConfiguration.createSection("gifts");
                    yamlConfiguration.set("flowers", 0);
                    yamlConfiguration.set("popularity", 0);
                    yamlConfiguration.set("gifts", new ArrayList());
                    yamlConfiguration.set("advertisement", new ArrayList());
                    yamlConfiguration.set("limitblock", new ArrayList());
                    yamlConfiguration.set("icon", "");

                    try {
                        yamlConfiguration.save(f2);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    if (has_set_Members && Trustlist != null) {
                        yamlConfiguration.set("Members", Trustlist);
                    }

                    yamlConfiguration.createSection("X");
                    yamlConfiguration.createSection("Y");
                    yamlConfiguration.createSection("Z");
                    if (has_set_spawn) {
                        yamlConfiguration.set("X", X);
                        yamlConfiguration.set("Y", Y);
                        yamlConfiguration.set("Z", Z);
                    } else {
                        yamlConfiguration.set("X", 0.0);
                        yamlConfiguration.set("Y", 0.0);
                        yamlConfiguration.set("Z", 0.0);
                    }

                    try {
                        yamlConfiguration.save(f2);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    sender.sendMessage("成功导出" + name + ".yml到本插件的数据文件夹");
                    System.out.println("成功导出" + name + ".yml到本插件的数据文件夹");
                    File oldf;
                    if (Variable.world_prefix.equalsIgnoreCase("")) {
                        if (Bukkit.getVersion().toString().toUpperCase().contains("ARCLIGHT")) {
                            oldf = new File(Variable.single_server_gen + "PlayerWorldsPro" + Variable.file_loc_prefix);
                        } else {
                            oldf = new File(Variable.single_server_gen + "world" + Variable.file_loc_prefix);
                        }
                    } else {
                        oldf = new File(Variable.single_server_gen + "PlayerWorldsPro" + Variable.file_loc_prefix);
                    }

                    File newf;
                    if (Variable.world_prefix.equalsIgnoreCase("")) {
                        if (Bukkit.getVersion().toString().toUpperCase().contains("ARCLIGHT")) {
                            newf = new File(Variable.single_server_gen + Variable.world_prefix);
                        } else {
                            newf = new File(Variable.single_server_gen + "world" + Variable.file_loc_prefix);
                        }
                    } else {
                        newf = new File(Variable.single_server_gen + Variable.world_prefix);
                    }

                    File oldFile = new File(String.valueOf(oldf.getPath().toString()) + Variable.file_loc_prefix + prefix + uuid);
                    System.out.println(oldFile.getPath());
                    File newFile = new File(String.valueOf(newf.getPath().toString()) + Variable.file_loc_prefix + name);
                    if (oldFile.renameTo(newFile)) {
                        sender.sendMessage(name + "玩家的存档文件重命名成功");
                        System.out.println(name + "玩家的存档文件重命名成功");
                    } else {
                        sender.sendMessage(name + "玩家重命名失败！");
                        System.out.println(name + "玩家重命名失败！");
                    }
                }
            }

            return false;
        } else if (args.length == 2 && args[0].equalsIgnoreCase("ForceDelete")) {
            if (sender instanceof Player) {
                Player temp = (Player)sender;
                if (!temp.isOp()) {
                    sender.sendMessage(Variable.Lang_YML.getString("PlayerIsNotOperator"));
                    return false;
                }
            }

            World world = Bukkit.getWorld(Variable.world_prefix + args[1]);
            if (world != null) {
                for (Player p6 : Bukkit.getWorld(Variable.world_prefix + args[1]).getPlayers()) {
                    p6.teleport(Bukkit.getWorld(Main.JavaPlugin.getConfig().getString("Spawn")).getSpawnLocation());
                    p6.sendMessage(Variable.Lang_YML.getString("WorldHasBeenForceDelete"));
                }

                Bukkit.unloadWorld(Variable.world_prefix + args[1], true);
                sender.sendMessage(Variable.Lang_YML.getString("WorldHasBeenForceDeleteSuccess"));
            }

            if (Variable.hook_multiverseCore) {
                WorldManager mv_m = MultiverseCoreApi.get().getWorldManager();
                mv_m.removeWorld(Variable.world_prefix + args[1]);
            }

            YamlConfiguration yamlConfiguration = YamlConfiguration.loadConfiguration(Variable.f_log);
            yamlConfiguration.set("NowID", yamlConfiguration.getInt("NowID") - 1);

            try {
                yamlConfiguration.save(Variable.f_log);
            } catch (Exception e) {
                e.printStackTrace();
            }

            Object f;
            if (Variable.world_prefix.equalsIgnoreCase("")) {
                if (!Bukkit.getVersion().toString().toUpperCase().contains("ARCLIGHT") && !Bukkit.getVersion().toString().contains("1.20.1")) {
                    f = new File(Variable.single_server_gen + "world" + Variable.file_loc_prefix + args[1]);
                } else {
                    f = new File(Variable.single_server_gen + Variable.world_prefix + args[1]);
                }
            } else {
                f = new File(Variable.single_server_gen + Variable.world_prefix + args[1]);
            }

            Util.deleteFile((File)f);
            sender.sendMessage(Variable.Lang_YML.getString("WorldHasBeenDeleted"));
            if (Variable.bungee) {
                MySQL.removePlayer(args[1]);
                sender.sendMessage(Variable.Lang_YML.getString("WorldConfigHasBeenDeleted"));
            } else {
                File f2 = new File(Variable.Tempf, String.valueOf(args[1]) + ".yml");
                if (f2.exists()) {
                    sender.sendMessage(Variable.Lang_YML.getString("WorldConfigHasBeenDeleted"));
                    f2.delete();
                }
            }

            return false;
        } else if (args.length == 2 && args[0].equalsIgnoreCase("UnLoad")) {
            if (sender instanceof Player) {
                Player temp = (Player)sender;
                if (!temp.isOp()) {
                    sender.sendMessage(Variable.Lang_YML.getString("PlayerIsNotOperator"));
                    return false;
                }
            }

            for (Player p6 : Bukkit.getWorld(args[1]).getPlayers()) {
                p6.teleport(Bukkit.getWorld(Main.JavaPlugin.getConfig().getString("Spawn")).getSpawnLocation());
            }

            Bukkit.unloadWorld(args[1], true);
            sender.sendMessage(Variable.Lang_YML.getString("ForceUnLoadWorld"));
            return false;
        } else if (args.length == 2 && args[0].equalsIgnoreCase("rank")) {
            if (sender instanceof Player) {
                Player temp = (Player)sender;
                if (!Main.JavaPlugin.getConfig().getBoolean("Permission.CommandUser")
                    && !temp.hasPermission("SelfHome.Rank")
                    && !temp.hasPermission("SelfHome.command.user")) {
                    String tip = Variable.Lang_YML.getString("NoPermissionCheck");
                    if (tip.contains("<Permission>")) {
                        tip = tip.replace("<Permission>", "SelfHome.Rank");
                    }

                    temp.sendMessage(tip);
                    return false;
                }
            }

            (new BukkitRunnable() {
                public void run() {
                    int YS = Integer.valueOf(args[1]);
                    if (Variable.world_StaticsTick.size() == 0) {
                        init.refreshWorldStatics(false);
                    }

                    for (int c = 0; c < Main.JavaPlugin.getConfig().getStringList("StatisticsTop").size(); c++) {
                        String a = (String)Main.JavaPlugin.getConfig().getStringList("StatisticsTop").get(c);
                        sender.sendMessage(a);
                    }

                    for (int i = 10 * YS - 10; i < YS * 10 && i < Variable.world_StaticsTick.size(); i++) {
                        StaticsTick s = Variable.world_StaticsTick.get(i);
                        String temp = Main.JavaPlugin.getConfig().getString("ShowFormat");
                        if (temp.contains("<index>")) {
                            temp = temp.replace("<index>", String.valueOf(i + 1));
                        }

                        if (temp.contains("<world>")) {
                            temp = temp.replace("<world>", s.name);
                        }

                        if (temp.contains("<tile>")) {
                            temp = temp.replace("<tile>", String.valueOf(s.tile));
                        }

                        if (temp.contains("<chunk>")) {
                            temp = temp.replace("<chunk>", String.valueOf(s.chunk));
                        }

                        if (temp.contains("<entity>")) {
                            temp = temp.replace("<entity>", String.valueOf(s.entity));
                        }

                        if (temp.contains("<drop>")) {
                            temp = temp.replace("<drop>", String.valueOf(s.drop));
                        }

                        if (temp.contains("<tps>")) {
                            temp = temp.replace("<tps>", String.format(Main.JavaPlugin.getConfig().getString("FormatInfo"), s.tps));
                        }

                        sender.sendMessage(temp);
                    }

                    for (int c = 0; c < Main.JavaPlugin.getConfig().getStringList("StatisticsEnd").size(); c++) {
                        String a = (String)Main.JavaPlugin.getConfig().getStringList("StatisticsEnd").get(c);
                        sender.sendMessage(a);
                    }
                }
            }).runTaskAsynchronously(Main.JavaPlugin);
            return false;
        } else if (args.length == 1 && args[0].equalsIgnoreCase("dimension")) {
            if (sender instanceof Player) {
                Player temp = (Player)sender;
                if (!temp.isOp()) {
                    sender.sendMessage(Variable.Lang_YML.getString("PlayerIsNotOperator"));
                    return false;
                }
            }

            for (World temp : Bukkit.getWorlds()) {
                String dimId = String.valueOf(temp.getEnvironment().ordinal());
                sender.sendMessage("§dWorld:§b" + temp.getName() + "§d,Dimension:§b" + dimId);
            }
            return false;
        } else {
            if (args.length == 4) {
                if (sender instanceof Player) {
                    Player temp = (Player)sender;
                    if (!temp.isOp()) {
                        sender.sendMessage(Variable.Lang_YML.getString("PlayerIsNotOperator"));
                        return false;
                    }
                }

                if (args[0].equalsIgnoreCase("admin") && args[1].equalsIgnoreCase("create")) {
                    if (Main.JavaPlugin.getConfig().getBoolean("BungeeCord")) {
                        if (MySQL.alreadyhastheplayerjoin(args[2])) {
                            String temp_BungeeCord = Variable.Lang_YML.getString("AdminCreateHasJoinButNotServer");
                            if (temp_BungeeCord.contains("<ServerName>")) {
                                temp_BungeeCord = temp_BungeeCord.replace("<ServerName>", MySQL.getJoinServer(args[2]));
                            }

                            sender.sendMessage(Variable.Lang_YML.getString("HeadLineTtitle"));
                            sender.sendMessage(temp_BungeeCord);
                            sender.sendMessage(Variable.Lang_YML.getString("BottomLineTtitle"));
                            return false;
                        }

                        if (MySQL.alreadyhastheplayerhome(args[2])) {
                            String temp_BungeeCord = Variable.Lang_YML.getString("AdminCreateHasCreateButNotServer");
                            if (temp_BungeeCord.contains("<ServerName>")) {
                                temp_BungeeCord = temp_BungeeCord.replace("<ServerName>", MySQL.getServer(args[2]));
                            }

                            sender.sendMessage(Variable.Lang_YML.getString("HeadLineTtitle"));
                            sender.sendMessage(temp_BungeeCord);
                            sender.sendMessage(Variable.Lang_YML.getString("BottomLineTtitle"));
                            return false;
                        }
                    } else {
                        File f2 = new File(Variable.Tempf, String.valueOf(args[2].replace(Variable.world_prefix, "")) + ".yml");
                        if (f2.exists()) {
                            sender.sendMessage(Variable.Lang_YML.getString("HeadLineTtitle"));
                            sender.sendMessage(Variable.Lang_YML.getString("HasBeenCreate"));
                            sender.sendMessage(Variable.Lang_YML.getString("BottomLineTtitle"));
                            return false;
                        }

                        boolean has_been_join = false;
                        File folder = new File(Variable.Tempf);
                        File[] arrayOfFile;
                        int j = (arrayOfFile = folder.listFiles()).length;

                        for (int b = 0; b < j; b++) {
                            File temp = arrayOfFile[b];
                            String want_to = temp.getPath().replace(Variable.Tempf, "").replace(".yml", "").replace(Variable.file_loc_prefix, "");
                            YamlConfiguration yamlConfiguration1 = YamlConfiguration.loadConfiguration(temp);

                            for (int i = 0; i < yamlConfiguration1.getStringList("OP").size(); i++) {
                                String temp_str = (String)yamlConfiguration1.getStringList("OP").get(i);
                                if (temp_str.equalsIgnoreCase(args[2])) {
                                    has_been_join = true;
                                    break;
                                }
                            }
                        }

                        if (has_been_join) {
                            String temp = Variable.Lang_YML.getString("HasAlreadyJoinOthers");
                            sender.sendMessage(Variable.Lang_YML.getString("HeadLineTtitle"));
                            sender.sendMessage(temp);
                            sender.sendMessage(Variable.Lang_YML.getString("BottomLineTtitle"));
                            return false;
                        }
                    }

                    YamlConfiguration yamlConfiguration = YamlConfiguration.loadConfiguration(Variable.f_log);
                    if (!yamlConfiguration.contains("NowID")) {
                        yamlConfiguration.set("NowID", 0);
                    }

                    if (!yamlConfiguration.contains("MaxID")) {
                        yamlConfiguration.set("MaxID", 1000);
                    }

                    try {
                        yamlConfiguration.save(Variable.f_log);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    int nowID = yamlConfiguration.getInt("NowID");
                    int MaxID = yamlConfiguration.getInt("MaxID");
                    if (nowID >= MaxID) {
                        String temp = Variable.Lang_YML.getString("ReachMaxCreate");
                        sender.sendMessage(Variable.Lang_YML.getString("HeadLineTtitle"));
                        sender.sendMessage(temp);
                        sender.sendMessage(Variable.Lang_YML.getString("BottomLineTtitle"));
                        return false;
                    }

                    String v = args[3];
                    if (v.equalsIgnoreCase("1")) {
                        WorldCreator creator = null;
                        creator = new WorldCreator(Variable.world_prefix + args[2]);
                        if (Main.JavaPlugin.getConfig().getBoolean("generateStructures")) {
                            creator = creator.generateStructures(true);
                        } else {
                            creator = creator.generateStructures(false);
                        }

                        creator.type(WorldType.NORMAL);
                        creator = creator.type(WorldType.NORMAL);
                        Variable.create_list_home.add(Variable.world_prefix + args[2]);
                        Bukkit.createWorld(creator);
                    } else if (v.equalsIgnoreCase("2")) {
                        WorldCreator creator = null;
                        creator = new WorldCreator(Variable.world_prefix + args[2]);
                        Main.JavaPlugin.getDefaultWorldGenerator(Variable.world_prefix + args[2], "");
                        if (Main.JavaPlugin.getConfig().getBoolean("generateStructures")) {
                            creator = creator.generateStructures(true);
                        } else {
                            creator = creator.generateStructures(false);
                        }

                        creator = creator.type(WorldType.FLAT);
                        Variable.create_list_home.add(Variable.world_prefix + args[2]);
                        Bukkit.createWorld(creator);
                    } else {
                        if (v.equalsIgnoreCase("random")) {
                            List<String> list = Main.JavaPlugin.getConfig().getStringList("Random");
                            int num = (int)(Math.random() * list.size());
                            Bukkit.dispatchCommand(sender, "sh admin create " + args[2] + " " + list.get(num));
                            return false;
                        }

                        String oldDir = String.valueOf(Variable.worldFinal) + v;
                        String newDir = "";
                        if (Variable.world_prefix.equalsIgnoreCase("")) {
                            if (!Bukkit.getVersion().toString().toUpperCase().contains("ARCLIGHT") && !Bukkit.getVersion().toString().contains("1.20.1")) {
                                newDir = Variable.single_server_gen + "world" + Variable.file_loc_prefix + args[2];
                            } else {
                                newDir = Variable.single_server_gen + Variable.world_prefix + args[2];
                            }
                        } else {
                            newDir = Variable.single_server_gen + Variable.world_prefix + args[2];
                        }

                        File exist_file = new File(oldDir);
                        if (!exist_file.exists()) {
                            String temp = Variable.Lang_YML.getString("WorldFileNotExist");
                            if (temp.contains("<name>")) {
                                temp = temp.replace("<name>", v);
                            }

                            sender.sendMessage(Variable.Lang_YML.getString("HeadLineTtitle"));
                            sender.sendMessage(temp);
                            sender.sendMessage(Variable.Lang_YML.getString("BottomLineTtitle"));
                            return false;
                        }

                        Util.copyDir(oldDir, newDir);
                        WorldCreator creator = null;
                        creator = new WorldCreator(Variable.world_prefix + args[2]);
                        if (Main.JavaPlugin.getConfig().getBoolean("generateStructures")) {
                            creator.generateStructures(true);
                        } else {
                            creator.generateStructures(false);
                        }

                        Variable.create_list_home.add(Variable.world_prefix + args[2]);
                        Bukkit.createWorld(creator);
                    }

                    World world = Bukkit.getWorld(Variable.world_prefix + args[2]);
                    configureTemplateSpawn(v, world);
                    if (Variable.hook_multiverseCore) {
                        configureMultiverseWorld(Variable.world_prefix + args[2]);
                    }

                    if (Variable.bungee) {
                        MySQL.insertvalue(
                            args[2],
                            "",
                            "",
                            "",
                            String.valueOf(Main.JavaPlugin.getConfig().getBoolean("NormalPublic")),
                            "1",
                            String.valueOf(Main.JavaPlugin.getConfig().getBoolean("NormalPVP")),
                            String.valueOf(Main.JavaPlugin.getConfig().getBoolean("NormalPickup")),
                            String.valueOf(Main.JavaPlugin.getConfig().getBoolean("NormalDrop")),
                            Main.JavaPlugin.getConfig().getString("Server"),
                            "false",
                            "false",
                            "0",
                            String.valueOf(world.getSpawnLocation().getX()),
                            String.valueOf(world.getSpawnLocation().getY()),
                            String.valueOf(world.getSpawnLocation().getZ()),
                            "0",
                            "0",
                            "",
                            "",
                            "",
                            "",
                            ""
                        );
                    } else {
                        File f2 = new File(Variable.Tempf, String.valueOf(args[2]) + ".yml");
                        if (f2.exists()) {
                            sender.sendMessage(Variable.Lang_YML.getString("HeadLineTtitle"));
                            sender.sendMessage(Variable.Lang_YML.getString("AdminCreateHomeForPlayerFailed"));
                            sender.sendMessage(Variable.Lang_YML.getString("BottomLineTtitle"));
                            return false;
                        }

                        try {
                            f2.createNewFile();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        YamlConfiguration yamlConfiguration1 = YamlConfiguration.loadConfiguration(f2);
                        yamlConfiguration1.createSection("Members");
                        yamlConfiguration1.createSection("OP");
                        yamlConfiguration1.createSection("Denys");
                        yamlConfiguration1.createSection("Public");
                        yamlConfiguration1.createSection("Level");
                        yamlConfiguration1.createSection("pvp");
                        yamlConfiguration1.createSection("pickup");
                        yamlConfiguration1.createSection("drop");
                        yamlConfiguration1.createSection("Server");
                        yamlConfiguration1.createSection("locktime");
                        yamlConfiguration1.createSection("lockweather");
                        yamlConfiguration1.createSection("time");
                        if (!yamlConfiguration.contains("NowID")) {
                            yamlConfiguration.set("NowID", 0);
                        }

                        if (!yamlConfiguration.contains("MaxID")) {
                            yamlConfiguration.set("MaxID", 1000);
                        }

                        try {
                            yamlConfiguration.save(Variable.f_log);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        yamlConfiguration1.set("Public", Main.JavaPlugin.getConfig().getBoolean("NormalPublic"));
                        yamlConfiguration1.set("pickup", Main.JavaPlugin.getConfig().getBoolean("NormalPVP"));
                        yamlConfiguration1.set("drop", Main.JavaPlugin.getConfig().getBoolean("NormalPickup"));
                        yamlConfiguration1.set("pvp", Main.JavaPlugin.getConfig().getBoolean("NormalDrop"));
                        yamlConfiguration1.set("locktime", false);
                        yamlConfiguration1.set("time", 0);
                        yamlConfiguration1.set("lockweather", false);
                        int set_level = 1;
                        yamlConfiguration1.set("Level", set_level);
                        yamlConfiguration1.set("Server", Main.JavaPlugin.getConfig().getString("Server"));

                        try {
                            yamlConfiguration1.save(f2);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        yamlConfiguration.set("NowID", nowID + 1);

                        try {
                            yamlConfiguration.save(Variable.f_log);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        yamlConfiguration1.createSection("flowers");
                        yamlConfiguration1.createSection("popularity");
                        yamlConfiguration1.createSection("gifts");
                        yamlConfiguration1.createSection("icon");
                        yamlConfiguration1.createSection("advertisement");
                        yamlConfiguration1.createSection("limitblock");
                        yamlConfiguration1.set("flowers", 0);
                        yamlConfiguration1.set("popularity", 0);
                        yamlConfiguration1.set("gifts", new ArrayList());
                        yamlConfiguration1.set("icon", "");
                        yamlConfiguration1.set("advertisement", new ArrayList());
                        yamlConfiguration1.set("limitblock", new ArrayList());
                        yamlConfiguration1.createSection("X");
                        yamlConfiguration1.createSection("Y");
                        yamlConfiguration1.createSection("Z");
                        Location loc = world.getSpawnLocation();
                        yamlConfiguration1.set("X", loc.getX());
                        yamlConfiguration1.set("Y", loc.getY());
                        yamlConfiguration1.set("Z", loc.getZ());

                        try {
                            yamlConfiguration1.save(f2);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        if (Main.JavaPlugin.getConfig().getInt("MaxSpawnMonstersAmount") != -1) {
                            world.setMonsterSpawnLimit(Main.JavaPlugin.getConfig().getInt("MaxSpawnMonstersAmount"));
                        }

                        if (Main.JavaPlugin.getConfig().getInt("MaxSpawnAnimalsAmount") != -1) {
                            world.setMonsterSpawnLimit(Main.JavaPlugin.getConfig().getInt("MaxSpawnAnimalsAmount"));
                        }

                        /* excluded FirstBorderShaped */
                        sender.sendMessage(Variable.Lang_YML.getString("HeadLineTtitle"));
                        sender.sendMessage(Variable.Lang_YML.getString("AdminCreateHomeForPlayerSuccess"));
                        sender.sendMessage(Variable.Lang_YML.getString("BottomLineTtitle"));
                    }

                    return false;
                }
            }

            if (args.length == 1 && args[0].equalsIgnoreCase("Help")) {
                Bukkit.dispatchCommand(sender, "sh help 1");
                return false;
            }

            if (args.length == 1 && args[0].equalsIgnoreCase("rank")) {
                Bukkit.dispatchCommand(sender, "sh rank 1");
                return false;
            }

            if (!(sender instanceof Player)) {
                sender.sendMessage(Variable.Lang_YML.getString("HeadLineTtitle"));
                sender.sendMessage(Variable.Lang_YML.getString("CommandSenderTip"));
                sender.sendMessage(Variable.Lang_YML.getString("BottomLineTtitle"));
                return false;
            }

            final Player p = (Player)sender;
            if (Util.CheckIllegalName(p)) {
                sender.sendMessage(Variable.Lang_YML.getString("PlayerHasIllegalName"));
                return false;
            }

            if (args.length != 1 || !args[0].equalsIgnoreCase("Open") && !args[0].equalsIgnoreCase("Menu")) {
                if (args.length == 2 && args[0].equalsIgnoreCase("Open") && args[1].equalsIgnoreCase("Main")) {
                    MainGui gui = new MainGui(p);
                    p.openInventory(gui.getInventory());
                    return false;
                }

                if (args.length == 2 && args[0].equalsIgnoreCase("Open") && args[1].equalsIgnoreCase("Check")) {
                    CheckGui gui = new CheckGui(p);
                    p.openInventory(gui.getInventory());
                    return false;
                }

                if (args.length == 2 && args[0].equalsIgnoreCase("Open") && args[1].equalsIgnoreCase("Create")) {
                    CreateGui gui = new CreateGui(p);
                    p.openInventory(gui.getInventory());
                    return false;
                }

                if (args.length == 2 && args[0].equalsIgnoreCase("Open") && args[1].equalsIgnoreCase("Manage")) {
                    ManageGui gui = new ManageGui(p);
                    p.openInventory(gui.getInventory());
                    return false;
                }

                if (args.length == 2 && args[0].equalsIgnoreCase("Open") && args[1].equalsIgnoreCase("Manage2")) {
                    ManageGui2 gui = new ManageGui2(p);
                    p.openInventory(gui.getInventory());
                    return false;
                }

                if (args.length == 2 && args[0].equalsIgnoreCase("Open") && args[1].equalsIgnoreCase("Visit")) {
                    VisitGui gui = new VisitGui();
                    p.openInventory(gui.getInventory());
                    return false;
                }

                if (args.length == 2 && args[0].equalsIgnoreCase("Open") && args[1].equalsIgnoreCase("Invite")) {
                    InviteGui gui = new InviteGui();
                    p.openInventory(gui.getInventory());
                    return false;
                }

                if (args.length == 2 && args[0].equalsIgnoreCase("Open") && args[1].equalsIgnoreCase("Trust")) {
                    TrustGui gui = new TrustGui();
                    p.openInventory(gui.getInventory());
                    return false;
                }

                if (args.length == 2 && args[0].equalsIgnoreCase("Open") && args[1].equalsIgnoreCase("Deny")) {
                    DenyGui gui = new DenyGui();
                    p.openInventory(gui.getInventory());
                    return false;
                }

                if (args.length == 1 && args[0].equalsIgnoreCase("close")) {
                    if (p.getOpenInventory() != null) {
                        p.closeInventory();
                    }

                    return false;
                } else {
                    if (args.length == 2 && args[0].equalsIgnoreCase("Help")) {
                        if (args[1].equalsIgnoreCase("1")) {
                            sender.sendMessage(Variable.Lang_YML.getString("HeadLineTtitle"));

                            for (int i = 0; i < Variable.Lang_YML.getStringList("Help-1").size(); i++) {
                                String[] str = ((String)Variable.Lang_YML.getStringList("Help-1").get(i)).split(",");
                            String cleanCmd = str.length > 1 ? str[1].replaceAll("\u00A7.", "") : "";
                                if (Variable.has_no_click_message) {
                                    p.sendMessage(LegacyComponentSerializer.legacySection().deserialize(str[0]));
                                } else {
                                    Component msg = LegacyComponentSerializer.legacySection().deserialize(str[0]);
                                    if (!str[0].contains("下一")
                                        && !str[0].contains("第一")
                                        && !str[0].contains("First")
                                        && !str[0].contains("Next")
                                        && !str[0].contains("上一")) {
                                        msg = msg.clickEvent(net.kyori.adventure.text.event.ClickEvent.suggestCommand(cleanCmd));
                                    } else {
                                        msg = msg.clickEvent(net.kyori.adventure.text.event.ClickEvent.runCommand(cleanCmd));
                                    }

                                    p.sendMessage(msg);
                                }
                            }

                            sender.sendMessage(Variable.Lang_YML.getString("BottomLineTtitle"));
                            return false;
                        }

                        if (args[1].equalsIgnoreCase("2")) {
                            sender.sendMessage(Variable.Lang_YML.getString("HeadLineTtitle"));

                            for (int i = 0; i < Variable.Lang_YML.getStringList("Help-2").size(); i++) {
                                String[] str = ((String)Variable.Lang_YML.getStringList("Help-2").get(i)).split(",");
                                if (Variable.has_no_click_message) {
                                    p.sendMessage(LegacyComponentSerializer.legacySection().deserialize(str[0]));
                                } else {
                                    TextComponent e1 = new TextComponent(str[0]);
                                    if (!str[0].contains("下一")
                                        && !str[0].contains("第一")
                                        && !str[0].contains("First")
                                        && !str[0].contains("Next")
                                        && !str[0].contains("上一")) {
                                        e1.setClickEvent(new ClickEvent(Action.SUGGEST_COMMAND, str[1]));
                                    } else {
                                        e1.setClickEvent(new ClickEvent(Action.RUN_COMMAND, str[1]));
                                    }

                                    p.spigot().sendMessage(e1);
                                }
                            }

                            sender.sendMessage(Variable.Lang_YML.getString("BottomLineTtitle"));
                            return false;
                        }

                        if (args[1].equalsIgnoreCase("3")) {
                            sender.sendMessage(Variable.Lang_YML.getString("HeadLineTtitle"));

                            for (int i = 0; i < Variable.Lang_YML.getStringList("Help-3").size(); i++) {
                                String[] str = ((String)Variable.Lang_YML.getStringList("Help-3").get(i)).split(",");
                                if (Variable.has_no_click_message) {
                                    p.sendMessage(LegacyComponentSerializer.legacySection().deserialize(str[0]));
                                } else {
                                    TextComponent e1 = new TextComponent(str[0]);
                                    if (!str[0].contains("下一")
                                        && !str[0].contains("第一")
                                        && !str[0].contains("Next")
                                        && !str[0].contains("First")
                                        && !str[0].contains("上一")) {
                                        e1.setClickEvent(new ClickEvent(Action.SUGGEST_COMMAND, str[1]));
                                    } else {
                                        e1.setClickEvent(new ClickEvent(Action.RUN_COMMAND, str[1]));
                                    }

                                    p.spigot().sendMessage(e1);
                                }
                            }

                            sender.sendMessage(Variable.Lang_YML.getString("BottomLineTtitle"));
                            return false;
                        }

                        if (args[1].equalsIgnoreCase("4")) {
                            sender.sendMessage(Variable.Lang_YML.getString("HeadLineTtitle"));

                            for (int i = 0; i < Variable.Lang_YML.getStringList("Help-4").size(); i++) {
                                String[] str = ((String)Variable.Lang_YML.getStringList("Help-4").get(i)).split(",");
                                if (Variable.has_no_click_message) {
                                    p.sendMessage(LegacyComponentSerializer.legacySection().deserialize(str[0]));
                                } else {
                                    TextComponent e1 = new TextComponent(str[0]);
                                    if (!str[0].contains("下一")
                                        && !str[0].contains("第一")
                                        && !str[0].contains("Next")
                                        && !str[0].contains("First")
                                        && !str[0].contains("上一")) {
                                        e1.setClickEvent(new ClickEvent(Action.SUGGEST_COMMAND, str[1]));
                                    } else {
                                        e1.setClickEvent(new ClickEvent(Action.RUN_COMMAND, str[1]));
                                    }

                                    p.spigot().sendMessage(e1);
                                }
                            }

                            sender.sendMessage(Variable.Lang_YML.getString("BottomLineTtitle"));
                            return false;
                        }

                        if (args[1].equalsIgnoreCase("5")) {
                            sender.sendMessage(Variable.Lang_YML.getString("HeadLineTtitle"));

                            for (int i = 0; i < Variable.Lang_YML.getStringList("Help-5").size(); i++) {
                                String[] str = ((String)Variable.Lang_YML.getStringList("Help-5").get(i)).split(",");
                                if (Variable.has_no_click_message) {
                                    p.sendMessage(LegacyComponentSerializer.legacySection().deserialize(str[0]));
                                } else {
                                    TextComponent e1 = new TextComponent(str[0]);
                                    if (!str[0].contains("下一")
                                        && !str[0].contains("第一")
                                        && !str[0].contains("Next")
                                        && !str[0].contains("First")
                                        && !str[0].contains("上一")) {
                                        e1.setClickEvent(new ClickEvent(Action.SUGGEST_COMMAND, str[1]));
                                    } else {
                                        e1.setClickEvent(new ClickEvent(Action.RUN_COMMAND, str[1]));
                                    }

                                    p.spigot().sendMessage(e1);
                                }
                            }

                            sender.sendMessage(Variable.Lang_YML.getString("BottomLineTtitle"));
                            return false;
                        }

                        if (args[1].equalsIgnoreCase("6")) {
                            sender.sendMessage(Variable.Lang_YML.getString("HeadLineTtitle"));

                            for (int i = 0; i < Variable.Lang_YML.getStringList("Help-6").size(); i++) {
                                String[] str = ((String)Variable.Lang_YML.getStringList("Help-6").get(i)).split(",");
                                if (Variable.has_no_click_message) {
                                    p.sendMessage(LegacyComponentSerializer.legacySection().deserialize(str[0]));
                                } else {
                                    TextComponent e1 = new TextComponent(str[0]);
                                    if (!str[0].contains("下一")
                                        && !str[0].contains("第一")
                                        && !str[0].contains("Next")
                                        && !str[0].contains("First")
                                        && !str[0].contains("上一")) {
                                        e1.setClickEvent(new ClickEvent(Action.SUGGEST_COMMAND, str[1]));
                                    } else {
                                        e1.setClickEvent(new ClickEvent(Action.RUN_COMMAND, str[1]));
                                    }

                                    p.spigot().sendMessage(e1);
                                }
                            }

                            sender.sendMessage(Variable.Lang_YML.getString("BottomLineTtitle"));
                            return false;
                        }
                    }

                    if (args.length == 1 && args[0].equalsIgnoreCase("check")) {
                        Player p5 = null;
                        if (sender instanceof Player) {
                            p5 = (Player)sender;
                        }

                        if (!Main.JavaPlugin.getConfig().getBoolean("Permission.CommandUser")
                            && !p.hasPermission("SelfHome.check")
                            && !p.hasPermission("SelfHome.command.user")) {
                            String tip = Variable.Lang_YML.getString("NoPermissionCheck");
                            if (tip.contains("<Permission>")) {
                                tip = tip.replace("<Permission>", "SelfHome.Check");
                            }

                            p.sendMessage(tip);
                            return false;
                        } else {
                            (new BukkitRunnable() {
                                    Player p5;

                                    public void run() {
                                        File folder = new File(Variable.Tempf);
                                        sender.sendMessage(Variable.Lang_YML.getString("CheckListTitle"));
                                        if (Variable.bungee) {
                                            for (String e : MySQL.CheckHasPermission(p.getName())) {
                                                if (Variable.has_no_click_message) {
                                                    p.sendMessage("§e" + e + Variable.Lang_YML.getString("CheckSuffix"));
                                                } else {
                                                    TextComponent Click_Message = new TextComponent("§e" + e + Variable.Lang_YML.getString("CheckSuffix"));
                                                    Click_Message.setClickEvent(new ClickEvent(Action.RUN_COMMAND, "/sh v " + e));
                                                    p.spigot().sendMessage(Click_Message);
                                                }
                                            }
                                        } else {
                                            File[] arrayOfFile;
                                            int j = (arrayOfFile = folder.listFiles()).length;

                                            for (int b = 0; b < j; b++) {
                                                File temp = arrayOfFile[b];
                                                String want_to = temp.getPath()
                                                    .replace(Variable.Tempf, "")
                                                    .replace(".yml", "")
                                                    .replace(Variable.file_loc_prefix, "");
                                                YamlConfiguration yamlConfiguration = YamlConfiguration.loadConfiguration(temp);

                                                for (int i = 0; i < yamlConfiguration.getStringList("Members").size(); i++) {
                                                    String temp_str = (String)yamlConfiguration.getStringList("Members").get(i);
                                                    if (temp_str.equalsIgnoreCase(this.p5.getPlayer().getName()) || temp_str.equals("*")) {
                                                        if (Variable.has_no_click_message) {
                                                            p.sendMessage("§e" + want_to + Variable.Lang_YML.getString("CheckSuffix"));
                                                        } else {
                                                            TextComponent Click_Message = new TextComponent(
                                                                "§e" + want_to + Variable.Lang_YML.getString("CheckSuffix")
                                                            );
                                                            Click_Message.setClickEvent(new ClickEvent(Action.RUN_COMMAND, "/sh v " + want_to));
                                                            p.spigot().sendMessage(Click_Message);
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        sender.sendMessage(Variable.Lang_YML.getString("CheckListEnd"));
                                    }
                                })
                                .runTask(Main.JavaPlugin);
                            return false;
                        }
                    } else if (args.length == 1 && args[0].equalsIgnoreCase("setSpawn")) {
                        if (!Main.JavaPlugin.getConfig().getBoolean("Permission.SetSpawn") && !p.hasPermission("SelfHome.SetSpawn")) {
                            String tip = Variable.Lang_YML.getString("NoPermissionCheck");
                            if (tip.contains("<Permission>")) {
                                tip = tip.replace("<Permission>", "SelfHome.SetSpawn");
                            }

                            p.sendMessage(tip);
                            return false;
                        } else {
                            if (Util.CheckOwnerAndManagerAndOP(p, p.getWorld().getName().replace(Variable.world_prefix, ""))) {
                                if (!Bukkit.getVersion().contains("1.7.10") && !Bukkit.getVersion().contains("1.7.2")) {
                                    p.getWorld().setSpawnLocation(p.getLocation());
                                } else {
                                    p.getWorld().setSpawnLocation((int)p.getLocation().getX(), (int)p.getLocation().getY(), (int)p.getLocation().getZ());
                                }

                                if (Main.JavaPlugin.getConfig().getBoolean("BorderSwitch")) {
                                    try {
                                        World world = Bukkit.getWorld(Variable.world_prefix + p.getWorld().getName().replace(Variable.world_prefix, ""));
                                        world.getWorldBorder().setCenter(p.getLocation());
                                        world.getWorldBorder().setSize(world.getWorldBorder().getSize());
                                    } catch (NoSuchMethodError e) {
                                        Bukkit.getConsoleSender().sendMessage(Variable.Lang_YML.getString("BorderException"));
                                    }
                                }

                                String temp = Variable.Lang_YML.getString("SetSpawnSuccess");
                                sender.sendMessage(Variable.Lang_YML.getString("HeadLineTtitle"));
                                sender.sendMessage(temp);
                                sender.sendMessage(Variable.Lang_YML.getString("BottomLineTtitle"));
                            } else {
                                String temp = Variable.Lang_YML.getString("NoOwnerAndManagerPermission");
                                sender.sendMessage(Variable.Lang_YML.getString("HeadLineTtitle"));
                                sender.sendMessage(temp);
                                sender.sendMessage(Variable.Lang_YML.getString("BottomLineTtitle"));
                            }

                            return false;
                        }
                    } else if (args.length == 1 && args[0].equalsIgnoreCase("mobs")) {
                        Player p3 = (Player)sender;
                        if (p3.isOp()) {
                            String result = "";

                            for (Entity entity : p3.getWorld().getEntities()) {
                                if (entity instanceof LivingEntity) {
                                    result = String.valueOf(result) + " " + entity.getType().toString();
                                }
                            }

                            p3.sendMessage(result);
                        } else {
                            String temp = Variable.Lang_YML.getString("AdminCommand");
                            sender.sendMessage(Variable.Lang_YML.getString("HeadLineTtitle"));
                            sender.sendMessage(temp);
                            sender.sendMessage(Variable.Lang_YML.getString("BottomLineTtitle"));
                        }

                        return false;
                    } else if (args.length == 2 && args[0].equalsIgnoreCase("setBiome")) {
                        if (sender instanceof Player) {
                            Player p3 = (Player)sender;
                            if (p3.hasPermission("SelfHome.Biome")) {
                                World world = p3.getWorld();
                                Chunk chunk = p3.getWorld().getChunkAt(p3.getLocation());

                                try {
                                    for (int x = 0; x < 16; x++) {
                                        for (int z = 0; z < 16; z++) {
                                            for (int y = 0; y < 255; y++) {
                                                Block block = chunk.getBlock(x, y, z);
                                                block.setBiome(Biome.valueOf(args[1].toUpperCase()));
                                            }
                                        }
                                    }
                                } catch (IllegalArgumentException e) {
                                    p.sendMessage(Variable.Lang_YML.getString("BiomeError"));
                                    return false;
                                }

                                World tworld = p3.getWorld();

                                for (Player t : tworld.getPlayers()) {
                                    t.sendMessage(Variable.Lang_YML.getString("BiomeChangeTip"));
                                }
                            } else {
                                String Language = Variable.Lang_YML.getString("NoPermissionCheck");
                                if (Language.contains("<Permission>")) {
                                    Language = Language.replace("<Permission>", "SelfHome.Biome");
                                }

                                if (!p.hasPermission("SelfHome.Biome")) {
                                    sender.sendMessage(Variable.Lang_YML.getString("HeadLineTtitle"));
                                    sender.sendMessage(Language);
                                    sender.sendMessage(Variable.Lang_YML.getString("BottomLineTtitle"));
                                    return false;
                                }
                            }
                        }

                        return false;
                    } else if (args.length == 1 && args[0].equalsIgnoreCase("update")) {
                        if (!Main.JavaPlugin.getConfig().getBoolean("Permission.CommandUser")
                            && !p.hasPermission("SelfHome.Update")
                            && !p.hasPermission("SelfHome.command.user")) {
                            String tip = Variable.Lang_YML.getString("NoPermissionCheck");
                            if (tip.contains("<Permission>")) {
                                tip = tip.replace("<Permission>", "SelfHome.Update");
                            }

                            p.sendMessage(tip);
                            return false;
                        } else {
                            if (!Util.CheckIsHome(p.getWorld().getName().replace(Variable.world_prefix, ""))) {
                                String tip = Variable.Lang_YML.getString("NowIsNotHome");
                                p.sendMessage(tip);
                                return false;
                            }

                            if (Util.CheckOwnerAndManagerAndOP(p, p.getWorld().getName().replace(Variable.world_prefix, ""))) {
                                File f = new File(Variable.Tempf, String.valueOf(p.getWorld().getName().replace(Variable.world_prefix, "")) + ".yml");
                                if (Variable.bungee) {
                                    Integer Now = Integer.valueOf(MySQL.getLevel(p.getWorld().getName().replace(Variable.world_prefix, "")));
                                    if (Now >= Main.JavaPlugin.getConfig().getInt("MaxLevel")) {
                                        String temp = Variable.Lang_YML.getString("ReachMaxLevel");
                                        if (temp.contains("<Level>")) {
                                            temp = temp.replace("<Level>", String.valueOf(Now));
                                        }

                                        sender.sendMessage(Variable.Lang_YML.getString("HeadLineTtitle"));
                                        sender.sendMessage(temp);
                                        sender.sendMessage(Variable.Lang_YML.getString("BottomLineTtitle"));
                                    } else {
                                        double GetMoney = Variable.econ.getBalance(p);
                                        if (GetMoney >= (Double)Main.JavaPlugin.getConfig().getDoubleList("MoneyNeed").get(Now - 1)) {
                                            if (Variable.PlyaerPointsModule) {
                                                Integer GetPoints = Variable.playerPoints.getAPI().look(p.getUniqueId());
                                                if (GetPoints < (Integer)Main.JavaPlugin.getConfig().getIntegerList("PointsNeed").get(Now - 1)) {
                                                    String temp = Variable.Lang_YML.getString("UpdateNoPoints");
                                                    if (temp.contains("<NeedPoints>")) {
                                                        temp = temp.replace(
                                                            "<NeedPoints>",
                                                            String.valueOf(Main.JavaPlugin.getConfig().getIntegerList("PointsNeed").get(Now - 1))
                                                        );
                                                    }

                                                    sender.sendMessage(Variable.Lang_YML.getString("HeadLineTtitle"));
                                                    sender.sendMessage(temp);
                                                    sender.sendMessage(Variable.Lang_YML.getString("BottomLineTtitle"));
                                                    return false;
                                                }
                                            }

                                            if (!((String)Main.JavaPlugin.getConfig().getStringList("ItemsNeed").get(Now - 1)).equalsIgnoreCase("")) {
                                                String[] temp = ((String)Main.JavaPlugin.getConfig().getStringList("ItemsNeed").get(Now - 1)).split(",");
                                                ItemStack i = new ItemStack(Material.valueOf(temp[0]));
                                                i.setAmount(Integer.valueOf(temp[1]));
                                                if (!p.getInventory().containsAtLeast(i, i.getAmount())) {
                                                    String lang = Variable.Lang_YML.getString("UpdateNoEnoughItems");
                                                    if (lang.contains("<Amount>")) {
                                                        lang = lang.replace("<Amount>", String.valueOf(i.getAmount()));
                                                    }

                                                    if (lang.contains("<Item>")) {
                                                        lang = lang.replace("<Item>", String.valueOf(i.getType().toString()));
                                                    }

                                                    sender.sendMessage(Variable.Lang_YML.getString("HeadLineTtitle"));
                                                    p.sendMessage(lang);
                                                    sender.sendMessage(Variable.Lang_YML.getString("BottomLineTtitle"));
                                                    return false;
                                                }

                                                int amount = i.getAmount();

                                                for (int e = 0; e < p.getInventory().getSize(); e++) {
                                                    if (p.getInventory().getItem(e) != null) {
                                                        ItemStack i_temp = p.getInventory().getItem(e);
                                                        if (i_temp.getType() == i.getType()) {
                                                            if (i_temp.getAmount() > amount) {
                                                                ItemStack clone = i_temp.clone();
                                                                clone.setAmount(i_temp.getAmount() - amount);
                                                                p.getInventory().setItem(e, clone);
                                                                break;
                                                            }

                                                            amount -= i_temp.getAmount();
                                                            p.getInventory().setItem(e, null);
                                                        }
                                                    }
                                                }
                                            }

                                            if (Variable.PlyaerPointsModule) {
                                                Variable.playerPoints
                                                    .getAPI()
                                                    .take(p.getUniqueId(), (Integer)Main.JavaPlugin.getConfig().getIntegerList("PointsNeed").get(Now - 1));
                                            }

                                            Variable.econ.withdrawPlayer(p, (Double)Main.JavaPlugin.getConfig().getDoubleList("MoneyNeed").get(Now - 1));
                                            MySQL.setLevel(p.getWorld().getName().replace(Variable.world_prefix, ""), String.valueOf(Now + 1));
                                            if (Variable.hook_FastAsyncWorldEdit
                                                && Main.JavaPlugin.getConfig().getBoolean("FaweSwitch")
                                                && Main.JavaPlugin.getConfig().getBoolean("UpdateClearOld")) {
                                                /* excluded FirstBorderShaped */
                                            }

                                            sender.sendMessage(Variable.Lang_YML.getString("HeadLineTtitle"));
                                            String temp = Variable.Lang_YML.getString("UpdateToNewLevel");
                                            if (temp.contains("<Level>")) {
                                                temp = temp.replace(
                                                    "<Level>", String.valueOf(MySQL.getLevel(p.getWorld().getName().replace(Variable.world_prefix, "")))
                                                );
                                            }

                                            sender.sendMessage(temp);
                                            sender.sendMessage(Variable.Lang_YML.getString("BottomLineTtitle"));
                                            if (Main.JavaPlugin.getConfig().getBoolean("BorderSwitch")) {
                                                try {
                                                    World world = Bukkit.getWorld(Variable.world_prefix + p.getName());
                                                    world.getWorldBorder().setCenter(world.getSpawnLocation());
                                                    world.getWorldBorder()
                                                        .setSize(world.getWorldBorder().getSize() + Main.JavaPlugin.getConfig().getInt("UpdateRadius"));
                                                } catch (Exception e) {
                                                    Bukkit.getConsoleSender().sendMessage(Variable.Lang_YML.getString("BorderException"));
                                                }
                                            }
                                        } else {
                                            String temp = Variable.Lang_YML.getString("UpdateNoEnoughMoney");
                                            if (temp.contains("<NeedMoney>")) {
                                                temp = temp.replace(
                                                    "<NeedMoney>", String.valueOf(Main.JavaPlugin.getConfig().getDoubleList("MoneyNeed").get(Now - 1))
                                                );
                                            }

                                            sender.sendMessage(Variable.Lang_YML.getString("HeadLineTtitle"));
                                            sender.sendMessage(temp);
                                            sender.sendMessage(Variable.Lang_YML.getString("BottomLineTtitle"));
                                        }
                                    }

                                    Util.refreshBorder(p.getWorld());
                                } else {
                                    YamlConfiguration yamlConfiguration = YamlConfiguration.loadConfiguration(f);
                                    Integer Now = yamlConfiguration.getInt("Level");
                                    if (Now >= Main.JavaPlugin.getConfig().getInt("MaxLevel")) {
                                        String temp = Variable.Lang_YML.getString("ReachMaxLevel");
                                        if (temp.contains("<Level>")) {
                                            temp = temp.replace("<Level>", String.valueOf(yamlConfiguration.getInt("Level")));
                                        }

                                        sender.sendMessage(Variable.Lang_YML.getString("HeadLineTtitle"));
                                        sender.sendMessage(temp);
                                        sender.sendMessage(Variable.Lang_YML.getString("BottomLineTtitle"));
                                    } else {
                                        double GetMoney = Variable.econ.getBalance(p);
                                        if (GetMoney >= (Double)Main.JavaPlugin.getConfig().getDoubleList("MoneyNeed").get(Now - 1)) {
                                            if (Variable.PlyaerPointsModule) {
                                                Integer GetPoints = Variable.playerPoints.getAPI().look(p.getUniqueId());
                                                if (GetPoints < (Integer)Main.JavaPlugin.getConfig().getIntegerList("PointsNeed").get(Now - 1)) {
                                                    String temp = Variable.Lang_YML.getString("UpdateNoPoints");
                                                    if (temp.contains("<NeedPoints>")) {
                                                        temp = temp.replace(
                                                            "<NeedPoints>",
                                                            String.valueOf(Main.JavaPlugin.getConfig().getIntegerList("PointsNeed").get(Now - 1))
                                                        );
                                                    }

                                                    sender.sendMessage(Variable.Lang_YML.getString("HeadLineTtitle"));
                                                    sender.sendMessage(temp);
                                                    sender.sendMessage(Variable.Lang_YML.getString("BottomLineTtitle"));
                                                    return false;
                                                }
                                            }

                                            if (!((String)Main.JavaPlugin.getConfig().getStringList("ItemsNeed").get(Now - 1)).equalsIgnoreCase("")) {
                                                String[] temp = ((String)Main.JavaPlugin.getConfig().getStringList("ItemsNeed").get(Now - 1)).split(",");
                                                ItemStack i = new ItemStack(Material.valueOf(temp[0]));
                                                i.setAmount(Integer.valueOf(temp[1]));
                                                if (!p.getInventory().containsAtLeast(i, i.getAmount())) {
                                                    String lang = Variable.Lang_YML.getString("UpdateNoEnoughItems");
                                                    if (lang.contains("<Amount>")) {
                                                        lang = lang.replace("<Amount>", String.valueOf(i.getAmount()));
                                                    }

                                                    if (lang.contains("<Item>")) {
                                                        lang = lang.replace("<Item>", String.valueOf(i.getType().toString()));
                                                    }

                                                    sender.sendMessage(Variable.Lang_YML.getString("HeadLineTtitle"));
                                                    p.sendMessage(lang);
                                                    sender.sendMessage(Variable.Lang_YML.getString("BottomLineTtitle"));
                                                    return false;
                                                }

                                                int amount = i.getAmount();

                                                for (int e = 0; e < p.getInventory().getSize(); e++) {
                                                    if (p.getInventory().getItem(e) != null) {
                                                        ItemStack i_temp = p.getInventory().getItem(e);
                                                        if (i_temp.getType() == i.getType()) {
                                                            if (i_temp.getAmount() > amount) {
                                                                ItemStack clone = i_temp.clone();
                                                                clone.setAmount(i_temp.getAmount() - amount);
                                                                p.getInventory().setItem(e, clone);
                                                                break;
                                                            }

                                                            amount -= i_temp.getAmount();
                                                            p.getInventory().setItem(e, null);
                                                        }
                                                    }
                                                }
                                            }

                                            if (Variable.PlyaerPointsModule) {
                                                Variable.playerPoints
                                                    .getAPI()
                                                    .take(p.getUniqueId(), (Integer)Main.JavaPlugin.getConfig().getIntegerList("PointsNeed").get(Now - 1));
                                            }

                                            Variable.econ.withdrawPlayer(p, (Double)Main.JavaPlugin.getConfig().getDoubleList("MoneyNeed").get(Now - 1));
                                            yamlConfiguration.set("Level", yamlConfiguration.getInt("Level") + 1);

                                            try {
                                                yamlConfiguration.save(f);
                                            } catch (Exception e) {
                                                e.printStackTrace();
                                            }

                                            yamlConfiguration = YamlConfiguration.loadConfiguration(f);

                                            for (int c = 0; c < Main.JavaPlugin.getConfig().getStringList("AfterUpdateDispathCommand").size(); c++) {
                                                String temp1 = (String)Main.JavaPlugin.getConfig().getStringList("DispathCommand").get(c);
                                                if (temp1.contains("<Name>")) {
                                                    temp1 = temp1.replace("<Name>", p.getName());
                                                }

                                                if (temp1.contains("[console]")) {
                                                    temp1 = temp1.replace("[console]", "");
                                                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), temp1);
                                                } else if (temp1.contains("[player]")) {
                                                    temp1 = temp1.replace("[player]", "");
                                                    Bukkit.dispatchCommand(p, temp1);
                                                }
                                            }

                                            sender.sendMessage(Variable.Lang_YML.getString("HeadLineTtitle"));
                                            String temp = Variable.Lang_YML.getString("UpdateToNewLevel");
                                            if (temp.contains("<Level>")) {
                                                temp = temp.replace("<Level>", String.valueOf(yamlConfiguration.getInt("Level")));
                                            }

                                            sender.sendMessage(temp);
                                            sender.sendMessage(Variable.Lang_YML.getString("BottomLineTtitle"));
                                            /* excluded FirstBorderShaped */
                                            if (Main.JavaPlugin.getConfig().getBoolean("BorderSwitch")) {
                                                try {
                                                    World world = Bukkit.getWorld(Variable.world_prefix + p.getName());
                                                    world.getWorldBorder().setCenter(world.getSpawnLocation());
                                                    world.getWorldBorder()
                                                        .setSize(world.getWorldBorder().getSize() + Main.JavaPlugin.getConfig().getInt("UpdateRadius"));
                                                } catch (Exception e) {
                                                    Bukkit.getConsoleSender().sendMessage(Variable.Lang_YML.getString("BorderException"));
                                                }
                                            }
                                        } else {
                                            String temp = Variable.Lang_YML.getString("UpdateNoEnoughMoney");
                                            if (temp.contains("<NeedMoney>")) {
                                                temp = temp.replace(
                                                    "<NeedMoney>", String.valueOf(Main.JavaPlugin.getConfig().getDoubleList("MoneyNeed").get(Now - 1))
                                                );
                                            }

                                            sender.sendMessage(Variable.Lang_YML.getString("HeadLineTtitle"));
                                            sender.sendMessage(temp);
                                            sender.sendMessage(Variable.Lang_YML.getString("BottomLineTtitle"));
                                        }
                                    }

                                    Util.refreshBorder(p.getWorld());
                                }
                            } else {
                                String temp = Variable.Lang_YML.getString("NoOwnerAndManagerPermission");
                                sender.sendMessage(Variable.Lang_YML.getString("HeadLineTtitle"));
                                sender.sendMessage(temp);
                                sender.sendMessage(Variable.Lang_YML.getString("BottomLineTtitle"));
                            }

                            return false;
                        }
                    } else if (args.length == 1 && args[0].equalsIgnoreCase("look")) {
                        if (!Main.JavaPlugin.getConfig().getBoolean("Permission.CommandUser")
                            && !p.hasPermission("SelfHome.Look")
                            && !p.hasPermission("SelfHome.command.user")) {
                            String tip = Variable.Lang_YML.getString("NoPermissionCheck");
                            if (tip.contains("<Permission>")) {
                                tip = tip.replace("<Permission>", "SelfHome.Look");
                            }

                            p.sendMessage(tip);
                            return false;
                        } else {
                            if (!Util.CheckIsHome(p.getWorld().getName().replace(Variable.world_prefix, ""))) {
                                String tip = Variable.Lang_YML.getString("NowIsNotHome");
                                p.sendMessage(tip);
                                return false;
                            }

                            sender.sendMessage(Variable.Lang_YML.getString("HeadLineTtitle"));

                            for (int i = 0; i < Variable.Lang_YML.getStringList("LookInfo").size(); i++) {
                                String temp = (String)Variable.Lang_YML.getStringList("LookInfo").get(i);
                                temp = PlaceholderAPI.setPlaceholders(p, temp);
                                sender.sendMessage(temp);
                            }

                            sender.sendMessage(Variable.Lang_YML.getString("BottomLineTtitle"));
                            return false;
                        }
                    } else if (args.length == 1 && args[0].equalsIgnoreCase("calc")) {
                        if (!p.hasPermission("SelfHome.Calc")) {
                            String tip = Variable.Lang_YML.getString("NoPermissionCheck");
                            if (tip.contains("<Permission>")) {
                                tip = tip.replace("<Permission>", "SelfHome.Calc");
                            }

                            p.sendMessage(tip);
                            return false;
                        } else {
                            if (Variable.calc_cooldown.contains(p.getName())) {
                                p.sendMessage("Cooldown ing... waif for one minute!!!");
                                return false;
                            }

                            Variable.calc_cooldown.add(p.getName());
                            (new BukkitRunnable() {
                                public void run() {
                                    if (Variable.calc_cooldown.contains(p.getName())) {
                                        Variable.calc_cooldown.remove(p.getName());
                                    }
                                }
                            }).runTaskLater(Main.JavaPlugin, 1200L);
                            if (!Util.CheckIsHome(p.getWorld().getName().replace(Variable.world_prefix, ""))) {
                                String tip = Variable.Lang_YML.getString("NowIsNotHome");
                                p.sendMessage(tip);
                                return false;
                            }

                            World world = p.getWorld();
                            TreeMap<String, Integer> sorted_map = new TreeMap<>(Collections.reverseOrder());
                            Chunk[] var833;
                            int var746 = (var833 = world.getLoadedChunks()).length;

                            for (int var655 = 0; var655 < var746; var655++) {
                                Chunk chunk = var833[var655];
                                BlockState[] var1041;
                                int var1018 = (var1041 = chunk.getTileEntities()).length;

                                for (int var975 = 0; var975 < var1018; var975++) {
                                    BlockState tile = var1041[var975];
                                    String name = tile.getBlock().getType().toString();
                                    if (sorted_map.containsKey(name)) {
                                        sorted_map.put(name, sorted_map.get(name) + 1);
                                    } else {
                                        sorted_map.put(name, 1);
                                    }
                                }
                            }

                            String temp = "";

                            for (String key : sorted_map.keySet()) {
                                temp = temp + key + ":" + sorted_map.get(key) + " , ";
                            }

                            sender.sendMessage(temp);
                            return false;
                        }
                    } else if (args.length == 1 && args[0].equalsIgnoreCase("nbt")) {
                        if (p.isOp()) {
                            if (Variable.Debug.contains(p.getName())) {
                                Variable.Debug.remove(p.getName());
                                sender.sendMessage(Variable.Lang_YML.getString("DisableNBTDebug"));
                            } else {
                                Variable.Debug.add(p.getName());
                                sender.sendMessage(Variable.Lang_YML.getString("EnableNBTDebug"));
                            }

                            return false;
                        } else {
                            sender.sendMessage(Variable.Lang_YML.getString("PlayerIsNotOperator"));
                            return false;
                        }
                    } else if (args.length == 1 && args[0].equalsIgnoreCase("item")) {
                        if (sender instanceof Player) {
                            Player temp = (Player)sender;
                            if (!temp.isOp()) {
                                sender.sendMessage(Variable.Lang_YML.getString("PlayerIsNotOperator"));
                                return false;
                            }

                            temp = (Player)sender;
                            if (temp.getItemInHand() == null) {
                                temp.sendMessage("§enull");
                            } else if (temp.getItemInHand().getType() == Material.AIR) {
                                temp.sendMessage("§eAIR");
                            } else if (Variable.has_no_click_message) {
                                temp.sendMessage("§e" + Util.getItemNBTString(temp.getItemInHand()));
                            } else {
                                TextComponent Send_Block_Message = new TextComponent("§e" + Util.getItemNBTString(temp.getItemInHand()) + " §b>> §dCopy");
                                Send_Block_Message.setClickEvent(new ClickEvent(Action.SUGGEST_COMMAND, Util.getItemNBTString(temp.getItemInHand())));
                                temp.spigot().sendMessage(Send_Block_Message);
                            }

                            return false;
                        } else {
                            sender.sendMessage(Variable.Lang_YML.getString("CommandSenderIsNotAllowToUseTheCommand"));
                            return false;
                        }
                    } else if (args.length == 1 && args[0].equalsIgnoreCase("item")) {
                        if (p.isOp()) {
                            ItemStack i = p.getItemInHand();
                            p.sendMessage("§e§l§m--------------§7[§eDeBug§7]§e§l§m--------------");
                            if (Variable.has_no_click_message) {
                                p.sendMessage("§eMaterial:§d" + i.getType().toString() + "§e,SubID:§d" + i.getDurability());
                            } else {
                                TextComponent Send_Block_Message = new TextComponent(
                                    "§eMaterial:§d" + i.getType().toString() + "§e,SubID:§d" + i.getDurability() + " §b>> §dCopy"
                                );
                                Send_Block_Message.setClickEvent(
                                    new ClickEvent(Action.SUGGEST_COMMAND, "Material:" + i.getType().toString() + ",SubID:" + i.getDurability())
                                );
                                p.spigot().sendMessage(Send_Block_Message);
                            }

                            p.sendMessage("§e§l§m--------------§7[§eDebug§7]§e§l§m--------------");
                        } else {
                            p.sendMessage(Variable.Lang_YML.getString("PlayerIsNotOperator"));
                        }

                        return false;
                    } else if (args.length == 1 && args[0].equalsIgnoreCase("wholeDelete")) {
                        if (!Main.JavaPlugin.getConfig().getBoolean("Permission.CommandUser")
                            && !p.hasPermission("SelfHome.WholeDelete")
                            && !p.hasPermission("SelfHome.command.user")) {
                            String tip = Variable.Lang_YML.getString("NoPermissionCheck");
                            if (tip.contains("<Permission>")) {
                                tip = tip.replace("<Permission>", "SelfHome.WholeDelete");
                            }

                            p.sendMessage(tip);
                            return false;
                        } else {
                            if (!Util.CheckIsHome(p.getLocation().getWorld().getName().replace(Variable.world_prefix, ""))) {
                                String temp = Variable.Lang_YML.getString("NowIsNotHome");
                                sender.sendMessage(Variable.Lang_YML.getString("HeadLineTtitle"));
                                sender.sendMessage(temp);
                                sender.sendMessage(Variable.Lang_YML.getString("BottomLineTtitle"));
                                return false;
                            }

                            if (!p.getWorld().getName().replace(Variable.world_prefix, "").equalsIgnoreCase(p.getName())) {
                                String temp = Variable.Lang_YML.getString("DeleteNotIsMyHome");
                                sender.sendMessage(Variable.Lang_YML.getString("HeadLineTtitle"));
                                sender.sendMessage(temp);
                                sender.sendMessage(Variable.Lang_YML.getString("BottomLineTtitle"));
                                return false;
                            }

                            boolean real_delete = false;

                            for (int d = 0; d < Variable.waitDeleteconfirm.size(); d++) {
                                if (Variable.waitDeleteconfirm.get(d).equalsIgnoreCase(p.getName())) {
                                    real_delete = true;
                                }
                            }

                            if (!real_delete && Main.JavaPlugin.getConfig().getBoolean("EnableConfirmDelete")) {
                                if (Variable.has_no_click_message) {
                                    p.sendMessage(Variable.Lang_YML.getString("ConfirmDelete"));
                                } else {
                                    TextComponent Click_Message = new TextComponent(Variable.Lang_YML.getString("ConfirmDelete"));
                                    Click_Message.setClickEvent(new ClickEvent(Action.RUN_COMMAND, "/sh wholedelete"));
                                    p.spigot().sendMessage(Click_Message);
                                }

                                Variable.waitDeleteconfirm.add(p.getName());
                                (new BukkitRunnable() {
                                    public void run() {
                                        for (int i = 0; i < Variable.waitDeleteconfirm.size(); i++) {
                                            if (Variable.waitDeleteconfirm.get(i).equalsIgnoreCase(p.getName())) {
                                                Variable.waitDeleteconfirm.remove(p.getName());
                                            }
                                        }
                                    }
                                }).runTaskLater(Main.JavaPlugin, 100L);
                                return false;
                            } else {
                                Variable.waitDeleteconfirm.remove(p);
                                YamlConfiguration yamlConfiguration = YamlConfiguration.loadConfiguration(Variable.f_log);
                                List<String> list = yamlConfiguration.getStringList("DeleteTimes");
                                if (list == null) {
                                    list = new ArrayList<>();
                                    yamlConfiguration.set("DeleteTimes", list);

                                    try {
                                        yamlConfiguration.save(Variable.f_log);
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }

                                boolean check_contain = false;

                                for (int c = 0; c < list.size(); c++) {
                                    String[] temp3 = list.get(c).split(",");
                                    String name = temp3[0];
                                    if (name.equalsIgnoreCase(p.getName())) {
                                        check_contain = true;
                                        int cs = Integer.valueOf(temp3[1]);
                                        if (cs >= Main.JavaPlugin.getConfig().getInt("MaxDelete")) {
                                            String temp5 = Variable.Lang_YML.getString("MaxDeleteLanguage");
                                            sender.sendMessage(Variable.Lang_YML.getString("HeadLineTtitle"));
                                            sender.sendMessage(temp5);
                                            sender.sendMessage(Variable.Lang_YML.getString("BottomLineTtitle"));
                                            return false;
                                        }

                                        boolean cooldown_check = false;

                                        for (int i = 0; i < Variable.Deletecooldown.size(); i++) {
                                            if (Variable.Deletecooldown.get(i).equalsIgnoreCase(p.getName())) {
                                                cooldown_check = true;
                                            }
                                        }

                                        if (cooldown_check) {
                                            p.sendMessage(Variable.Lang_YML.getString("IsDeleteCooldown"));
                                            return false;
                                        }

                                        Variable.Deletecooldown.add(p.getName());
                                        (new BukkitRunnable() {
                                            public void run() {
                                                if (Variable.Deletecooldown.contains(p.getName())) {
                                                    Variable.Deletecooldown.remove(p.getName());
                                                    String temp = Variable.Lang_YML.getString("DeleteCooldownEnd");
                                                    sender.sendMessage(Variable.Lang_YML.getString("HeadLineTtitle"));
                                                    sender.sendMessage(temp);
                                                    sender.sendMessage(Variable.Lang_YML.getString("BottomLineTtitle"));
                                                }
                                            }
                                        }).runTaskLater(Main.JavaPlugin, 1200L);
                                        list.set(c, name + "," + (cs + 1));
                                        yamlConfiguration.set("DeleteTimes", list);

                                        try {
                                            yamlConfiguration.save(Variable.f_log);
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        }
                                        break;
                                    }
                                }

                                if (!check_contain) {
                                    list.add(String.valueOf(p.getName()) + ",1");
                                    yamlConfiguration.set("DeleteTimes", list);

                                    try {
                                        yamlConfiguration.save(Variable.f_log);
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }

                                yamlConfiguration = YamlConfiguration.loadConfiguration(Variable.f_log);
                                if (!yamlConfiguration.contains("NowID")) {
                                    yamlConfiguration.set("NowID", 0);
                                }

                                if (!yamlConfiguration.contains("MaxID")) {
                                    yamlConfiguration.set("MaxID", 1000);
                                }

                                try {
                                    yamlConfiguration.save(Variable.f_log);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }

                                yamlConfiguration.set("NowID", yamlConfiguration.getInt("NowID") - 1);

                                try {
                                    yamlConfiguration.save(Variable.f_log);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }

                                String temp = Variable.Lang_YML.getString("WholeDeleteSuccess");
                                sender.sendMessage(Variable.Lang_YML.getString("HeadLineTtitle"));
                                sender.sendMessage(temp);
                                sender.sendMessage(Variable.Lang_YML.getString("BottomLineTtitle"));
                                Variable.Deletecooldown.remove(p);
                                if (Variable.bungee) {
                                    MySQL.removePlayer(p.getName());
                                } else {
                                    File f2 = new File(Variable.Tempf, String.valueOf(p.getName()) + ".yml");
                                    f2.delete();
                                }

                                World world = Bukkit.getWorld(Variable.world_prefix + p.getName());
                                if (world != null) {
                                    for (Player p6 : Bukkit.getWorld(Variable.world_prefix + p.getName()).getPlayers()) {
                                        p6.teleport(Bukkit.getWorld(Main.JavaPlugin.getConfig().getString("Spawn")).getSpawnLocation());
                                        p6.sendMessage(Variable.Lang_YML.getString("WorldHasBeenDeleted"));
                                    }
                                }

                                Bukkit.unloadWorld(Variable.world_prefix + p.getName(), true);

                                for (int c = 0; c < Main.JavaPlugin.getConfig().getStringList("AfterDeleteDispathCommand").size(); c++) {
                                    String temp1 = (String)Main.JavaPlugin.getConfig().getStringList("DispathCommand").get(c);
                                    if (temp1.contains("<Name>")) {
                                        temp1 = temp1.replace("<Name>", p.getName());
                                    }

                                    if (temp1.contains("[console]")) {
                                        temp1 = temp1.replace("[console]", "");
                                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), temp1);
                                    } else if (temp1.contains("[player]")) {
                                        temp1 = temp1.replace("[player]", "");
                                        Bukkit.dispatchCommand(p, temp1);
                                    }
                                }

                                if (Variable.hook_multiverseCore) {
                                    WorldManager mv_m = MultiverseCoreApi.get().getWorldManager();
                                    mv_m.removeWorld(Variable.world_prefix + p.getName());
                                }

                                (new BukkitRunnable() {
                                        public void run() {
                                            if (Variable.world_prefix.equalsIgnoreCase("")) {
                                                if (!Bukkit.getVersion().toString().toUpperCase().contains("ARCLIGHT")
                                                    && !Bukkit.getVersion().toString().contains("1.20.1")) {
                                                    File f = new File(Variable.single_server_gen + "world" + Variable.file_loc_prefix + p.getName());
                                                    Util.deleteFile(f);
                                                } else {
                                                    File f = new File(Variable.single_server_gen + Variable.world_prefix + p.getName());
                                                    Util.deleteFile(f);
                                                }
                                            } else {
                                                File f = new File(Variable.single_server_gen + Variable.world_prefix + p.getName());
                                                Util.deleteFile(f);
                                            }
                                        }
                                    })
                                    .runTaskLater(Main.JavaPlugin, 5L);
                                return false;
                            }
                        }
                    } else if (args.length == 3 && args[0].equalsIgnoreCase("gift") && args[1].equalsIgnoreCase("send") && args[2].equalsIgnoreCase("all")) {
                        if (sender instanceof Player) {
                            Player temp = (Player)sender;
                            if (!temp.isOp()) {
                                sender.sendMessage(Variable.Lang_YML.getString("PlayerIsNotOperator"));
                                return false;
                            }
                        }

                        ItemStack c = p.getItemInHand().clone();
                        if (c == null) {
                            p.sendMessage(Variable.Lang_YML.getString("SendButTheHandIsAir"));
                            return false;
                        }

                        if (c.getType() == Material.AIR) {
                            p.sendMessage(Variable.Lang_YML.getString("SendButTheHandIsAir"));
                            return false;
                        }

                        List<String> has_send_list = new ArrayList<>();
                        List<String> has_not_send_list = new ArrayList<>();

                        for (Home home : HomeAPI.getHomes()) {
                            ItemStack i = c.clone();
                            String home_name = home.getName();
                            if (Variable.has_open_gifts_list.containsKey(home_name)) {
                                Player has_open = Bukkit.getPlayer(Variable.has_open_gifts_list.get(home_name));
                                if (has_open != null) {
                                    has_open.sendMessage(Variable.Lang_YML.getString("OperatorSendGiftButOpen"));
                                    has_open.closeInventory();
                                }
                            }

                            List<String> gifts = new ArrayList<>(home.getGifts());
                            if (gifts.size() >= 45) {
                                has_not_send_list.add(home.getName());
                            } else {
                                has_send_list.add(home.getName());
                                StreamSerializer ss = new StreamSerializer();
                                if (!Variable.Lang_YML.getString("GiftLoreAddPrefix").equalsIgnoreCase("")) {
                                    String lore = String.valueOf(Variable.Lang_YML.getString("GiftLoreAddPrefix")) + p.getName();
                                    if (i.hasItemMeta()) {
                                        ItemMeta meta = i.getItemMeta();
                                        if (meta.hasLore()) {
                                            List<String> lores = meta.getLore();
                                            lores.add(lore);
                                            meta.setLore(lores);
                                            i.setItemMeta(meta);
                                        } else {
                                            List<String> lores = new ArrayList<>();
                                            lores.add(lore);
                                            meta.setLore(lores);
                                            i.setItemMeta(meta);
                                        }
                                    } else {
                                        ItemMeta meta = i.getItemMeta();
                                        List<String> lores = new ArrayList<>();
                                        lores.add(lore);
                                        meta.setLore(lores);
                                        i.setItemMeta(meta);
                                    }
                                }

                                try {
                                    gifts.add(ss.serializeItemStack(i));
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }

                                try {
                                    home.setGifts(gifts);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }

                                String temp = Variable.Lang_YML.getString("GiftAdd");
                                if (temp.contains("<Name>")) {
                                    temp = temp.replace("<Name>", p.getName());
                                }

                                if (Variable.has_no_click_message) {
                                    if (Bukkit.getPlayer(home.getName()) != null) {
                                        Bukkit.getPlayer(home.getName()).sendMessage(temp);
                                    }
                                } else {
                                    TextComponent Click_Message = new TextComponent(temp);
                                    Click_Message.setClickEvent(new ClickEvent(Action.RUN_COMMAND, "/sh gift open"));
                                    if (Bukkit.getPlayer(home.getName()) != null) {
                                        Bukkit.getPlayer(home.getName()).spigot().sendMessage(Click_Message);
                                    }
                                }
                            }
                        }

                        String temp2 = Variable.Lang_YML.getString("SuccessedSendToAll");
                        if (temp2.contains("<List>")) {
                            temp2 = temp2.replace("<List>", has_send_list.toString());
                        }

                        p.sendMessage(temp2);
                        if (has_not_send_list.size() != 0) {
                            String temp3 = Variable.Lang_YML.getString("FailedSendToAll");
                            if (temp3.contains("<List>")) {
                                temp3 = temp3.replace("<List>", has_not_send_list.toString());
                            }

                            p.sendMessage(temp3);
                        }

                        return false;
                    } else if (args.length == 3 && args[0].equalsIgnoreCase("gift") && args[1].equalsIgnoreCase("send")) {
                        if (!p.hasPermission("SelfHome.Gift.Send")) {
                            p.sendMessage(Variable.Lang_YML.getString("NoPermissionSendTheItemToGift"));
                            return false;
                        }

                        String home_name = args[2];
                        if (!Util.CheckIsHome(home_name)) {
                            String temp = Variable.Lang_YML.getString("NowIsNotHome");
                            sender.sendMessage(Variable.Lang_YML.getString("HeadLineTtitle"));
                            sender.sendMessage(temp);
                            sender.sendMessage(Variable.Lang_YML.getString("BottomLineTtitle"));
                            return false;
                        }

                        if (p.getName().equalsIgnoreCase(home_name)) {
                            String temp = Variable.Lang_YML.getString("SendButTheMyHome");
                            sender.sendMessage(temp);
                            return false;
                        }

                        ItemStack i = p.getItemInHand();
                        if (i == null) {
                            p.sendMessage(Variable.Lang_YML.getString("SendButTheHandIsAir"));
                            return false;
                        }

                        if (i.getType() == Material.AIR) {
                            p.sendMessage(Variable.Lang_YML.getString("SendButTheHandIsAir"));
                            return false;
                        }

                        if (Variable.has_open_gifts_list.containsKey(home_name)) {
                            p.sendMessage(Variable.Lang_YML.getString("SendButTheInvHasBeenOpen"));
                            return false;
                        }

                        Home home = HomeAPI.getHome(home_name);
                        List<String> gifts = new ArrayList<>(home.getGifts());
                        if (gifts.size() >= 45) {
                            p.sendMessage(Variable.Lang_YML.getString("GiftFail"));
                            return false;
                        }

                        StreamSerializer ss = new StreamSerializer();
                        if (!Variable.Lang_YML.getString("GiftLoreAddPrefix").equalsIgnoreCase("")) {
                            String lore = String.valueOf(Variable.Lang_YML.getString("GiftLoreAddPrefix")) + p.getName();
                            if (i.hasItemMeta()) {
                                ItemMeta meta = i.getItemMeta();
                                if (meta.hasLore()) {
                                    List<String> lores = meta.getLore();
                                    lores.add(lore);
                                    meta.setLore(lores);
                                    i.setItemMeta(meta);
                                } else {
                                    List<String> lores = new ArrayList<>();
                                    lores.add(lore);
                                    meta.setLore(lores);
                                    i.setItemMeta(meta);
                                }
                            } else {
                                ItemMeta meta = i.getItemMeta();
                                List<String> lores = new ArrayList<>();
                                lores.add(lore);
                                meta.setLore(lores);
                                i.setItemMeta(meta);
                            }
                        }

                        try {
                            gifts.add(ss.serializeItemStack(i));
                            p.getInventory().remove(i);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        try {
                            home.setGifts(gifts);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        String temp2 = Variable.Lang_YML.getString("GiftSuccess");
                        if (temp2.contains("<Name>")) {
                            temp2 = temp2.replace("<Name>", home_name);
                        }

                        p.sendMessage(temp2);
                        String temp = Variable.Lang_YML.getString("GiftAdd");
                        if (temp.contains("<Name>")) {
                            temp = temp.replace("<Name>", p.getName());
                        }

                        if (Variable.has_no_click_message) {
                            if (Bukkit.getPlayer(home.getName()) != null
                                && Bukkit.getPlayer(home.getName()).isOnline()
                                && !home.getName().equalsIgnoreCase(p.getName())) {
                                Bukkit.getPlayer(home.getName()).sendMessage(temp);
                            }
                        } else {
                            TextComponent Click_Message = new TextComponent(temp);
                            Click_Message.setClickEvent(new ClickEvent(Action.RUN_COMMAND, "/sh gift open"));
                            if (Bukkit.getPlayer(home.getName()) != null
                                && Bukkit.getPlayer(home.getName()).isOnline()
                                && !home.getName().equalsIgnoreCase(p.getName())) {
                                Bukkit.getPlayer(home.getName()).spigot().sendMessage(Click_Message);
                            }
                        }

                        return false;
                    } else if (args.length == 4 && args[0].equalsIgnoreCase("popularity") && args[1].equalsIgnoreCase("add")) {
                        if (sender instanceof Player) {
                            Player temp = (Player)sender;
                            if (!temp.isOp()) {
                                sender.sendMessage(Variable.Lang_YML.getString("PlayerIsNotOperator"));
                                return false;
                            }
                        }

                        Home home = HomeAPI.getHome(args[2]);
                        if (home == null) {
                            sender.sendMessage(Variable.Lang_YML.getString("PopularityAddButHomeIsNotExist"));
                            return false;
                        }

                        try {
                            home.setPopularity(home.getPopularity() + Integer.valueOf(args[3]));
                        } catch (NumberFormatException e) {
                            e.printStackTrace();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        String temp2 = Variable.Lang_YML.getString("PopularityAddSuccess");
                        if (temp2.contains("<Now>")) {
                            temp2 = temp2.replace("<Now>", String.valueOf(home.getPopularity()));
                        }

                        p.sendMessage(temp2);
                        return false;
                    } else if (args.length == 4 && args[0].equalsIgnoreCase("flower") && args[1].equalsIgnoreCase("add")) {
                        if (sender instanceof Player) {
                            Player temp = (Player)sender;
                            if (!temp.isOp()) {
                                sender.sendMessage(Variable.Lang_YML.getString("PlayerIsNotOperator"));
                                return false;
                            }
                        }

                        Home home = HomeAPI.getHome(args[2]);
                        if (home == null) {
                            sender.sendMessage(Variable.Lang_YML.getString("FlowerAddButHomeIsNotExist"));
                            return false;
                        }

                        try {
                            home.setFlowers(home.getFlowers() + Integer.valueOf(args[3]));
                        } catch (NumberFormatException e) {
                            e.printStackTrace();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        String temp2 = Variable.Lang_YML.getString("FlowerAddSuccess");
                        if (temp2.contains("<Now>")) {
                            temp2 = temp2.replace("<Now>", String.valueOf(home.getFlowers()));
                        }

                        p.sendMessage(temp2);
                        return false;
                    } else if (args.length == 3 && args[0].equalsIgnoreCase("gift") && args[1].equalsIgnoreCase("inv")) {
                        if (!p.hasPermission("SelfHome.Gift.Inv")) {
                            p.sendMessage(Variable.Lang_YML.getString("InvPlayersGiftGuiButNoPermission"));
                            return false;
                        }

                        String home_name = args[2];
                        if (!Util.CheckIsHome(home_name)) {
                            String temp = Variable.Lang_YML.getString("NowIsNotHome");
                            sender.sendMessage(Variable.Lang_YML.getString("HeadLineTtitle"));
                            sender.sendMessage(temp);
                            sender.sendMessage(Variable.Lang_YML.getString("BottomLineTtitle"));
                            return false;
                        }

                        if (Variable.has_open_gifts_list.containsKey(home_name)) {
                            String str = Variable.Lang_YML.getString("HasAlreadyOpenByOthers");
                            if (str.contains("<Name>")) {
                                str = str.replace("<Name>", Variable.has_open_gifts_list.get(home_name));
                            }

                            p.sendMessage(str);
                            return false;
                        } else {
                            // Variable.has_open_gifts_list.put(home_name, p.getName());
                            // GiftGui giftgui = new GiftGui(p, home_name);
                            // p.openInventory(giftgui.getInventory());
                            return false;
                        }
                    } else if (args.length == 1 && args[0].equalsIgnoreCase("Icon")) {
                        if (!p.hasPermission("SelfHome.Icon")) {
                            p.sendMessage(Variable.Lang_YML.getString("NoPermissionSetIcon"));
                            return false;
                        }

                        if (!Util.CheckIsHome(p.getWorld().getName().replace(Variable.world_prefix, ""))) {
                            String temp = Variable.Lang_YML.getString("NowIsNotHome");
                            sender.sendMessage(Variable.Lang_YML.getString("HeadLineTtitle"));
                            sender.sendMessage(temp);
                            sender.sendMessage(Variable.Lang_YML.getString("BottomLineTtitle"));
                            return false;
                        }

                        if (!Util.Check(p, p.getWorld().getName().replace(Variable.world_prefix, ""))) {
                            p.sendMessage(Variable.Lang_YML.getString("NoPermissionSetOthersIcon"));
                            return false;
                        }

                        ItemStack i = p.getItemInHand();
                        if (i == null) {
                            p.sendMessage(Variable.Lang_YML.getString("SetIconButHandIsAir"));
                            return false;
                        }

                        if (i.getType() == Material.AIR) {
                            p.sendMessage(Variable.Lang_YML.getString("SetIconButHandIsAir"));
                            return false;
                        }

                        Home home = HomeAPI.getHome(p.getWorld().getName().replace(Variable.world_prefix, ""));

                        try {
                            home.setIcon(String.valueOf(i.getType().toString()) + ":" + i.getDurability());
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        if (i.getAmount() == 1) {
                            p.setItemInHand(null);
                        } else {
                            i.setAmount(i.getAmount() - 1);
                            p.setItemInHand(i);
                        }

                        p.sendMessage(Variable.Lang_YML.getString("SetIconSuccess"));
                        return false;
                    } else if (args.length == 5 && args[0].equalsIgnoreCase("AddBlockLimit")) {
                        if (sender instanceof Player) {
                            Player te = (Player)sender;
                            if (!te.isOp()) {
                                te.sendMessage(Variable.Lang_YML.getString("NoPermissionSetCustomBlockLimit"));
                                return false;
                            }
                        }

                        Home home = HomeAPI.getHome(args[1]);
                        if (home == null) {
                            sender.sendMessage(Variable.Lang_YML.getString("SetCustomBlockButHomeIsNull"));
                            return false;
                        }

                        String str = args[2] + "|" + args[3].toUpperCase() + "|";
                        List<String> list = home.getLimitBlock();
                        List<String> list2 = new ArrayList<>();

                        for (String e : list) {
                            list2.add(e);
                        }

                        boolean success = false;

                        for (int c = 0; c < list2.size(); c++) {
                            String temp = list2.get(c);
                            if (temp.contains(str)) {
                                success = true;
                                String[] args2 = temp.split("\\|");
                                int amount = Integer.valueOf(args2[2]);
                                int now = amount + Integer.valueOf(args[4]);
                                list2.set(c, str + String.valueOf(now));

                                try {
                                    home.setLimitBlock(list2);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }

                                String send_message = Variable.Lang_YML.getString("AddCustomBlockSuccess");
                                if (send_message.contains("<Name>")) {
                                    send_message = send_message.replace("<Name>", args[1]);
                                }

                                if (send_message.contains("<NBT>")) {
                                    send_message = send_message.replace("<NBT>", args[3].toUpperCase());
                                }

                                if (send_message.contains("<Amount>")) {
                                    send_message = send_message.replace("<Amount>", String.valueOf(amount));
                                }

                                if (send_message.contains("<Type>")) {
                                    send_message = send_message.replace("<Type>", args[2]);
                                }

                                if (send_message.contains("<NowAmount>")) {
                                    send_message = send_message.replace("<NowAmount>", String.valueOf(now));
                                }

                                sender.sendMessage(send_message);
                                break;
                            }
                        }

                        if (!success) {
                            list2.add(str + args[4]);

                            try {
                                home.setLimitBlock(list2);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }

                            String send_message = Variable.Lang_YML.getString("SetCustomBlockSuccess");
                            if (send_message.contains("<Name>")) {
                                send_message = send_message.replace("<Name>", args[1]);
                            }

                            if (send_message.contains("<NBT>")) {
                                send_message = send_message.replace("<NBT>", args[3].toUpperCase());
                            }

                            if (send_message.contains("<Amount>")) {
                                send_message = send_message.replace("<Amount>", args[4]);
                            }

                            if (send_message.contains("<Type>")) {
                                send_message = send_message.replace("<Type>", args[2]);
                            }

                            sender.sendMessage(send_message);
                        }

                        return false;
                    } else if (args.length == 5 && args[0].equalsIgnoreCase("SetBlockLimit")) {
                        if (sender instanceof Player) {
                            Player te = (Player)sender;
                            if (!te.isOp()) {
                                te.sendMessage(Variable.Lang_YML.getString("NoPermissionSetCustomBlockLimit"));
                                return false;
                            }
                        }

                        Home home = HomeAPI.getHome(args[1]);
                        if (home == null) {
                            sender.sendMessage(Variable.Lang_YML.getString("SetCustomBlockButHomeIsNull"));
                            return false;
                        }

                        String str = args[2] + "|" + args[3].toUpperCase() + "|" + args[4];
                        List<String> list = home.getLimitBlock();
                        List<String> list2 = new ArrayList<>();

                        for (String e : list) {
                            list2.add(e);
                        }

                        for (int c = 0; c < list2.size(); c++) {
                            String tem = list2.get(c);
                            if (tem.contains(args[2] + "|" + args[3].toUpperCase())) {
                                list2.remove(c);
                            }
                        }

                        list2.add(str);

                        try {
                            home.setLimitBlock(list2);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        String send_message = Variable.Lang_YML.getString("SetCustomBlockSuccess");
                        if (send_message.contains("<Name>")) {
                            send_message = send_message.replace("<Name>", args[1]);
                        }

                        if (send_message.contains("<NBT>")) {
                            send_message = send_message.replace("<NBT>", args[3].toUpperCase());
                        }

                        if (send_message.contains("<Amount>")) {
                            send_message = send_message.replace("<Amount>", args[4]);
                        }

                        if (send_message.contains("<Type>")) {
                            send_message = send_message.replace("<Type>", args[2]);
                        }

                        sender.sendMessage(send_message);
                        return false;
                    } else if (args.length == 4 && args[0].equalsIgnoreCase("DelBlockLimit")) {
                        if (sender instanceof Player) {
                            Player te = (Player)sender;
                            if (!te.isOp()) {
                                te.sendMessage(Variable.Lang_YML.getString("NoPermissionSetCustomBlockLimit"));
                                return false;
                            }
                        }

                        Home home = HomeAPI.getHome(args[1]);
                        if (home == null) {
                            sender.sendMessage(Variable.Lang_YML.getString("SetCustomBlockButHomeIsNull"));
                            return false;
                        }

                        String str = args[2] + "|" + args[3].toUpperCase();
                        List<String> list = home.getLimitBlock();
                        List<String> list2 = new ArrayList<>();

                        for (String e : list) {
                            list2.add(e);
                        }

                        boolean remove_success = false;

                        for (int c = 0; c < list2.size(); c++) {
                            String str2 = list2.get(c);
                            if (str2.contains(args[2] + "|" + args[3].toUpperCase())) {
                                list2.remove(c);
                                remove_success = true;
                            }
                        }

                        if (!remove_success) {
                            sender.sendMessage(Variable.Lang_YML.getString("SetCustomBlockButHomeButNotContain"));
                            return false;
                        }

                        try {
                            home.setLimitBlock(list2);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        String send_message = Variable.Lang_YML.getString("RemoveCustomBlockSuccess");
                        if (send_message.contains("<Name>")) {
                            send_message = send_message.replace("<Name>", args[1]);
                        }

                        if (send_message.contains("<NBT>")) {
                            send_message = send_message.replace("<NBT>", args[3].toUpperCase());
                        }

                        if (send_message.contains("<Type>")) {
                            send_message = send_message.replace("<Type>", args[2]);
                        }

                        sender.sendMessage(send_message);
                        return false;
                    } else if (args.length >= 2 && args[0].equalsIgnoreCase("info")) {
                        String str = "";

                        for (int d = 1; d < args.length; d++) {
                            str = str + " " + args[d];
                        }

                        if (!p.hasPermission("SelfHome.info")) {
                            p.sendMessage(Variable.Lang_YML.getString("NoPermissionSetInfo"));
                            return false;
                        }

                        if (!Util.CheckIsHome(p.getWorld().getName().replace(Variable.world_prefix, ""))) {
                            String temp = Variable.Lang_YML.getString("NowIsNotHome");
                            sender.sendMessage(Variable.Lang_YML.getString("HeadLineTtitle"));
                            sender.sendMessage(temp);
                            sender.sendMessage(Variable.Lang_YML.getString("BottomLineTtitle"));
                            return false;
                        }

                        if (!Util.Check(p, p.getWorld().getName().replace(Variable.world_prefix, ""))) {
                            p.sendMessage(Variable.Lang_YML.getString("NoPermissionSetOthersInfo"));
                            return false;
                        }

                        List<String> adv = new ArrayList<>();
                        if (str.contains(",")) {
                            String[] content = str.split(",");

                            for (int i = 0; i < content.length; i++) {
                                content[i] = "§f" + content[i];
                                if (content[i].contains("&")) {
                                    if (!p.hasPermission("SelfHome.Info.Color")) {
                                        p.sendMessage(Variable.Lang_YML.getString("NoPermissionSetColorInfo"));
                                        return false;
                                    }

                                    content[i] = content[i].replace("&", "§");
                                }
                            }

                            adv = Arrays.asList(content);
                        } else {
                            if (str.contains("&")) {
                                if (!p.hasPermission("SelfHome.Info.Color")) {
                                    p.sendMessage(Variable.Lang_YML.getString("NoPermissionSetColorInfo"));
                                    return false;
                                }

                                str = str.replace("&", "§");
                            }

                            adv.add("§f" + str);
                        }

                        Home home = HomeAPI.getHome(p.getWorld().getName().replace(Variable.world_prefix, ""));

                        try {
                            home.setAdvertisement(adv);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        p.sendMessage(Variable.Lang_YML.getString("SetInfoSuccess"));
                        return false;
                    } else if (args.length == 2 && args[0].equalsIgnoreCase("gift") && args[1].equalsIgnoreCase("open")) {
                        if (!p.hasPermission("SelfHome.Gift.Open")) {
                            p.sendMessage(Variable.Lang_YML.getString("NoPermissionOpenGift"));
                            return false;
                        }

                        if (!Util.CheckIsHome(p.getWorld().getName().replace(Variable.world_prefix, ""))) {
                            String temp = Variable.Lang_YML.getString("NowIsNotHome");
                            sender.sendMessage(Variable.Lang_YML.getString("HeadLineTtitle"));
                            sender.sendMessage(temp);
                            sender.sendMessage(Variable.Lang_YML.getString("BottomLineTtitle"));
                            return false;
                        }

                        if (!Util.Check(p, p.getWorld().getName().replace(Variable.world_prefix, ""))) {
                            p.sendMessage(Variable.Lang_YML.getString("NoPermissionOpenOthersGift"));
                            return false;
                        }

                        if (Variable.has_open_gifts_list.containsKey(p.getWorld().getName().replace(Variable.world_prefix, ""))) {
                            String str = Variable.Lang_YML.getString("HasAlreadyOpenByOthers");
                            if (str.contains("<Name>")) {
                                str = str.replace("<Name>", Variable.has_open_gifts_list.get(p.getWorld().getName().replace(Variable.world_prefix, "")));
                            }

                            p.sendMessage(str);
                            return false;
                        } else {
                            // Variable.has_open_gifts_list.put(p.getWorld().getName().replace(Variable.world_prefix, ""), p.getName());
                            // GiftGui giftgui = new GiftGui(p, p.getWorld().getName().replace(Variable.world_prefix, ""));
                            // p.openInventory(giftgui.getInventory());
                            return false;
                        }
                    } else if (args.length != 1 || !args[0].equalsIgnoreCase("home") && !args[0].equalsIgnoreCase("h")) {
                        if (args.length == 1) {
                            if (args[0].equalsIgnoreCase("public")) {
                                if (!Main.JavaPlugin.getConfig().getBoolean("Permission.CommandUser")
                                    && !p.hasPermission("SelfHome.Public")
                                    && !p.hasPermission("SelfHome.command.user")) {
                                    String tip = Variable.Lang_YML.getString("NoPermissionCheck");
                                    if (tip.contains("<Permission>")) {
                                        tip = tip.replace("<Permission>", "SelfHome.Public");
                                    }

                                    p.sendMessage(tip);
                                    return false;
                                }

                                if (Variable.bungee) {
                                    if (!MySQL.CheckIsAHome(p.getWorld().getName().replace(Variable.world_prefix, ""))) {
                                        String tip = Variable.Lang_YML.getString("NowIsNotHome");
                                        p.sendMessage(tip);
                                        return false;
                                    }
                                } else {
                                    File f2 = new File(Variable.Tempf, String.valueOf(p.getWorld().getName().replace(Variable.world_prefix, "")) + ".yml");
                                    if (!f2.exists()) {
                                        String tip = Variable.Lang_YML.getString("NowIsNotHome");
                                        p.sendMessage(tip);
                                        return false;
                                    }
                                }

                                File f2 = new File(Variable.Tempf, String.valueOf(p.getWorld().getName().replace(Variable.world_prefix, "")) + ".yml");
                                if (Util.CheckOwnerAndManagerAndOP(p, p.getWorld().getName().replace(Variable.world_prefix, ""))) {
                                    if (Variable.bungee) {
                                        if (MySQL.getPublic(p.getWorld().getName().replace(Variable.world_prefix, "")).equals("true")) {
                                            MySQL.setPublic(p.getWorld().getName().replace(Variable.world_prefix, ""), "false");
                                            String temp = Variable.Lang_YML.getString("DisablePublic");
                                            sender.sendMessage(Variable.Lang_YML.getString("HeadLineTtitle"));
                                            sender.sendMessage(temp);
                                            sender.sendMessage(Variable.Lang_YML.getString("BottomLineTtitle"));

                                            for (Player pt : Bukkit.getOnlinePlayers()) {
                                                if (!Util.Check(pt, p.getWorld().getName().replace(Variable.world_prefix, ""))
                                                    && pt.getWorld()
                                                        .getName()
                                                        .replace(Variable.world_prefix, "")
                                                        .equalsIgnoreCase(p.getWorld().getName().replace(Variable.world_prefix, ""))) {
                                                    String bekicked = Main.JavaPlugin.getConfig().getString("BeKickedCommand");
                                                    if (bekicked.contains("<Name>")) {
                                                        bekicked = bekicked.replace("<Name>", pt.getName());
                                                    }

                                                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), bekicked);
                                                    pt.sendMessage("§a§l§m--------------" + Variable.Prefix + "§a§l§m--------------");
                                                    String temp2 = Variable.Lang_YML.getString("BeKicked");
                                                    pt.sendMessage(temp2);
                                                    pt.sendMessage("§a§l§m--------------" + Variable.Prefix + "§a§l§m--------------");
                                                }
                                            }
                                        } else {
                                            MySQL.setPublic(p.getWorld().getName().replace(Variable.world_prefix, ""), "true");
                                            String temp = Variable.Lang_YML.getString("EnablePublic");
                                            sender.sendMessage(Variable.Lang_YML.getString("HeadLineTtitle"));
                                            sender.sendMessage(temp);
                                            sender.sendMessage(Variable.Lang_YML.getString("BottomLineTtitle"));
                                        }
                                    } else {
                                        YamlConfiguration yamlConfiguration = YamlConfiguration.loadConfiguration(f2);
                                        if (yamlConfiguration.getBoolean("Public")) {
                                            yamlConfiguration.set("Public", false);

                                            try {
                                                yamlConfiguration.save(f2);
                                            } catch (Exception e) {
                                                e.printStackTrace();
                                            }

                                            String temp = Variable.Lang_YML.getString("DisablePublic");
                                            sender.sendMessage(Variable.Lang_YML.getString("HeadLineTtitle"));
                                            sender.sendMessage(temp);
                                            sender.sendMessage(Variable.Lang_YML.getString("BottomLineTtitle"));

                                            for (Player pt : Bukkit.getOnlinePlayers()) {
                                                if (!Util.Check(pt, p.getWorld().getName().replace(Variable.world_prefix, ""))
                                                    && pt.getWorld()
                                                        .getName()
                                                        .replace(Variable.world_prefix, "")
                                                        .equalsIgnoreCase(p.getWorld().getName().replace(Variable.world_prefix, ""))) {
                                                    String bekicked = Variable.Lang_YML.getString("BeKickedCommand");
                                                    if (bekicked.contains("<Name>")) {
                                                        bekicked = bekicked.replace("<Name>", pt.getName());
                                                    }

                                                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), bekicked);
                                                    pt.sendMessage("§a§l§m--------------" + Variable.Prefix + "§a§l§m--------------");
                                                    String temp2 = Variable.Lang_YML.getString("BeKicked");
                                                    pt.sendMessage(temp2);
                                                    pt.sendMessage("§a§l§m--------------" + Variable.Prefix + "§a§l§m--------------");
                                                }
                                            }
                                        } else {
                                            yamlConfiguration.set("Public", true);

                                            try {
                                                yamlConfiguration.save(f2);
                                            } catch (Exception e) {
                                                e.printStackTrace();
                                            }

                                            String temp = Variable.Lang_YML.getString("EnablePublic");
                                            sender.sendMessage(Variable.Lang_YML.getString("HeadLineTtitle"));
                                            sender.sendMessage(temp);
                                            sender.sendMessage(Variable.Lang_YML.getString("BottomLineTtitle"));
                                        }
                                    }
                                } else {
                                    String temp = Variable.Lang_YML.getString("NoOwnerAndManagerPermission");
                                    sender.sendMessage(Variable.Lang_YML.getString("HeadLineTtitle"));
                                    sender.sendMessage(temp);
                                    sender.sendMessage(Variable.Lang_YML.getString("BottomLineTtitle"));
                                }

                                return false;
                            }

                            if (args[0].equalsIgnoreCase("tpset")) {
                                if (Variable.bungee) {
                                    if (Util.CheckOwnerAndManagerAndOP(p, p.getWorld().getName().replace(Variable.world_prefix, ""))) {
                                        MySQL.setX(p.getWorld().getName().replace(Variable.world_prefix, ""), String.valueOf(p.getLocation().getX()));
                                        MySQL.setY(p.getWorld().getName().replace(Variable.world_prefix, ""), String.valueOf(p.getLocation().getY()));
                                        MySQL.setZ(p.getWorld().getName().replace(Variable.world_prefix, ""), String.valueOf(p.getLocation().getZ()));
                                        String temp = Variable.Lang_YML.getString("TpSetSuccess");
                                        sender.sendMessage(Variable.Lang_YML.getString("HeadLineTtitle"));
                                        sender.sendMessage(temp);
                                        sender.sendMessage(Variable.Lang_YML.getString("BottomLineTtitle"));
                                        return false;
                                    }

                                    String temp = Variable.Lang_YML.getString("NoOwnerAndManagerPermission");
                                    sender.sendMessage(Variable.Lang_YML.getString("HeadLineTtitle"));
                                    sender.sendMessage(temp);
                                    sender.sendMessage(Variable.Lang_YML.getString("BottomLineTtitle"));
                                    return false;
                                }

                                File f2 = new File(Variable.Tempf, String.valueOf(p.getWorld().getName().replace(Variable.world_prefix, "")) + ".yml");
                                if (!Main.JavaPlugin.getConfig().getBoolean("Permission.CommandUser")
                                    && !p.hasPermission("SelfHome.tpset")
                                    && !p.hasPermission("SelfHome.command.user")) {
                                    String tip = Variable.Lang_YML.getString("NoPermissionCheck");
                                    if (tip.contains("<Permission>")) {
                                        tip = tip.replace("<Permission>", "SelfHome.TpSet");
                                    }

                                    p.sendMessage(tip);
                                    return false;
                                }

                                if (Util.CheckOwnerAndManagerAndOP(p, p.getWorld().getName().replace(Variable.world_prefix, ""))) {
                                    YamlConfiguration yamlConfiguration = YamlConfiguration.loadConfiguration(f2);
                                    yamlConfiguration.set("X", p.getLocation().getX());
                                    yamlConfiguration.set("Y", p.getLocation().getY());
                                    yamlConfiguration.set("Z", p.getLocation().getZ());

                                    try {
                                        yamlConfiguration.save(f2);
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }

                                    String temp = Variable.Lang_YML.getString("TpSetSuccess");
                                    sender.sendMessage(Variable.Lang_YML.getString("HeadLineTtitle"));
                                    sender.sendMessage(temp);
                                    sender.sendMessage(Variable.Lang_YML.getString("BottomLineTtitle"));
                                    return false;
                                }

                                String temp = Variable.Lang_YML.getString("NoOwnerAndManagerPermission");
                                sender.sendMessage(Variable.Lang_YML.getString("HeadLineTtitle"));
                                sender.sendMessage(temp);
                                sender.sendMessage(Variable.Lang_YML.getString("BottomLineTtitle"));
                                return false;
                            }

                            if (args[0].equalsIgnoreCase("flower")) {
                                if (sender instanceof Player) {
                                    if (p.getName().equalsIgnoreCase(p.getWorld().getName().replace(Variable.world_prefix, ""))) {
                                        p.sendMessage(Variable.Lang_YML.getString("FlowersMySelf"));
                                        return false;
                                    }

                                    int set_flower = Main.JavaPlugin.getConfig().getInt("MaxFlowers");

                                    for (int i = Main.JavaPlugin.getConfig().getInt("MaxFlowers") + 100; i > 0; i--) {
                                        if (p.hasPermission("SelfHome.Flowers." + i) && !p.isOp()) {
                                            set_flower = i;
                                            break;
                                        }
                                    }

                                    Home home_check = HomeAPI.getHome(p.getWorld().getName());
                                    if (home_check == null) {
                                        sender.sendMessage(Variable.Lang_YML.getString("FlowerAddButHomeIsNotExist"));
                                        return false;
                                    }

                                    if (Variable.flowers_list.containsKey(p.getName())) {
                                        int has_give_amount = Variable.flowers_list.get(p.getName());
                                        if (has_give_amount >= set_flower) {
                                            String temp = Variable.Lang_YML.getString("FlowersMax");
                                            if (temp.contains("<Max>")) {
                                                temp = temp.replace("<Max>", String.valueOf(set_flower));
                                            }

                                            p.sendMessage(temp);
                                            return false;
                                        }

                                        Variable.flowers_list.put(p.getName(), has_give_amount + 1);
                                        Home home = HomeAPI.getHome(p.getWorld().getName().replace(Variable.world_prefix, ""));

                                        try {
                                            home.setFlowers(home.getFlowers() + 1);
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        }

                                        String temp = Variable.Lang_YML.getString("FlowersAdd");
                                        if (temp.contains("<Name>")) {
                                            temp = temp.replace("<Name>", p.getWorld().getName().replace(Variable.world_prefix, ""));
                                        }

                                        if (temp.contains("<Now>")) {
                                            temp = temp.replace("<Now>", String.valueOf(Variable.flowers_list.get(p.getName())));
                                        }

                                        if (temp.contains("<Max>")) {
                                            temp = temp.replace("<Max>", String.valueOf(Main.JavaPlugin.getConfig().getInt("MaxFlowers")));
                                        }

                                        p.sendMessage(temp);
                                        String temp2 = Variable.Lang_YML.getString("FlowersAddToOwnerAndOP");
                                        if (temp2.contains("<Player>")) {
                                            temp2 = temp2.replace("<Player>", p.getName());
                                        }

                                        for (String s : home.getOPs()) {
                                            if (Bukkit.getPlayer(s) != null) {
                                                Bukkit.getPlayer(temp2);
                                            }
                                        }

                                        if (Bukkit.getPlayer(home.getName()) != null) {
                                            Bukkit.getPlayer(home.getName()).sendMessage(temp2);
                                        }
                                    } else {
                                        Variable.flowers_list.put(p.getName(), 1);
                                        Home home = HomeAPI.getHome(p.getWorld().getName().replace(Variable.world_prefix, ""));

                                        try {
                                            home.setFlowers(1);
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        }

                                        String temp = Variable.Lang_YML.getString("FlowersAdd");
                                        if (temp.contains("<Name>")) {
                                            temp = temp.replace("<Name>", p.getWorld().getName().replace(Variable.world_prefix, ""));
                                        }

                                        if (temp.contains("<Now>")) {
                                            temp = temp.replace("<Now>", "1");
                                        }

                                        if (temp.contains("<Max>")) {
                                            temp = temp.replace("<Max>", String.valueOf(Main.JavaPlugin.getConfig().getInt("MaxFlowers")));
                                        }

                                        p.sendMessage(temp);
                                        String temp2 = Variable.Lang_YML.getString("FlowersAddToOwnerAndOP");
                                        if (temp2.contains("<Player>")) {
                                            temp2 = temp2.replace("<Player>", p.getName());
                                        }

                                        for (String s : home.getOPs()) {
                                            if (Bukkit.getPlayer(s) != null) {
                                                Bukkit.getPlayer(temp2);
                                            }
                                        }

                                        if (Bukkit.getPlayer(home.getName()) != null) {
                                            Bukkit.getPlayer(home.getName()).sendMessage(temp2);
                                        }
                                    }
                                }

                                return false;
                            }

                            if (args[0].equalsIgnoreCase("MobSpawn")) {
                                if (Util.CheckOwnerAndManagerAndOP(p, p.getWorld().getName().replace(Variable.world_prefix, ""))) {
                                    if (!p.hasPermission("SelfHome.MobSpawn")) {
                                        String tip = Variable.Lang_YML.getString("NoPermissionCheck");
                                        if (tip.contains("<Permission>")) {
                                            tip = tip.replace("<Permission>", "SelfHome.MobSpawn");
                                        }

                                        p.sendMessage(tip);
                                        return false;
                                    }

                                    World world = p.getWorld();
                                    if (world.getGameRuleValue("doMobSpawning").equalsIgnoreCase("false")) {
                                        world.setGameRuleValue("doMobSpawning", "true");
                                        if (Variable.hook_multiverseCore) {
                                            WorldManager mv_m = MultiverseCoreApi.get().getWorldManager();
                                            MultiverseWorld mv = mv_m.getWorld(p.getLocation().getWorld().getName()).getOrNull();
                                            // mv.setAllowMonsterSpawn(true);
                                        }

                                        if (world.getDifficulty() == Difficulty.PEACEFUL) {
                                            world.setDifficulty(Difficulty.HARD);
                                            if (Variable.hook_multiverseCore) {
                                                WorldManager mv_m = MultiverseCoreApi.get().getWorldManager();
                                                MultiverseWorld mv = mv_m.getWorld(p.getLocation().getWorld().getName()).getOrNull();
                                                mv.setDifficulty(Difficulty.HARD);
                                            }
                                        }

                                        String temp = Variable.Lang_YML.getString("EnableMobSpawn");
                                        sender.sendMessage(Variable.Lang_YML.getString("HeadLineTtitle"));
                                        sender.sendMessage(temp);
                                        sender.sendMessage(Variable.Lang_YML.getString("BottomLineTtitle"));
                                    } else if (world.getGameRuleValue("doMobSpawning").equalsIgnoreCase("true")) {
                                        world.setGameRuleValue("doMobSpawning", "false");
                                        if (Main.JavaPlugin.getConfig().getString("Difficulty").equalsIgnoreCase("Easy")) {
                                            world.setDifficulty(Difficulty.EASY);
                                            if (Variable.hook_multiverseCore) {
                                                WorldManager mv_m = MultiverseCoreApi.get().getWorldManager();
                                                MultiverseWorld mv = mv_m.getWorld(p.getLocation().getWorld().getName()).getOrNull();
                                                mv.setDifficulty(Difficulty.EASY);
                                            }
                                        } else if (Main.JavaPlugin.getConfig().getString("Difficulty").equalsIgnoreCase("Normal")) {
                                            world.setDifficulty(Difficulty.NORMAL);
                                            if (Variable.hook_multiverseCore) {
                                                WorldManager mv_m = MultiverseCoreApi.get().getWorldManager();
                                                MultiverseWorld mv = mv_m.getWorld(p.getLocation().getWorld().getName()).getOrNull();
                                                mv.setDifficulty(Difficulty.NORMAL);
                                            }
                                        } else if (Main.JavaPlugin.getConfig().getString("Difficulty").equalsIgnoreCase("Hard")) {
                                            world.setDifficulty(Difficulty.HARD);
                                            if (Variable.hook_multiverseCore) {
                                                WorldManager mv_m = MultiverseCoreApi.get().getWorldManager();
                                                MultiverseWorld mv = mv_m.getWorld(p.getLocation().getWorld().getName()).getOrNull();
                                                mv.setDifficulty(Difficulty.HARD);
                                            }
                                        } else if (Main.JavaPlugin.getConfig().getString("Difficulty").equalsIgnoreCase("Peaceful")) {
                                            world.setDifficulty(Difficulty.PEACEFUL);
                                            if (Variable.hook_multiverseCore) {
                                                WorldManager mv_m = MultiverseCoreApi.get().getWorldManager();
                                                MultiverseWorld mv = mv_m.getWorld(p.getLocation().getWorld().getName()).getOrNull();
                                                mv.setDifficulty(Difficulty.PEACEFUL);
                                            }
                                        }

                                        String temp = Variable.Lang_YML.getString("DisableMobSpawn");
                                        sender.sendMessage(Variable.Lang_YML.getString("HeadLineTtitle"));
                                        sender.sendMessage(temp);
                                        sender.sendMessage(Variable.Lang_YML.getString("BottomLineTtitle"));
                                    }

                                    return false;
                                }

                                String temp = Variable.Lang_YML.getString("NoOwnerAndManagerPermission");
                                sender.sendMessage(Variable.Lang_YML.getString("HeadLineTtitle"));
                                sender.sendMessage(temp);
                                sender.sendMessage(Variable.Lang_YML.getString("BottomLineTtitle"));
                                return false;
                            }

                            if (args[0].equalsIgnoreCase("pvp")) {
                                File f2 = new File(Variable.Tempf, String.valueOf(p.getWorld().getName().replace(Variable.world_prefix, "")) + ".yml");
                                if (!Main.JavaPlugin.getConfig().getBoolean("Permission.CommandUser")
                                    && !p.hasPermission("SelfHome.PVP")
                                    && !p.hasPermission("SelfHome.command.user")) {
                                    String tip = Variable.Lang_YML.getString("NoPermissionCheck");
                                    if (tip.contains("<Permission>")) {
                                        tip = tip.replace("<Permission>", "SelfHome.PVP");
                                    }

                                    p.sendMessage(tip);
                                    return false;
                                }

                                if (Util.CheckOwnerAndManagerAndOP(p, p.getWorld().getName().replace(Variable.world_prefix, ""))) {
                                    if (Variable.bungee) {
                                        if (MySQL.getPVP(p.getWorld().getName().replace(Variable.world_prefix, "")).equalsIgnoreCase("true")) {
                                            MySQL.setpvp(p.getWorld().getName().replace(Variable.world_prefix, ""), "false");
                                            if (Variable.hook_multiverseCore) {
                                                WorldManager mv_m = MultiverseCoreApi.get().getWorldManager();
                                                MultiverseWorld mv = mv_m.getWorld(p.getLocation().getWorld().getName()).getOrNull();
                                                mv.setPvp(false);
                                            }

                                            String temp = Variable.Lang_YML.getString("DisablePVP");
                                            sender.sendMessage(Variable.Lang_YML.getString("HeadLineTtitle"));
                                            sender.sendMessage(temp);
                                            sender.sendMessage(Variable.Lang_YML.getString("BottomLineTtitle"));
                                        } else {
                                            MySQL.setpvp(p.getWorld().getName().replace(Variable.world_prefix, ""), "true");
                                            if (Variable.hook_multiverseCore) {
                                                WorldManager mv_m = MultiverseCoreApi.get().getWorldManager();
                                                MultiverseWorld mv = mv_m.getWorld(p.getLocation().getWorld().getName()).getOrNull();
                                                mv.setPvp(true);
                                            }

                                            String temp = Variable.Lang_YML.getString("EnablePVP");
                                            sender.sendMessage(Variable.Lang_YML.getString("HeadLineTtitle"));
                                            sender.sendMessage(temp);
                                            sender.sendMessage(Variable.Lang_YML.getString("BottomLineTtitle"));
                                        }
                                    } else {
                                        YamlConfiguration yamlConfiguration = YamlConfiguration.loadConfiguration(f2);
                                        if (yamlConfiguration.getBoolean("pvp")) {
                                            yamlConfiguration.set("pvp", false);

                                            try {
                                                yamlConfiguration.save(f2);
                                            } catch (Exception e) {
                                                e.printStackTrace();
                                            }

                                            if (Variable.hook_multiverseCore) {
                                                WorldManager mv_m = MultiverseCoreApi.get().getWorldManager();
                                                MultiverseWorld mv = mv_m.getWorld(p.getLocation().getWorld().getName()).getOrNull();
                                                mv.setPvp(false);
                                            }

                                            String temp = Variable.Lang_YML.getString("DisablePVP");
                                            sender.sendMessage(Variable.Lang_YML.getString("HeadLineTtitle"));
                                            sender.sendMessage(temp);
                                            sender.sendMessage(Variable.Lang_YML.getString("BottomLineTtitle"));
                                        } else {
                                            yamlConfiguration.set("pvp", true);

                                            try {
                                                yamlConfiguration.save(f2);
                                            } catch (Exception e) {
                                                e.printStackTrace();
                                            }

                                            if (Variable.hook_multiverseCore) {
                                                WorldManager mv_m = MultiverseCoreApi.get().getWorldManager();
                                                MultiverseWorld mv = mv_m.getWorld(p.getLocation().getWorld().getName()).getOrNull();
                                                mv.setPvp(true);
                                            }

                                            String temp = Variable.Lang_YML.getString("EnablePVP");
                                            sender.sendMessage(Variable.Lang_YML.getString("HeadLineTtitle"));
                                            sender.sendMessage(temp);
                                            sender.sendMessage(Variable.Lang_YML.getString("BottomLineTtitle"));
                                        }
                                    }
                                } else {
                                    String temp = Variable.Lang_YML.getString("NoOwnerAndManagerPermission");
                                    sender.sendMessage(Variable.Lang_YML.getString("HeadLineTtitle"));
                                    sender.sendMessage(temp);
                                    sender.sendMessage(Variable.Lang_YML.getString("BottomLineTtitle"));
                                }

                                return false;
                            }

                            if (args[0].equalsIgnoreCase("pickup")) {
                                File f2 = new File(Variable.Tempf, String.valueOf(p.getWorld().getName().replace(Variable.world_prefix, "")) + ".yml");
                                if (!Main.JavaPlugin.getConfig().getBoolean("Permission.CommandUser")
                                    && !p.hasPermission("SelfHome.PickUp")
                                    && !p.hasPermission("SelfHome.command.user")) {
                                    String tip = Variable.Lang_YML.getString("NoPermissionCheck");
                                    if (tip.contains("<Permission>")) {
                                        tip = tip.replace("<Permission>", "SelfHome.PickUp");
                                    }

                                    p.sendMessage(tip);
                                    return false;
                                }

                                if (Util.CheckOwnerAndManagerAndOP(p, p.getWorld().getName().replace(Variable.world_prefix, ""))) {
                                    if (Variable.bungee) {
                                        if (MySQL.getpickup(p.getWorld().getName().replace(Variable.world_prefix, "")).equalsIgnoreCase("true")) {
                                            MySQL.setpickup(p.getWorld().getName().replace(Variable.world_prefix, ""), "false");
                                            String temp = Variable.Lang_YML.getString("DisablePickup");
                                            sender.sendMessage(Variable.Lang_YML.getString("HeadLineTtitle"));
                                            sender.sendMessage(temp);
                                            sender.sendMessage(Variable.Lang_YML.getString("BottomLineTtitle"));
                                        } else {
                                            MySQL.setpickup(p.getWorld().getName().replace(Variable.world_prefix, ""), "true");
                                            String temp = Variable.Lang_YML.getString("EnablePickup");
                                            sender.sendMessage(Variable.Lang_YML.getString("HeadLineTtitle"));
                                            sender.sendMessage(temp);
                                            sender.sendMessage(Variable.Lang_YML.getString("BottomLineTtitle"));
                                        }
                                    } else {
                                        YamlConfiguration yamlConfiguration = YamlConfiguration.loadConfiguration(f2);
                                        if (yamlConfiguration.getBoolean("pickup")) {
                                            yamlConfiguration.set("pickup", false);

                                            try {
                                                yamlConfiguration.save(f2);
                                            } catch (Exception e) {
                                                e.printStackTrace();
                                            }

                                            String temp = Variable.Lang_YML.getString("DisablePickup");
                                            sender.sendMessage(Variable.Lang_YML.getString("HeadLineTtitle"));
                                            sender.sendMessage(temp);
                                            sender.sendMessage(Variable.Lang_YML.getString("BottomLineTtitle"));
                                        } else {
                                            yamlConfiguration.set("pickup", true);

                                            try {
                                                yamlConfiguration.save(f2);
                                            } catch (Exception e) {
                                                e.printStackTrace();
                                            }

                                            String temp = Variable.Lang_YML.getString("EnablePickup");
                                            sender.sendMessage(Variable.Lang_YML.getString("HeadLineTtitle"));
                                            sender.sendMessage(temp);
                                            sender.sendMessage(Variable.Lang_YML.getString("BottomLineTtitle"));
                                        }
                                    }
                                } else {
                                    String temp = Variable.Lang_YML.getString("NoOwnerPermission");
                                    sender.sendMessage(Variable.Lang_YML.getString("HeadLineTtitle"));
                                    sender.sendMessage(temp);
                                    sender.sendMessage(Variable.Lang_YML.getString("BottomLineTtitle"));
                                }

                                return false;
                            }

                            if (args[0].equalsIgnoreCase("drop")) {
                                File f2 = new File(Variable.Tempf, String.valueOf(p.getWorld().getName().replace(Variable.world_prefix, "")) + ".yml");
                                if (!Main.JavaPlugin.getConfig().getBoolean("Permission.CommandUser")
                                    && !p.hasPermission("SelfHome.Drop")
                                    && !p.hasPermission("SelfHome.command.user")) {
                                    String tip = Variable.Lang_YML.getString("NoPermissionCheck");
                                    if (tip.contains("<Permission>")) {
                                        tip = tip.replace("<Permission>", "SelfHome.Drop");
                                    }

                                    p.sendMessage(tip);
                                    return false;
                                }

                                if (Util.CheckOwnerAndManagerAndOP(p, p.getWorld().getName().replace(Variable.world_prefix, ""))) {
                                    if (Variable.bungee) {
                                        if (MySQL.getdropitem(p.getWorld().getName().replace(Variable.world_prefix, "")).equalsIgnoreCase("true")) {
                                            MySQL.setdropitem(p.getWorld().getName().replace(Variable.world_prefix, ""), "false");
                                            String temp = Variable.Lang_YML.getString("DisableDrop");
                                            sender.sendMessage(Variable.Lang_YML.getString("HeadLineTtitle"));
                                            sender.sendMessage(temp);
                                            sender.sendMessage(Variable.Lang_YML.getString("BottomLineTtitle"));
                                        } else {
                                            MySQL.setdropitem(p.getWorld().getName().replace(Variable.world_prefix, ""), "true");
                                            String temp = Variable.Lang_YML.getString("EnableDrop");
                                            sender.sendMessage(Variable.Lang_YML.getString("HeadLineTtitle"));
                                            sender.sendMessage(temp);
                                            sender.sendMessage(Variable.Lang_YML.getString("BottomLineTtitle"));
                                        }
                                    } else {
                                        YamlConfiguration yamlConfiguration = YamlConfiguration.loadConfiguration(f2);
                                        if (yamlConfiguration.getBoolean("drop")) {
                                            yamlConfiguration.set("drop", false);

                                            try {
                                                yamlConfiguration.save(f2);
                                            } catch (Exception e) {
                                                e.printStackTrace();
                                            }

                                            String temp = Variable.Lang_YML.getString("DisableDrop");
                                            sender.sendMessage(Variable.Lang_YML.getString("HeadLineTtitle"));
                                            sender.sendMessage(temp);
                                            sender.sendMessage(Variable.Lang_YML.getString("BottomLineTtitle"));
                                        } else {
                                            yamlConfiguration.set("drop", true);

                                            try {
                                                yamlConfiguration.save(f2);
                                            } catch (Exception e) {
                                                e.printStackTrace();
                                            }

                                            String temp = Variable.Lang_YML.getString("EnableDrop");
                                            sender.sendMessage(Variable.Lang_YML.getString("HeadLineTtitle"));
                                            sender.sendMessage(temp);
                                            sender.sendMessage(Variable.Lang_YML.getString("BottomLineTtitle"));
                                        }
                                    }
                                } else {
                                    String temp = Variable.Lang_YML.getString("NoOwnerPermission");
                                    sender.sendMessage(Variable.Lang_YML.getString("BottomLineTtitle"));
                                    sender.sendMessage(temp);
                                    sender.sendMessage(Variable.Lang_YML.getString("BottomLineTtitle"));
                                }

                                return false;
                            }
                        }

                        if (args.length == 2 && args[0].equalsIgnoreCase("GAMEMODE")) {
                            if (!Util.CheckOwnerAndManagerAndOP(p, p.getWorld().getName().replace(Variable.world_prefix, ""))) {
                                String temp = Variable.Lang_YML.getString("NoOwnerAndManagerPermission");
                                sender.sendMessage(Variable.Lang_YML.getString("HeadLineTtitle"));
                                sender.sendMessage(temp);
                                sender.sendMessage(Variable.Lang_YML.getString("BottomLineTtitle"));
                                return false;
                            }

                            if (args[1].equalsIgnoreCase("EASY")) {
                                if (!p.hasPermission("SelfHome.GAMEMODE.EASY")) {
                                    String tip = Variable.Lang_YML.getString("NoPermissionCheck");
                                    if (tip.contains("<Permission>")) {
                                        tip = tip.replace("<Permission>", "SelfHome.GAMEMODE.EASY");
                                    }

                                    p.sendMessage(tip);
                                    return false;
                                }

                                p.getWorld().setDifficulty(Difficulty.EASY);
                                String temp = Variable.Lang_YML.getString("DifficultyModify");
                                if (temp.contains("<Mode>")) {
                                    temp = temp.replace("<Mode>", "EASY");
                                }

                                p.sendMessage(temp);
                                return false;
                            }

                            if (args[1].equalsIgnoreCase("HARD")) {
                                if (!p.hasPermission("SelfHome.GAMEMODE.HARD")) {
                                    String tip = Variable.Lang_YML.getString("NoPermissionCheck");
                                    if (tip.contains("<Permission>")) {
                                        tip = tip.replace("<Permission>", "SelfHome.GAMEMODE.HARD");
                                    }

                                    p.sendMessage(tip);
                                    return false;
                                }

                                p.getWorld().setDifficulty(Difficulty.HARD);
                                String temp = Variable.Lang_YML.getString("DifficultyModify");
                                if (temp.contains("<Mode>")) {
                                    temp = temp.replace("<Mode>", "HARD");
                                }

                                p.sendMessage(temp);
                                return false;
                            }

                            if (args[1].equalsIgnoreCase("PEACEFUL")) {
                                if (!p.hasPermission("SelfHome.GAMEMODE.PEACEFUL")) {
                                    String tip = Variable.Lang_YML.getString("NoPermissionCheck");
                                    if (tip.contains("<Permission>")) {
                                        tip = tip.replace("<Permission>", "SelfHome.GAMEMODE.PEACEFUL");
                                    }

                                    p.sendMessage(tip);
                                    return false;
                                }

                                p.getWorld().setDifficulty(Difficulty.PEACEFUL);
                                String temp = Variable.Lang_YML.getString("DifficultyModify");
                                if (temp.contains("<Mode>")) {
                                    temp = temp.replace("<Mode>", "PEACEFUL");
                                }

                                p.sendMessage(temp);
                                return false;
                            }
                        }

                        if (args.length != 2 || !args[0].equalsIgnoreCase("tp") && !args[0].equalsIgnoreCase("visit") && !args[0].equalsIgnoreCase("v")) {
                            if (args.length != 2 || !args[0].equalsIgnoreCase("invite") && !args[0].equalsIgnoreCase("i")) {
                                if (args.length == 1 && args[0].equalsIgnoreCase("accept")) {
                                    if (Variable.bungee) {
                                        if (!Variable.invite_list.containsValue(p.getName())) {
                                            String temp = Variable.Lang_YML.getString("HasNoOthersInvite");
                                            sender.sendMessage(Variable.Lang_YML.getString("HeadLineTtitle"));
                                            sender.sendMessage(temp);
                                            sender.sendMessage(Variable.Lang_YML.getString("BottomLineTtitle"));
                                            return false;
                                        }

                                        String who_invite = "";

                                        for (String Key : Variable.invite_list.keySet()) {
                                            if (Variable.invite_list.get(Key).equalsIgnoreCase(p.getName())) {
                                                who_invite = Key;
                                                break;
                                            }
                                        }

                                        if (!Util.CheckIsHome(who_invite)) {
                                            String temp = Variable.Lang_YML.getString("InviteAcceptNoExistHome");
                                            sender.sendMessage(Variable.Lang_YML.getString("HeadLineTtitle"));
                                            sender.sendMessage(temp);
                                            sender.sendMessage(Variable.Lang_YML.getString("BottomLineTtitle"));
                                            return false;
                                        }

                                        List<String> OP = MySQL.getOP(who_invite);
                                        String result = MySQL.getListStringSpiltByDot(OP);
                                        if (result != null && !result.equalsIgnoreCase("")) {
                                            result = String.valueOf(result) + "," + p.getName();
                                        } else {
                                            result = p.getName();
                                        }

                                        MySQL.setOP(who_invite, result);
                                        String temp2 = Variable.Lang_YML.getString("SuccessJoinOthers");
                                        if (temp2.contains("<Name>")) {
                                            temp2 = temp2.replace("<Name>", who_invite);
                                        }

                                        sender.sendMessage(Variable.Lang_YML.getString("HeadLineTtitle"));
                                        sender.sendMessage(temp2);
                                        sender.sendMessage(Variable.Lang_YML.getString("BottomLineTtitle"));
                                        Player who_invite_player = Bukkit.getPlayer(who_invite);
                                        if (who_invite_player != null) {
                                            String temp = Variable.Lang_YML.getString("SuccessInviteOther");
                                            if (temp.contains("<Name>")) {
                                                temp = temp.replace("<Name>", p.getName());
                                            }

                                            who_invite_player.sendMessage("§a§l§m--------------" + Variable.Prefix + "§a§l§m--------------");
                                            who_invite_player.sendMessage(temp);
                                            who_invite_player.sendMessage("§a§l§m--------------" + Variable.Prefix + "§a§l§m--------------");
                                        }

                                        Variable.invite_list.remove(who_invite);
                                    } else {
                                        if (!Variable.invite_list.containsValue(p.getName())) {
                                            String temp = Variable.Lang_YML.getString("HasNoOthersInvite");
                                            sender.sendMessage(Variable.Lang_YML.getString("HeadLineTtitle"));
                                            sender.sendMessage(temp);
                                            sender.sendMessage(Variable.Lang_YML.getString("BottomLineTtitle"));
                                            return false;
                                        }

                                        String who_invite = "";

                                        for (String Key : Variable.invite_list.keySet()) {
                                            if (Variable.invite_list.get(Key).equalsIgnoreCase(p.getName())) {
                                                who_invite = Key;
                                                break;
                                            }
                                        }

                                        File f2 = new File(Variable.Tempf, String.valueOf(who_invite) + ".yml");
                                        if (!f2.exists()) {
                                            String temp = Variable.Lang_YML.getString("InviteAcceptNoExistHome");
                                            sender.sendMessage(Variable.Lang_YML.getString("HeadLineTtitle"));
                                            sender.sendMessage(temp);
                                            sender.sendMessage(Variable.Lang_YML.getString("BottomLineTtitle"));
                                            return false;
                                        }

                                        YamlConfiguration yamlConfiguration = YamlConfiguration.loadConfiguration(f2);
                                        List<String> OP = yamlConfiguration.getStringList("OP");
                                        if (OP == null) {
                                            OP = new ArrayList<>();
                                        }

                                        OP.add(p.getName());
                                        yamlConfiguration.set("OP", OP);

                                        try {
                                            yamlConfiguration.save(f2);
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        }

                                        String temp2 = Variable.Lang_YML.getString("SuccessJoinOthers");
                                        if (temp2.contains("<Name>")) {
                                            temp2 = temp2.replace("<Name>", who_invite);
                                        }

                                        sender.sendMessage(Variable.Lang_YML.getString("HeadLineTtitle"));
                                        sender.sendMessage(temp2);
                                        sender.sendMessage(Variable.Lang_YML.getString("BottomLineTtitle"));
                                        Player who_invite_player = Bukkit.getPlayer(who_invite);
                                        if (who_invite_player != null) {
                                            String temp = Variable.Lang_YML.getString("SuccessInviteOther");
                                            if (temp.contains("<Name>")) {
                                                temp = temp.replace("<Name>", p.getName());
                                            }

                                            who_invite_player.sendMessage("§a§l§m--------------" + Variable.Prefix + "§a§l§m--------------");
                                            who_invite_player.sendMessage(temp);
                                            who_invite_player.sendMessage("§a§l§m--------------" + Variable.Prefix + "§a§l§m--------------");
                                        }

                                        Variable.invite_list.remove(who_invite);
                                    }

                                    return false;
                                } else if (args.length == 1 && args[0].equalsIgnoreCase("locktime")) {
                                    if (!Main.JavaPlugin.getConfig().getBoolean("EnableTimeLock")) {
                                        sender.sendMessage("this function is disabled by config.yml");
                                        return false;
                                    }

                                    File f2 = new File(Variable.Tempf, String.valueOf(p.getWorld().getName().replace(Variable.world_prefix, "")) + ".yml");
                                    if (Util.CheckOwnerAndManagerAndOP(p, p.getWorld().getName().replace(Variable.world_prefix, ""))) {
                                        if (!Main.JavaPlugin.getConfig().getBoolean("Permission.LockTime") && !p.hasPermission("SelfHome.locktime")) {
                                            String temp = Variable.Lang_YML.getString("NoPermissionLockTime");
                                            if (temp.contains("<Permission>")) {
                                                temp = temp.replace("<Permission>", "SelfHome.LockTime");
                                            }

                                            sender.sendMessage(Variable.Lang_YML.getString("HeadLineTtitle"));
                                            sender.sendMessage(temp);
                                            sender.sendMessage(Variable.Lang_YML.getString("BottomLineTtitle"));
                                            return false;
                                        }

                                        if (Variable.bungee) {
                                            if (MySQL.getlocktime(p.getWorld().getName().replace(Variable.world_prefix, "")).equalsIgnoreCase("true")) {
                                                MySQL.setlocktime(p.getWorld().getName().replace(Variable.world_prefix, ""), "false");
                                                String temp = Variable.Lang_YML.getString("TimeUnLocked");
                                                sender.sendMessage(Variable.Lang_YML.getString("HeadLineTtitle"));
                                                sender.sendMessage(temp);
                                                sender.sendMessage(Variable.Lang_YML.getString("BottomLineTtitle"));
                                            } else {
                                                MySQL.setlocktime(p.getWorld().getName().replace(Variable.world_prefix, ""), "true");
                                                MySQL.settime(p.getWorld().getName().replace(Variable.world_prefix, ""), String.valueOf(p.getWorld().getTime()));
                                                String temp = Variable.Lang_YML.getString("TimeLocked");
                                                sender.sendMessage(Variable.Lang_YML.getString("HeadLineTtitle"));
                                                sender.sendMessage(temp);
                                                sender.sendMessage(Variable.Lang_YML.getString("BottomLineTtitle"));
                                            }
                                        } else {
                                            YamlConfiguration yamlConfiguration = YamlConfiguration.loadConfiguration(f2);
                                            if (yamlConfiguration.getBoolean("locktime")) {
                                                yamlConfiguration.set("locktime", false);

                                                try {
                                                    yamlConfiguration.save(f2);
                                                } catch (Exception e) {
                                                    e.printStackTrace();
                                                }

                                                String temp = Variable.Lang_YML.getString("TimeUnLocked");
                                                sender.sendMessage(Variable.Lang_YML.getString("HeadLineTtitle"));
                                                sender.sendMessage(temp);
                                                sender.sendMessage(Variable.Lang_YML.getString("BottomLineTtitle"));
                                            } else {
                                                yamlConfiguration.set("locktime", true);
                                                yamlConfiguration.set("time", p.getWorld().getTime());

                                                try {
                                                    yamlConfiguration.save(f2);
                                                } catch (Exception e) {
                                                    e.printStackTrace();
                                                }

                                                String temp = Variable.Lang_YML.getString("TimeLocked");
                                                sender.sendMessage(Variable.Lang_YML.getString("HeadLineTtitle"));
                                                sender.sendMessage(temp);
                                                sender.sendMessage(Variable.Lang_YML.getString("BottomLineTtitle"));
                                            }
                                        }
                                    } else {
                                        String temp = Variable.Lang_YML.getString("NoOwnerAndManagerPermission");
                                        sender.sendMessage(Variable.Lang_YML.getString("HeadLineTtitle"));
                                        sender.sendMessage(temp);
                                        sender.sendMessage(Variable.Lang_YML.getString("BottomLineTtitle"));
                                    }

                                    return false;
                                } else if (args.length == 1 && args[0].equalsIgnoreCase("lockweather")) {
                                    File f2 = new File(Variable.Tempf, String.valueOf(p.getWorld().getName().replace(Variable.world_prefix, "")) + ".yml");
                                    if (Util.CheckOwnerAndManagerAndOP(p, p.getWorld().getName().replace(Variable.world_prefix, ""))) {
                                        String Language = Variable.Lang_YML.getString("NoPermissionCheck");
                                        if (Language.contains("<Permission>")) {
                                            Language = Language.replace("<Permission>", "SelfHome.lockWeather");
                                        }

                                        if (!Main.JavaPlugin.getConfig().getBoolean("Permission.LockWeather") && !p.hasPermission("SelfHome.lockweather")) {
                                            sender.sendMessage(Variable.Lang_YML.getString("HeadLineTtitle"));
                                            sender.sendMessage(Language);
                                            sender.sendMessage(Variable.Lang_YML.getString("BottomLineTtitle"));
                                            return false;
                                        }

                                        if (Variable.bungee) {
                                            if (MySQL.getlockweather(p.getWorld().getName().replace(Variable.world_prefix, "")).equalsIgnoreCase("true")) {
                                                MySQL.setlockweather(p.getWorld().getName().replace(Variable.world_prefix, ""), "false");
                                                String temp = Variable.Lang_YML.getString("WeatherUnLocked");
                                                sender.sendMessage(Variable.Lang_YML.getString("HeadLineTtitle"));
                                                sender.sendMessage(temp);
                                                sender.sendMessage(Variable.Lang_YML.getString("BottomLineTtitle"));
                                            } else {
                                                MySQL.setlockweather(p.getWorld().getName().replace(Variable.world_prefix, ""), "true");
                                                String temp = Variable.Lang_YML.getString("WeatherLocked");
                                                sender.sendMessage(Variable.Lang_YML.getString("HeadLineTtitle"));
                                                sender.sendMessage(temp);
                                                sender.sendMessage(Variable.Lang_YML.getString("BottomLineTtitle"));
                                            }
                                        } else {
                                            YamlConfiguration yamlConfiguration = YamlConfiguration.loadConfiguration(f2);
                                            if (yamlConfiguration.getBoolean("lockweather")) {
                                                yamlConfiguration.set("lockweather", false);

                                                try {
                                                    yamlConfiguration.save(f2);
                                                } catch (Exception e) {
                                                    e.printStackTrace();
                                                }

                                                String temp = Variable.Lang_YML.getString("WeatherUnLocked");
                                                sender.sendMessage(Variable.Lang_YML.getString("HeadLineTtitle"));
                                                sender.sendMessage(temp);
                                                sender.sendMessage(Variable.Lang_YML.getString("BottomLineTtitle"));
                                            } else {
                                                yamlConfiguration.set("lockweather", true);

                                                try {
                                                    yamlConfiguration.save(f2);
                                                } catch (Exception e) {
                                                    e.printStackTrace();
                                                }

                                                String temp = Variable.Lang_YML.getString("WeatherLocked");
                                                sender.sendMessage(Variable.Lang_YML.getString("HeadLineTtitle"));
                                                sender.sendMessage(temp);
                                                sender.sendMessage(Variable.Lang_YML.getString("BottomLineTtitle"));
                                            }
                                        }
                                    } else {
                                        String temp = Variable.Lang_YML.getString("NoOwnerPermission");
                                        sender.sendMessage(Variable.Lang_YML.getString("HeadLineTtitle"));
                                        sender.sendMessage(temp);
                                        sender.sendMessage(Variable.Lang_YML.getString("BottomLineTtitle"));
                                    }

                                    return false;
                                } else if (args.length == 1 && args[0].equalsIgnoreCase("day")) {
                                    if (Util.CheckOwnerAndManagerAndOP(p, p.getWorld().getName().replace(Variable.world_prefix, ""))) {
                                        String Language = Variable.Lang_YML.getString("NoPermissionCheck");
                                        if (Language.contains("<Permission>")) {
                                            Language = Language.replace("<Permission>", "SelfHome.Day");
                                        }

                                        if (!Main.JavaPlugin.getConfig().getBoolean("Permission.Day") && !p.hasPermission("SelfHome.Day")) {
                                            sender.sendMessage(Variable.Lang_YML.getString("HeadLineTtitle"));
                                            sender.sendMessage(Language);
                                            sender.sendMessage(Variable.Lang_YML.getString("BottomLineTtitle"));
                                            return false;
                                        }

                                        if (HomeAPI.getHome(p.getWorld().getName()).isLocktime()) {
                                            try {
                                                HomeAPI.getHome(p.getWorld().getName()).setLocktime(false);
                                            } catch (Exception e) {
                                                e.printStackTrace();
                                            }
                                        }

                                        p.sendMessage(Variable.Lang_YML.getString("TimeDay"));
                                        p.getWorld().setTime(0L);
                                    } else {
                                        String temp = Variable.Lang_YML.getString("NoOwnerAndManagerPermission");
                                        sender.sendMessage(Variable.Lang_YML.getString("HeadLineTtitle"));
                                        sender.sendMessage(temp);
                                        sender.sendMessage(Variable.Lang_YML.getString("BottomLineTtitle"));
                                    }

                                    return false;
                                } else if (args.length == 1 && args[0].equalsIgnoreCase("sun")) {
                                    if (Util.CheckOwnerAndManagerAndOP(p, p.getWorld().getName().replace(Variable.world_prefix, ""))) {
                                        String Language = Variable.Lang_YML.getString("NoPermissionCheck");
                                        if (Language.contains("<Permission>")) {
                                            Language = Language.replace("<Permission>", "SelfHome.Sun");
                                        }

                                        if (!Main.JavaPlugin.getConfig().getBoolean("Permission.Sun") && !p.hasPermission("SelfHome.Sun")) {
                                            sender.sendMessage(Variable.Lang_YML.getString("HeadLineTtitle"));
                                            sender.sendMessage(Language);
                                            sender.sendMessage(Variable.Lang_YML.getString("BottomLineTtitle"));
                                            return false;
                                        }

                                        if (HomeAPI.getHome(p.getWorld().getName()).isLockweather()) {
                                            try {
                                                HomeAPI.getHome(p.getWorld().getName()).setLockweather(false);
                                            } catch (Exception e) {
                                                e.printStackTrace();
                                            }
                                        }

                                        p.sendMessage(Variable.Lang_YML.getString("WeatherSun"));
                                        p.getWorld().setStorm(false);
                                        p.getWorld().setThundering(false);
                                    } else {
                                        String temp = Variable.Lang_YML.getString("NoOwnerAndManagerPermission");
                                        sender.sendMessage(Variable.Lang_YML.getString("HeadLineTtitle"));
                                        sender.sendMessage(temp);
                                        sender.sendMessage(Variable.Lang_YML.getString("BottomLineTtitle"));
                                    }

                                    return false;
                                } else if (args.length == 1 && args[0].equalsIgnoreCase("togglecc")) {
                                    if (Util.CheckOwnerAndManagerAndOP(p, p.getWorld().getName().replace(Variable.world_prefix, ""))) {
                                        String Language = Variable.Lang_YML.getString("NoPermissionCheck");
                                        if (Language.contains("<Permission>")) {
                                            Language = Language.replace("<Permission>", "SelfHome.Togglecc");
                                        }

                                        if (!p.hasPermission("SelfHome.Togglecc")) {
                                            sender.sendMessage(Variable.Lang_YML.getString("HeadLineTtitle"));
                                            sender.sendMessage(Language);
                                            sender.sendMessage(Variable.Lang_YML.getString("BottomLineTtitle"));
                                            return false;
                                        }

                                    } else {
                                        String temp = Variable.Lang_YML.getString("NoOwnerAndManagerPermission");
                                        sender.sendMessage(Variable.Lang_YML.getString("HeadLineTtitle"));
                                        sender.sendMessage(temp);
                                        sender.sendMessage(Variable.Lang_YML.getString("BottomLineTtitle"));
                                    }

                                    return false;
                                } else if (args.length == 1 && args[0].equalsIgnoreCase("nether")) {
                                    if (!Main.JavaPlugin.getConfig().getBoolean("EnableNetherTeleport")) {
                                        p.sendMessage(Variable.Lang_YML.getString("NoOpenNetherTeleport"));
                                        return false;
                                    }

                                    String Language = Variable.Lang_YML.getString("NoPermissionCheck");
                                    if (Language.contains("<Permission>")) {
                                        Language = Language.replace("<Permission>", "SelfHome.Nether");
                                    }

                                    if (!Main.JavaPlugin.getConfig().getBoolean("Permission.Nether") && !p.hasPermission("SelfHome.Nether")) {
                                        sender.sendMessage(Variable.Lang_YML.getString("HeadLineTtitle"));
                                        sender.sendMessage(Language);
                                        sender.sendMessage(Variable.Lang_YML.getString("BottomLineTtitle"));
                                        return false;
                                    }

                                    World world = Bukkit.getWorld(Main.JavaPlugin.getConfig().getString("NetherWorldName"));
                                    if (world == null) {
                                        WorldCreator creator = new WorldCreator(Main.JavaPlugin.getConfig().getString("NetherWorldName"));
                                        Variable.create_list_home.add(Main.JavaPlugin.getConfig().getString("NetherWorldName"));
                                        Bukkit.createWorld(creator);
                                    }

                                    world = Bukkit.getWorld(Main.JavaPlugin.getConfig().getString("NetherWorldName"));

                                    for (int i = 0; i < Main.JavaPlugin.getConfig().getStringList("NeitherGameRules").size(); i++) {
                                        String[] temp = ((String)Main.JavaPlugin.getConfig().getStringList("NeitherGameRules").get(i)).split(",");
                                        world.setGameRuleValue(temp[0], temp[1]);
                                    }

                                    p.teleport(world.getSpawnLocation());
                                    return false;
                                } else if (args.length == 1 && args[0].equalsIgnoreCase("end")) {
                                    if (!Main.JavaPlugin.getConfig().getBoolean("EnableEndTeleport")) {
                                        p.sendMessage(Variable.Lang_YML.getString("NoOpenEndTeleport"));
                                        return false;
                                    }

                                    String Language = Variable.Lang_YML.getString("NoPermissionCheck");
                                    if (Language.contains("<Permission>")) {
                                        Language = Language.replace("<Permission>", "SelfHome.End");
                                    }

                                    if (!Main.JavaPlugin.getConfig().getBoolean("Permission.End") && !p.hasPermission("SelfHome.End")) {
                                        sender.sendMessage(Variable.Lang_YML.getString("HeadLineTtitle"));
                                        sender.sendMessage(Language);
                                        sender.sendMessage(Variable.Lang_YML.getString("BottomLineTtitle"));
                                        return false;
                                    }

                                    World world = Bukkit.getWorld(Main.JavaPlugin.getConfig().getString("EndWorldName"));
                                    if (world == null) {
                                        WorldCreator creator = new WorldCreator(Main.JavaPlugin.getConfig().getString("EndWorldName"));
                                        Variable.create_list_home.add(Main.JavaPlugin.getConfig().getString("EndWorldName"));
                                        Bukkit.createWorld(creator);
                                    }

                                    world = Bukkit.getWorld(Main.JavaPlugin.getConfig().getString("EndWorldName"));

                                    for (int i = 0; i < Main.JavaPlugin.getConfig().getStringList("EndGameRules").size(); i++) {
                                        String[] temp = ((String)Main.JavaPlugin.getConfig().getStringList("EndGameRules").get(i)).split(",");
                                        world.setGameRuleValue(temp[0], temp[1]);
                                    }

                                    p.teleport(world.getSpawnLocation());
                                    return false;
                                } else if (args.length == 1 && args[0].equalsIgnoreCase("rain")) {
                                    if (Util.CheckOwnerAndManagerAndOP(p, p.getWorld().getName().replace(Variable.world_prefix, ""))) {
                                        String Language = Variable.Lang_YML.getString("NoPermissionCheck");
                                        if (Language.contains("<Permission>")) {
                                            Language = Language.replace("<Permission>", "SelfHome.Rain");
                                        }

                                        if (!Main.JavaPlugin.getConfig().getBoolean("Permission.Rain") && !p.hasPermission("SelfHome.Rain")) {
                                            sender.sendMessage(Variable.Lang_YML.getString("HeadLineTtitle"));
                                            sender.sendMessage(Language);
                                            sender.sendMessage(Variable.Lang_YML.getString("BottomLineTtitle"));
                                            return false;
                                        }

                                        if (HomeAPI.getHome(p.getWorld().getName()).isLockweather()) {
                                            try {
                                                HomeAPI.getHome(p.getWorld().getName()).setLockweather(false);
                                            } catch (Exception e) {
                                                e.printStackTrace();
                                            }
                                        }

                                        p.sendMessage(Variable.Lang_YML.getString("WeatherRain"));
                                        p.getWorld().setStorm(true);
                                    } else {
                                        String temp = Variable.Lang_YML.getString("NoOwnerAndManagerPermission");
                                        sender.sendMessage(Variable.Lang_YML.getString("HeadLineTtitle"));
                                        sender.sendMessage(temp);
                                        sender.sendMessage(Variable.Lang_YML.getString("BottomLineTtitle"));
                                    }

                                    return false;
                                } else if (args.length == 1 && args[0].equalsIgnoreCase("seed")) {
                                    if (Util.CheckOwnerAndManagerAndOP(p, p.getWorld().getName().replace(Variable.world_prefix, ""))) {
                                        String Language = Variable.Lang_YML.getString("NoPermissionCheck");
                                        if (Language.contains("<Permission>")) {
                                            Language = Language.replace("<Permission>", "SelfHome.Seed");
                                        }

                                        if (!p.hasPermission("SelfHome.Seed")) {
                                            sender.sendMessage(Variable.Lang_YML.getString("HeadLineTtitle"));
                                            sender.sendMessage(Language);
                                            sender.sendMessage(Variable.Lang_YML.getString("BottomLineTtitle"));
                                            return false;
                                        }

                                        String message = Variable.Lang_YML.getString("LookSeed");
                                        if (message.contains("<Seed>")) {
                                            message = message.replace("<Seed>", String.valueOf(p.getWorld().getSeed()));
                                        }

                                        p.sendMessage(message);
                                    } else {
                                        String temp = Variable.Lang_YML.getString("NoOwnerAndManagerPermission");
                                        sender.sendMessage(Variable.Lang_YML.getString("HeadLineTtitle"));
                                        sender.sendMessage(temp);
                                        sender.sendMessage(Variable.Lang_YML.getString("BottomLineTtitle"));
                                    }

                                    return false;
                                } else if (args.length == 2 && args[0].equalsIgnoreCase("fly")) {
                                    if (Util.CheckOwnerAndManagerAndOP(p, p.getWorld().getName().replace(Variable.world_prefix, ""))) {
                                        String Language = Variable.Lang_YML.getString("NoPermissionCheck");
                                        if (Language.contains("<Permission>")) {
                                            Language = Language.replace("<Permission>", "SelfHome.Fly");
                                        }

                                        if (!p.hasPermission("SelfHome.Fly")) {
                                            sender.sendMessage(Variable.Lang_YML.getString("HeadLineTtitle"));
                                            sender.sendMessage(Language);
                                            sender.sendMessage(Variable.Lang_YML.getString("BottomLineTtitle"));
                                            return false;
                                        }

                                        if (args[1].equalsIgnoreCase("off")) {
                                            for (Player e : p.getWorld().getPlayers()) {
                                                String message = Variable.Lang_YML.getString("DisableFly");
                                                e.sendMessage(message);
                                                if (Variable.flying_list.containsKey(e.getName())) {
                                                    Variable.flying_list.remove(e.getName());
                                                }

                                                if (e.getAllowFlight()) {
                                                    e.setAllowFlight(false);
                                                }
                                            }
                                        } else {
                                            for (Player e : p.getWorld().getPlayers()) {
                                                String message = Variable.Lang_YML.getString("EnableFly");
                                                e.sendMessage(message);
                                                if (!e.getAllowFlight()) {
                                                    if (!Variable.flying_list.containsKey(e.getName())) {
                                                        Variable.flying_list.put(e.getName(), p.getWorld().getName());
                                                    }

                                                    e.setAllowFlight(true);
                                                }
                                            }
                                        }
                                    } else {
                                        String temp = Variable.Lang_YML.getString("NoOwnerAndManagerPermission");
                                        sender.sendMessage(Variable.Lang_YML.getString("HeadLineTtitle"));
                                        sender.sendMessage(temp);
                                        sender.sendMessage(Variable.Lang_YML.getString("BottomLineTtitle"));
                                    }

                                    return false;
                                } else if (args.length == 1 && args[0].equalsIgnoreCase("night")) {
                                    if (Util.CheckOwnerAndManagerAndOP(p, p.getWorld().getName().replace(Variable.world_prefix, ""))) {
                                        String temp = Variable.Lang_YML.getString("NoNightPermission");
                                        if (temp.contains("<Permission>")) {
                                            temp = temp.replace("<Permission>", "SelfHome.Night");
                                        }

                                        if (!Main.JavaPlugin.getConfig().getBoolean("Permission.Night") && !p.hasPermission("SelfHome.night")) {
                                            sender.sendMessage(Variable.Lang_YML.getString("HeadLineTtitle"));
                                            sender.sendMessage(temp);
                                            sender.sendMessage(Variable.Lang_YML.getString("BottomLineTtitle"));
                                            return false;
                                        }

                                        if (HomeAPI.getHome(p.getWorld().getName()).isLocktime()) {
                                            try {
                                                HomeAPI.getHome(p.getWorld().getName()).setLocktime(false);
                                            } catch (Exception e) {
                                                e.printStackTrace();
                                            }
                                        }

                                        p.sendMessage(Variable.Lang_YML.getString("TimeNight"));
                                        p.getWorld().setTime(14000L);
                                    } else {
                                        String temp = Variable.Lang_YML.getString("NoOwnerAndManagerPermission");
                                        sender.sendMessage(Variable.Lang_YML.getString("HeadLineTtitle"));
                                        sender.sendMessage(temp);
                                        sender.sendMessage(Variable.Lang_YML.getString("BottomLineTtitle"));
                                    }

                                    return false;
                                } else if (args.length != 2
                                    || !args[0].equalsIgnoreCase("trust") && !args[0].equalsIgnoreCase("t") && !args[0].equalsIgnoreCase("add")) {
                                    if (args.length == 2 && args[0].equalsIgnoreCase("Deny")) {
                                        String Name = args[1];
                                        if (!Main.JavaPlugin.getConfig().getBoolean("Permission.CommandUser")
                                            && !p.hasPermission("SelfHome.Deny")
                                            && !p.hasPermission("SelfHome.command.user")) {
                                            String tip = Variable.Lang_YML.getString("NoPermissionCheck");
                                            if (tip.contains("<Permission>")) {
                                                tip = tip.replace("<Permission>", "SelfHome.Deny");
                                            }

                                            p.sendMessage(tip);
                                            return false;
                                        } else {
                                            File f2 = new File(
                                                Variable.Tempf, String.valueOf(p.getWorld().getName().replace(Variable.world_prefix, "")) + ".yml"
                                            );
                                            if (!Util.CheckOwnerAndManagerAndOP(p, p.getWorld().getName().replace(Variable.world_prefix, ""))) {
                                                String temp = Variable.Lang_YML.getString("NoOwnerPermission");
                                                sender.sendMessage(Variable.Lang_YML.getString("HeadLineTtitle"));
                                                sender.sendMessage(temp);
                                                sender.sendMessage(Variable.Lang_YML.getString("BottomLineTtitle"));
                                                return false;
                                            }

                                            if (args[1].equalsIgnoreCase(p.getName())) {
                                                String temp = Variable.Lang_YML.getString("AddOwnerToBlack");
                                                sender.sendMessage(Variable.Lang_YML.getString("HeadLineTtitle"));
                                                sender.sendMessage(temp);
                                                sender.sendMessage(Variable.Lang_YML.getString("BottomLineTtitle"));
                                                return false;
                                            }

                                            if (Variable.bungee) {
                                                List<String> trustlist = MySQL.getMembers(p.getWorld().getName().replace(Variable.world_prefix, ""));
                                                if (trustlist == null) {
                                                    trustlist = new ArrayList<>();
                                                }

                                                for (int i = 0; i < trustlist.size(); i++) {
                                                    if (trustlist.get(i).equalsIgnoreCase(args[1])) {
                                                        String temp = Variable.Lang_YML.getString("HasInTrust");
                                                        sender.sendMessage(Variable.Lang_YML.getString("HeadLineTtitle"));
                                                        sender.sendMessage(temp);
                                                        sender.sendMessage(Variable.Lang_YML.getString("BottomLineTtitle"));
                                                        return false;
                                                    }
                                                }

                                                List<String> oplist = MySQL.getOP(p.getWorld().getName().replace(Variable.world_prefix, ""));
                                                if (oplist == null) {
                                                    oplist = new ArrayList<>();
                                                }

                                                for (int i = 0; i < oplist.size(); i++) {
                                                    if (oplist.get(i).equalsIgnoreCase(args[1])) {
                                                        String temp = Variable.Lang_YML.getString("HasInManager");
                                                        sender.sendMessage(Variable.Lang_YML.getString("HeadLineTtitle"));
                                                        sender.sendMessage(temp);
                                                        sender.sendMessage(Variable.Lang_YML.getString("BottomLineTtitle"));
                                                        return false;
                                                    }
                                                }

                                                List<String> save = MySQL.getDenys(p.getWorld().getName().replace(Variable.world_prefix, ""));
                                                if (save == null) {
                                                    save = new ArrayList<>();
                                                }

                                                Boolean CheckSame = false;

                                                for (int i = 0; i < save.size(); i++) {
                                                    if (save.get(i).equalsIgnoreCase(Name)) {
                                                        CheckSame = true;
                                                        String temp = Variable.Lang_YML.getString("HasAlreadyExistBlack");
                                                        sender.sendMessage(Variable.Lang_YML.getString("HeadLineTtitle"));
                                                        sender.sendMessage(temp);
                                                        sender.sendMessage(Variable.Lang_YML.getString("BottomLineTtitle"));
                                                        return false;
                                                    }
                                                }

                                                if (!CheckSame) {
                                                    String result = MySQL.getListStringSpiltByDot(
                                                        MySQL.getDenys(p.getWorld().getName().replace(Variable.world_prefix, ""))
                                                    );
                                                    if (result != null && !result.equalsIgnoreCase("")) {
                                                        result = String.valueOf(result) + "," + Name;
                                                    } else {
                                                        result = Name;
                                                    }

                                                    MySQL.setDenys(p.getWorld().getName().replace(Variable.world_prefix, ""), result);

                                                    for (Player pt : Bukkit.getWorld(Variable.world_prefix + p.getName()).getPlayers()) {
                                                        if (pt.getName().equalsIgnoreCase(Name)) {
                                                            String temp = Variable.Lang_YML.getString("BeKicked");
                                                            pt.sendMessage("§a§l§m--------------" + Variable.Prefix + "§a§l§m--------------");
                                                            pt.sendMessage(temp);
                                                            pt.sendMessage("§a§l§m--------------" + Variable.Prefix + "§a§l§m--------------");
                                                            String bekicked = Main.JavaPlugin.getConfig().getString("BeKickedCommand");
                                                            if (bekicked.contains("<Name>")) {
                                                                bekicked = bekicked.replace("<Name>", pt.getName());
                                                            }

                                                            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), bekicked);
                                                        }
                                                    }

                                                    String temp = Variable.Lang_YML.getString("AddBlackSuccess");
                                                    if (temp.contains("<Name>")) {
                                                        temp = temp.replace("<Name>", Name);
                                                    }

                                                    sender.sendMessage(Variable.Lang_YML.getString("HeadLineTtitle"));
                                                    sender.sendMessage(temp);
                                                    sender.sendMessage(Variable.Lang_YML.getString("BottomLineTtitle"));
                                                }
                                            } else {
                                                YamlConfiguration yamlConfiguration = YamlConfiguration.loadConfiguration(f2);
                                                List<String> trustlist = yamlConfiguration.getStringList("Members");
                                                if (trustlist == null) {
                                                    trustlist = new ArrayList<>();
                                                }

                                                for (int i = 0; i < trustlist.size(); i++) {
                                                    if (trustlist.get(i).equalsIgnoreCase(args[1])) {
                                                        String temp = Variable.Lang_YML.getString("HasInTrust");
                                                        sender.sendMessage(Variable.Lang_YML.getString("HeadLineTtitle"));
                                                        sender.sendMessage(temp);
                                                        sender.sendMessage(Variable.Lang_YML.getString("BottomLineTtitle"));
                                                        return false;
                                                    }
                                                }

                                                List<String> oplist = yamlConfiguration.getStringList("OP");
                                                if (oplist == null) {
                                                    oplist = new ArrayList<>();
                                                }

                                                for (int i = 0; i < oplist.size(); i++) {
                                                    if (oplist.get(i).equalsIgnoreCase(args[1])) {
                                                        String temp = Variable.Lang_YML.getString("HasInManager");
                                                        sender.sendMessage(Variable.Lang_YML.getString("HeadLineTtitle"));
                                                        sender.sendMessage(temp);
                                                        sender.sendMessage(Variable.Lang_YML.getString("BottomLineTtitle"));
                                                        return false;
                                                    }
                                                }

                                                List<String> save = yamlConfiguration.getStringList("Denys");
                                                if (save == null) {
                                                    save = new ArrayList<>();
                                                }

                                                Boolean CheckSame = false;

                                                for (int i = 0; i < save.size(); i++) {
                                                    if (save.get(i).equalsIgnoreCase(Name)) {
                                                        CheckSame = true;
                                                        String temp = Variable.Lang_YML.getString("HasAlreadyExistBlack");
                                                        sender.sendMessage(Variable.Lang_YML.getString("HeadLineTtitle"));
                                                        sender.sendMessage(temp);
                                                        sender.sendMessage(Variable.Lang_YML.getString("BottomLineTtitle"));
                                                        return false;
                                                    }
                                                }

                                                if (!CheckSame) {
                                                    save.add(Name);
                                                    yamlConfiguration.set("Denys", save);

                                                    try {
                                                        yamlConfiguration.save(f2);
                                                    } catch (Exception e) {
                                                        e.printStackTrace();
                                                    }

                                                    for (Player pt : Bukkit.getWorld(Variable.world_prefix + p.getName()).getPlayers()) {
                                                        if (pt.getName().equalsIgnoreCase(Name)) {
                                                            String temp = Variable.Lang_YML.getString("BeKicked");
                                                            pt.sendMessage("§a§l§m--------------" + Variable.Prefix + "§a§l§m--------------");
                                                            pt.sendMessage(temp);
                                                            pt.sendMessage("§a§l§m--------------" + Variable.Prefix + "§a§l§m--------------");
                                                            String bekicked = Variable.Lang_YML.getString("BeKickedCommand");
                                                            if (bekicked.contains("<Name>")) {
                                                                bekicked = bekicked.replace("<Name>", pt.getName());
                                                            }

                                                            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), bekicked);
                                                        }
                                                    }

                                                    String temp = Variable.Lang_YML.getString("AddBlackSuccess");
                                                    if (temp.contains("<Name>")) {
                                                        temp = temp.replace("<Name>", Name);
                                                    }

                                                    sender.sendMessage(Variable.Lang_YML.getString("HeadLineTtitle"));
                                                    sender.sendMessage(temp);
                                                    sender.sendMessage(Variable.Lang_YML.getString("BottomLineTtitle"));
                                                }
                                            }

                                            return false;
                                        }
                                    } else if (args.length == 2 && args[0].equalsIgnoreCase("UnDeny")) {
                                        String Name = args[1];
                                        File f2 = new File(Variable.Tempf, String.valueOf(p.getWorld().getName().replace(Variable.world_prefix, "")) + ".yml");
                                        if (!Util.CheckOwnerAndManagerAndOP(p, p.getWorld().getName().replace(Variable.world_prefix, ""))) {
                                            String temp = Variable.Lang_YML.getString("NoOwnerAndManagerPermission");
                                            sender.sendMessage(Variable.Lang_YML.getString("HeadLineTtitle"));
                                            sender.sendMessage(temp);
                                            sender.sendMessage(Variable.Lang_YML.getString("BottomLineTtitle"));
                                            return false;
                                        }

                                        if (Variable.bungee) {
                                            List<String> save = MySQL.getDenys(p.getWorld().getName().replace(Variable.world_prefix, ""));
                                            Boolean Check = false;
                                            if (save == null) {
                                                save = new ArrayList<>();
                                            }

                                            for (int i = 0; i < save.size(); i++) {
                                                if (save.get(i).equalsIgnoreCase(Name)) {
                                                    Check = true;
                                                }
                                            }

                                            if (Check) {
                                                for (int i = 0; i < save.size(); i++) {
                                                    if (save.get(i).equalsIgnoreCase(Name)) {
                                                        String result = MySQL.getListStringSpiltByDot(
                                                            MySQL.getDenys(p.getWorld().getName().replace(Variable.world_prefix, ""))
                                                        );
                                                        result = result.replace("," + save.get(i), "");
                                                        result = result.replace(save.get(i), "");
                                                        MySQL.setDenys(p.getWorld().getName().replace(Variable.world_prefix, ""), result);
                                                        String temp = Variable.Lang_YML.getString("RemoveBlackSuccess");
                                                        if (temp.contains("<Name>")) {
                                                            temp = temp.replace("<Name>", Name);
                                                        }

                                                        sender.sendMessage(Variable.Lang_YML.getString("HeadLineTtitle"));
                                                        sender.sendMessage(temp);
                                                        sender.sendMessage(Variable.Lang_YML.getString("BottomLineTtitle"));
                                                    }
                                                }
                                            } else {
                                                String temp = Variable.Lang_YML.getString("NoBlackExist");
                                                sender.sendMessage(Variable.Lang_YML.getString("HeadLineTtitle"));
                                                sender.sendMessage(temp);
                                                sender.sendMessage(Variable.Lang_YML.getString("BottomLineTtitle"));
                                            }
                                        } else {
                                            YamlConfiguration yamlConfiguration = YamlConfiguration.loadConfiguration(f2);
                                            List<String> save = yamlConfiguration.getStringList("Denys");
                                            Boolean Check = false;
                                            if (save == null) {
                                                save = new ArrayList<>();
                                            }

                                            for (int i = 0; i < save.size(); i++) {
                                                if (save.get(i).equalsIgnoreCase(Name)) {
                                                    Check = true;
                                                }
                                            }

                                            if (Check) {
                                                for (int i = 0; i < save.size(); i++) {
                                                    if (save.get(i).equalsIgnoreCase(Name)) {
                                                        save.remove(i);
                                                        yamlConfiguration.set("Denys", save);

                                                        try {
                                                            yamlConfiguration.save(f2);
                                                        } catch (Exception e) {
                                                            e.printStackTrace();
                                                        }

                                                        String temp = Variable.Lang_YML.getString("RemoveBlackSuccess");
                                                        if (temp.contains("<Name>")) {
                                                            temp = temp.replace("<Name>", Name);
                                                        }

                                                        sender.sendMessage(Variable.Lang_YML.getString("HeadLineTtitle"));
                                                        sender.sendMessage(temp);
                                                        sender.sendMessage(Variable.Lang_YML.getString("BottomLineTtitle"));
                                                    }
                                                }
                                            } else {
                                                String temp = Variable.Lang_YML.getString("NoBlackExist");
                                                sender.sendMessage(Variable.Lang_YML.getString("HeadLineTtitle"));
                                                sender.sendMessage(temp);
                                                sender.sendMessage(Variable.Lang_YML.getString("BottomLineTtitle"));
                                            }
                                        }

                                        return false;
                                    } else if (args.length != 2
                                        || !args[0].equalsIgnoreCase("remove") && !args[0].equalsIgnoreCase("delete") && !args[0].equalsIgnoreCase("del")) {
                                        if (args.length != 1 || !args[0].equalsIgnoreCase("quit") && !args[0].equalsIgnoreCase("q")) {
                                            if (args.length != 2 || !args[0].equalsIgnoreCase("kick") && !args[0].equalsIgnoreCase("k")) {
                                                if (args.length == 1 && args[0].equalsIgnoreCase("create")) {
                                                    if (args.length < 2 && Main.JavaPlugin.getConfig().getString("NormalType").equalsIgnoreCase("0")) {
                                                        sender.sendMessage(Variable.Lang_YML.getString("HeadLineTtitle"));
                                                        String temp = Variable.Lang_YML.getString("CreateHelp");
                                                        sender.sendMessage(temp);
                                                        sender.sendMessage(Variable.Lang_YML.getString("BottomLineTtitle"));
                                                        return false;
                                                    }

                                                    if (args.length == 1 && !Main.JavaPlugin.getConfig().getString("NormalType").equalsIgnoreCase("0")) {
                                                        Bukkit.dispatchCommand(sender, "sh create " + Main.JavaPlugin.getConfig().getString("NormalType"));
                                                    }

                                                    return false;
                                                } else if (args.length == 2 && args[0].equalsIgnoreCase("create")) {
                                                    if (Main.JavaPlugin.getConfig().getBoolean("BungeeCord")) {
                                                        if (MySQL.alreadyhastheplayerjoin(p.getName())) {
                                                            String temp_BungeeCord = Variable.Lang_YML.getString("HasBeenJoin");
                                                            if (temp_BungeeCord.contains("<ServerName>")) {
                                                                temp_BungeeCord = temp_BungeeCord.replace("<ServerName>", MySQL.getJoinServer(p.getName()));
                                                            }

                                                            sender.sendMessage(Variable.Lang_YML.getString("HeadLineTtitle"));
                                                            sender.sendMessage(temp_BungeeCord);
                                                            sender.sendMessage(Variable.Lang_YML.getString("BottomLineTtitle"));
                                                            return false;
                                                        }

                                                        if (MySQL.alreadyhastheplayerhome(p.getName())) {
                                                            String temp_BungeeCord = Variable.Lang_YML.getString("HasBeenCreate");
                                                            if (temp_BungeeCord.contains("<ServerName>")) {
                                                                temp_BungeeCord = temp_BungeeCord.replace("<ServerName>", MySQL.getServer(p.getName()));
                                                            }

                                                            sender.sendMessage(Variable.Lang_YML.getString("HeadLineTtitle"));
                                                            sender.sendMessage(temp_BungeeCord);
                                                            sender.sendMessage(Variable.Lang_YML.getString("BottomLineTtitle"));
                                                            return false;
                                                        }
                                                    } else {
                                                        File f2 = new File(
                                                            Variable.Tempf, String.valueOf(p.getName().replace(Variable.world_prefix, "")) + ".yml"
                                                        );
                                                        if (f2.exists()) {
                                                            sender.sendMessage(Variable.Lang_YML.getString("HeadLineTtitle"));
                                                            String temp3 = Variable.Lang_YML.getString("HasBeenJoin");
                                                            if (temp3.contains("<ServerName>")) {
                                                                temp3 = temp3.replace("<ServerName>", Main.JavaPlugin.getConfig().getString("Server"));
                                                            }

                                                            sender.sendMessage(temp3);
                                                            sender.sendMessage(Variable.Lang_YML.getString("BottomLineTtitle"));
                                                            return false;
                                                        }
                                                    }

                                                    if (Main.JavaPlugin.getConfig().getBoolean("AutoReCreateInLowerLagHome")
                                                        && !Variable.wait_to_command.containsKey(p.getName())
                                                        && Main.JavaPlugin.getConfig().getBoolean("BungeeCord")) {
                                                        if (Main.JavaPlugin.getConfig().getString("DecideBy").equalsIgnoreCase("Player")) {
                                                            if (!MySQL.getLowerstLagServer().equalsIgnoreCase(Main.JavaPlugin.getConfig().getString("Server"))) {
                                                                try {
                                                                    /* excluded Channel */
                                                                } catch (Exception e) {
                                                                    e.printStackTrace();
                                                                }

                                                                p.sendMessage(Variable.Lang_YML.getString("StartLowestLagServer"));
                                                                /* excluded Channel */
                                                                return false;
                                                            }
                                                        } else if (!MySQL.getHighestTPSServer()
                                                            .equalsIgnoreCase(Main.JavaPlugin.getConfig().getString("Server"))) {
                                                            double now = 0.0;
                                                            if (Bukkit.getVersion().contains("1.7.10")) {
                                                                now = Bukkit.getTPS()[0];
                                                            } else {
                                                                double se1 = Double.valueOf(
                                                                    PlaceholderAPI.setPlaceholders(null, "%server_tps_1%").replace("*", "")
                                                                );
                                                                double se2 = Double.valueOf(
                                                                    PlaceholderAPI.setPlaceholders(null, "%server_tps_5%").replace("*", "")
                                                                );
                                                                double se3 = Double.valueOf(
                                                                    PlaceholderAPI.setPlaceholders(null, "%server_tps_15%").replace("*", "")
                                                                );
                                                                now = (se1 + se2 + se3) / 3.0;
                                                            }

                                                            if (MySQL.getServerAmount(MySQL.getLowerstLagServer()) != now) {
                                                                try {
                                                                    /* excluded Channel */
                                                                } catch (Exception e) {
                                                                    e.printStackTrace();
                                                                }

                                                                p.sendMessage(Variable.Lang_YML.getString("StartLowestLagServer"));
                                                                /* excluded Channel */
                                                                return false;
                                                            }
                                                        }
                                                    }

                                                    YamlConfiguration yamlConfiguration = YamlConfiguration.loadConfiguration(Variable.f_log);
                                                    if (!yamlConfiguration.contains("NowID")) {
                                                        yamlConfiguration.set("NowID", 0);
                                                    }

                                                    if (!yamlConfiguration.contains("MaxID")) {
                                                        yamlConfiguration.set("MaxID", 1000);
                                                    }

                                                    try {
                                                        yamlConfiguration.save(Variable.f_log);
                                                    } catch (Exception e) {
                                                        e.printStackTrace();
                                                    }

                                                    int nowID = yamlConfiguration.getInt("NowID");
                                                    int MaxID = yamlConfiguration.getInt("MaxID");
                                                    if (nowID >= MaxID) {
                                                        String temp = Variable.Lang_YML.getString("ReachMaxCreate");
                                                        sender.sendMessage(Variable.Lang_YML.getString("HeadLineTtitle"));
                                                        sender.sendMessage(temp);
                                                        sender.sendMessage(Variable.Lang_YML.getString("BottomLineTtitle"));
                                                        return false;
                                                    }

                                                    String v = args[1];
                                                    if (!Main.JavaPlugin.getConfig().getBoolean("Permission.Create-" + v)
                                                        && !p.hasPermission("SelfHome.Create." + v)
                                                        && !p.hasPermission("SelfHome.Create.*")) {
                                                        String temp = Variable.Lang_YML.getString("NoPermissionCreate");
                                                        if (temp.contains("<Permission>")) {
                                                            temp = temp.replace("<Permission>", "SelfHome.Create." + v);
                                                        }

                                                        sender.sendMessage(Variable.Lang_YML.getString("HeadLineTtitle"));
                                                        sender.sendMessage(temp);
                                                        sender.sendMessage(Variable.Lang_YML.getString("BottomLineTtitle"));
                                                        return false;
                                                    } else {
                                                        if (v.equalsIgnoreCase("1")) {
                                                            WorldCreator creator = null;
                                                            creator = new WorldCreator(Variable.world_prefix + p.getName());
                                                            if (Main.JavaPlugin.getConfig().getLong("Seed") != 0L) {
                                                                creator.seed(Main.JavaPlugin.getConfig().getLong("Seed"));
                                                            }

                                                            if (Main.JavaPlugin.getConfig().getBoolean("generateStructures")) {
                                                                creator.generateStructures(true);
                                                            } else {
                                                                creator.generateStructures(false);
                                                            }

                                                            creator.type(WorldType.NORMAL);
                                                            Variable.create_list_home.add(Variable.world_prefix + p.getName());
                                                            World var793 = Bukkit.createWorld(creator);
                                                        } else if (v.equalsIgnoreCase("2")) {
                                                            WorldCreator creator = null;
                                                            creator = new WorldCreator(Variable.world_prefix + p.getName());
                                                            if (Main.JavaPlugin.getConfig().getLong("Seed") != 0L) {
                                                                creator.seed(Main.JavaPlugin.getConfig().getLong("Seed"));
                                                            }

                                                            if (Main.JavaPlugin.getConfig().getBoolean("generateStructures")) {
                                                                creator.generateStructures(true);
                                                            } else {
                                                                creator.generateStructures(false);
                                                            }

                                                            creator.type(WorldType.FLAT);
                                                            Variable.create_list_home.add(Variable.world_prefix + p.getName());
                                                            World var794 = Bukkit.createWorld(creator);
                                                        } else {
                                                            if (v.equalsIgnoreCase("random")) {
                                                                List<String> list = Main.JavaPlugin.getConfig().getStringList("Random");
                                                                int num = (int)(Math.random() * list.size());
                                                                Bukkit.dispatchCommand(sender, "sh create " + list.get(num));
                                                                return false;
                                                            }

                                                            if (!Main.JavaPlugin.getConfig().getBoolean("Permission.Create-" + v)
                                                                && !p.hasPermission("SelfHome.Create." + v)
                                                                && !p.hasPermission("SelfHome.Create.*")) {
                                                                String temp = Variable.Lang_YML.getString("NoPermissionCreate");
                                                                if (temp.contains("<Permission>")) {
                                                                    temp = temp.replace("<Permission>", "SelfHome.Create." + v);
                                                                }

                                                                sender.sendMessage(Variable.Lang_YML.getString("HeadLineTtitle"));
                                                                sender.sendMessage(temp);
                                                                sender.sendMessage(Variable.Lang_YML.getString("BottomLineTtitle"));
                                                                return false;
                                                            }

                                                            String oldDir = String.valueOf(Variable.worldFinal) + v;
                                                            File newf;
                                                            if (Variable.world_prefix.equalsIgnoreCase("")) {
                                                                if (!Bukkit.getVersion().toString().toUpperCase().contains("ARCLIGHT")
                                                                    && !Bukkit.getVersion().toString().contains("1.20.1")) {
                                                                    newf = new File(Variable.single_server_gen + "world" + Variable.file_loc_prefix);
                                                                } else {
                                                                    newf = new File(Variable.single_server_gen + Variable.world_prefix);
                                                                }
                                                            } else {
                                                                newf = new File(Variable.single_server_gen + Variable.world_prefix);
                                                            }

                                                            String newDir = String.valueOf(newf.getPath().toString()) + Variable.file_loc_prefix + p.getName();
                                                            File exist_file = new File(oldDir);
                                                            if (!exist_file.exists()) {
                                                                String temp = Variable.Lang_YML.getString("WorldFileNotExist");
                                                                if (temp.contains("<name>")) {
                                                                    temp = temp.replace("<name>", v);
                                                                }

                                                                sender.sendMessage(Variable.Lang_YML.getString("HeadLineTtitle"));
                                                                sender.sendMessage(temp);
                                                                sender.sendMessage(Variable.Lang_YML.getString("BottomLineTtitle"));
                                                                return false;
                                                            }

                                                            Util.copyDir(oldDir, newDir);
                                                            WorldCreator creator = null;
                                                            creator = new WorldCreator(Variable.world_prefix + p.getName());
                                                            if (Main.JavaPlugin.getConfig().getBoolean("generateStructures")) {
                                                                creator.generateStructures(true);
                                                            } else {
                                                                creator.generateStructures(false);
                                                            }

                                                            Variable.create_list_home.add(Variable.world_prefix + p.getName());
                                                            World var1030 = Bukkit.createWorld(creator);
                                                        }

                                                        World world = Bukkit.getWorld(Variable.world_prefix + p.getName());
                                                        configureTemplateSpawn(v, world);
                                                        if (Variable.hook_multiverseCore) {
                                                            configureMultiverseWorld(Variable.world_prefix + p.getName());
                                                        }

                                                        if (!Main.JavaPlugin.getConfig().getBoolean("KeepInventory")) {
                                                            world.setGameRuleValue("keepInventory", "false");
                                                        } else if (Main.JavaPlugin.getConfig().getBoolean("KeepInventory")) {
                                                            world.setGameRuleValue("keepInventory", "true");
                                                        }

                                                        if (!Main.JavaPlugin.getConfig().getBoolean("doMobSpawning")) {
                                                            world.setGameRuleValue("doMobSpawning", "false");
                                                        } else if (Main.JavaPlugin.getConfig().getBoolean("doMobSpawning")) {
                                                            world.setGameRuleValue("doMobSpawning", "true");
                                                        }

                                                        if (!Main.JavaPlugin.getConfig().getBoolean("mobGriefing")) {
                                                            world.setGameRuleValue("mobGriefing", "false");
                                                        } else if (Main.JavaPlugin.getConfig().getBoolean("mobGriefing")) {
                                                            world.setGameRuleValue("mobGriefing", "true");
                                                        }

                                                        if (!Main.JavaPlugin.getConfig().getBoolean("doFireTick")) {
                                                            world.setGameRuleValue("doFireTick", "false");
                                                        } else if (Main.JavaPlugin.getConfig().getBoolean("doFireTick")) {
                                                            world.setGameRuleValue("doFireTick", "true");
                                                        }

                                                        if (Variable.bungee) {
                                                            int set_level = 1;

                                                            for (int i = Main.JavaPlugin.getConfig().getInt("MaxLevel"); i > 0; i--) {
                                                                if (p.hasPermission("SelfHome.Level." + i) && !p.isOp()) {
                                                                    set_level = i;
                                                                    break;
                                                                }
                                                            }

                                                            MySQL.insertvalue(
                                                                p.getName(),
                                                                "",
                                                                "",
                                                                "",
                                                                String.valueOf(Main.JavaPlugin.getConfig().getBoolean("NormalPublic")),
                                                                String.valueOf(set_level),
                                                                String.valueOf(Main.JavaPlugin.getConfig().getBoolean("NormalPVP")),
                                                                String.valueOf(Main.JavaPlugin.getConfig().getBoolean("NormalPickup")),
                                                                String.valueOf(Main.JavaPlugin.getConfig().getBoolean("NormalDrop")),
                                                                Main.JavaPlugin.getConfig().getString("Server"),
                                                                "false",
                                                                "false",
                                                                "0",
                                                                String.valueOf(world.getSpawnLocation().getX()),
                                                                String.valueOf(world.getSpawnLocation().getY()),
                                                                String.valueOf(world.getSpawnLocation().getZ()),
                                                                "0",
                                                                "0",
                                                                "",
                                                                "",
                                                                "",
                                                                "",
                                                                ""
                                                            );
                                                            if (Main.JavaPlugin.getConfig().getBoolean("ClearInventoryBeforeCreate")) {
                                                                p.getInventory().clear();
                                                                p.sendMessage(Variable.Lang_YML.getString("ClearInventoryBeforeCreate"));
                                                            }

                                                            for (int c = 0; c < Main.JavaPlugin.getConfig().getStringList("DispathCommand").size(); c++) {
                                                                String temp = (String)Main.JavaPlugin.getConfig().getStringList("DispathCommand").get(c);
                                                                if (temp.contains("<Name>")) {
                                                                    temp = temp.replace("<Name>", p.getName());
                                                                }

                                                                if (temp.contains("[console]")) {
                                                                    temp = temp.replace("[console]", "");
                                                                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), temp);
                                                                } else if (temp.contains("[player]")) {
                                                                    temp = temp.replace("[player]", "");
                                                                    Bukkit.dispatchCommand(p, temp);
                                                                }
                                                            }
                                                        } else {
                                                            File f2 = new File(Variable.Tempf, String.valueOf(p.getName()) + ".yml");
                                                            if (f2.exists()) {
                                                                sender.sendMessage(Variable.Lang_YML.getString("HeadLineTtitle"));
                                                                sender.sendMessage(Variable.Lang_YML.getString("AlreadyHome"));
                                                                sender.sendMessage(Variable.Lang_YML.getString("BottomLineTtitle"));
                                                                return false;
                                                            }

                                                            try {
                                                                f2.createNewFile();
                                                            } catch (Exception e) {
                                                                e.printStackTrace();
                                                            }

                                                            YamlConfiguration yamlConfiguration1 = YamlConfiguration.loadConfiguration(f2);
                                                            yamlConfiguration1.createSection("Members");
                                                            yamlConfiguration1.createSection("OP");
                                                            yamlConfiguration1.createSection("Denys");
                                                            yamlConfiguration1.createSection("Public");
                                                            yamlConfiguration1.createSection("Level");
                                                            yamlConfiguration1.createSection("pvp");
                                                            yamlConfiguration1.createSection("pickup");
                                                            yamlConfiguration1.createSection("drop");
                                                            yamlConfiguration1.createSection("Server");
                                                            yamlConfiguration1.createSection("locktime");
                                                            yamlConfiguration1.createSection("lockweather");
                                                            yamlConfiguration1.createSection("time");
                                                            if (!yamlConfiguration.contains("NowID")) {
                                                                yamlConfiguration.set("NowID", 0);
                                                            }

                                                            if (!yamlConfiguration.contains("MaxID")) {
                                                                yamlConfiguration.set("MaxID", 1000);
                                                            }

                                                            try {
                                                                yamlConfiguration.save(Variable.f_log);
                                                            } catch (Exception e) {
                                                                e.printStackTrace();
                                                            }

                                                            yamlConfiguration1.set("Public", Main.JavaPlugin.getConfig().getBoolean("NormalPublic"));
                                                            yamlConfiguration1.set("pickup", Main.JavaPlugin.getConfig().getBoolean("NormalPickup"));
                                                            yamlConfiguration1.set("drop", Main.JavaPlugin.getConfig().getBoolean("NormalDrop"));
                                                            yamlConfiguration1.set("pvp", Main.JavaPlugin.getConfig().getBoolean("NormalPVP"));
                                                            yamlConfiguration1.set("locktime", false);
                                                            yamlConfiguration1.set("time", 0);
                                                            yamlConfiguration1.set("lockweather", false);
                                                            int set_level = 1;

                                                            for (int i = Main.JavaPlugin.getConfig().getInt("MaxLevel"); i > 0; i--) {
                                                                if (p.hasPermission("SelfHome.Level." + i) && !p.isOp()) {
                                                                    set_level = i;
                                                                    break;
                                                                }
                                                            }

                                                            yamlConfiguration1.set("Level", set_level);
                                                            yamlConfiguration1.set("Server", Main.JavaPlugin.getConfig().getString("Server"));

                                                            try {
                                                                yamlConfiguration1.save(f2);
                                                            } catch (Exception e) {
                                                                e.printStackTrace();
                                                            }

                                                            yamlConfiguration.set("NowID", nowID + 1);

                                                            try {
                                                                yamlConfiguration.save(Variable.f_log);
                                                            } catch (Exception e) {
                                                                e.printStackTrace();
                                                            }

                                                            if (Main.JavaPlugin.getConfig().getInt("MaxSpawnMonstersAmount") != -1) {
                                                                world.setMonsterSpawnLimit(Main.JavaPlugin.getConfig().getInt("MaxSpawnMonstersAmount"));
                                                            }

                                                            if (Main.JavaPlugin.getConfig().getInt("MaxSpawnAnimalsAmount") != -1) {
                                                                world.setMonsterSpawnLimit(Main.JavaPlugin.getConfig().getInt("MaxSpawnAnimalsAmount"));
                                                            }

                                                            if (Main.JavaPlugin.getConfig().getInt("MaxSpawnAnimalsAmount") == 0
                                                                && Variable.hook_multiverseCore) {
                                                                MultiverseCore mvcore = (MultiverseCore)Bukkit.getServer()
                                                                    .getPluginManager()
                                                                    .getPlugin("Multiverse-Core");
                                                                WorldManager mv_m = MultiverseCoreApi.get().getWorldManager();
                                                                MultiverseWorld mv = mv_m.getWorld(p.getLocation().getWorld().getName()).getOrNull();
                                                                // mv.setAllowAnimalSpawn(false);
                                                            }

                                                            if (Main.JavaPlugin.getConfig().getInt("MaxSpawnMonstersAmount") == 0
                                                                && Variable.hook_multiverseCore) {
                                                                MultiverseCore mvcore = (MultiverseCore)Bukkit.getServer()
                                                                    .getPluginManager()
                                                                    .getPlugin("Multiverse-Core");
                                                                WorldManager mv_m = MultiverseCoreApi.get().getWorldManager();
                                                                MultiverseWorld mv = mv_m.getWorld(p.getLocation().getWorld().getName()).getOrNull();
                                                                // mv.setAllowMonsterSpawn(false);
                                                            }

                                                            yamlConfiguration1.createSection("X");
                                                            yamlConfiguration1.createSection("Y");
                                                            yamlConfiguration1.createSection("Z");
                                                            Location loc = world.getSpawnLocation();
                                                            yamlConfiguration1.set("X", loc.getX());
                                                            yamlConfiguration1.set("Y", loc.getY());
                                                            yamlConfiguration1.set("Z", loc.getZ());
                                                            yamlConfiguration1.createSection("flowers");
                                                            yamlConfiguration1.createSection("popularity");
                                                            yamlConfiguration1.createSection("gifts");
                                                            yamlConfiguration1.createSection("icon");
                                                            yamlConfiguration1.createSection("advertisement");
                                                            yamlConfiguration1.createSection("limitblock");
                                                            yamlConfiguration1.set("flowers", 0);
                                                            yamlConfiguration1.set("popularity", 0);
                                                            yamlConfiguration1.set("gifts", new ArrayList());
                                                            yamlConfiguration1.set("icon", "");
                                                            yamlConfiguration1.set("advertisement", new ArrayList());
                                                            yamlConfiguration1.set("limitblock", new ArrayList());

                                                            try {
                                                                yamlConfiguration1.save(f2);
                                                            } catch (Exception e) {
                                                                e.printStackTrace();
                                                            }

                                                            Bukkit.dispatchCommand(p, "sh h");
                                                            p.teleport(world.getSpawnLocation());
                                                            /* excluded FirstBorderShaped */
                                                            if (Main.JavaPlugin.getConfig().getBoolean("ClearInventoryBeforeCreate")) {
                                                                p.getInventory().clear();
                                                                p.sendMessage(Variable.Lang_YML.getString("ClearInventoryBeforeCreate"));
                                                            }

                                                            for (int c = 0; c < Main.JavaPlugin.getConfig().getStringList("DispathCommand").size(); c++) {
                                                                String temp = (String)Main.JavaPlugin.getConfig().getStringList("DispathCommand").get(c);
                                                                if (temp.contains("<Name>")) {
                                                                    temp = temp.replace("<Name>", p.getName());
                                                                }

                                                                if (temp.contains("[console]")) {
                                                                    temp = temp.replace("[console]", "");
                                                                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), temp);
                                                                } else if (temp.contains("[player]")) {
                                                                    temp = temp.replace("[player]", "");
                                                                    Bukkit.dispatchCommand(p, temp);
                                                                }
                                                            }
                                                        }

                                                        return false;
                                                    }
                                                } else {
                                                    sender.sendMessage(Variable.Lang_YML.getString("HeadLineTtitle"));
                                                    if (Variable.has_no_click_message) {
                                                        p.sendMessage(Variable.Lang_YML.getString("ErrorHelp"));
                                                    } else {
                                                        TextComponent Click_Message = new TextComponent(Variable.Lang_YML.getString("ErrorHelp"));
                                                        Click_Message.setClickEvent(new ClickEvent(Action.RUN_COMMAND, "/sh Help 1"));
                                                        p.spigot().sendMessage(Click_Message);
                                                    }

                                                    sender.sendMessage(Variable.Lang_YML.getString("BottomLineTtitle"));
                                                    return false;
                                                }
                                            } else {
                                                String Name = args[1];
                                                if (!p.getWorld()
                                                    .getName()
                                                    .replace(Variable.world_prefix, "")
                                                    .equalsIgnoreCase(p.getWorld().getName().replace(Variable.world_prefix, ""))) {
                                                    String temp = Variable.Lang_YML.getString("NoOwnerPermission");
                                                    sender.sendMessage(Variable.Lang_YML.getString("HeadLineTtitle"));
                                                    sender.sendMessage(temp);
                                                    sender.sendMessage(Variable.Lang_YML.getString("BottomLineTtitle"));
                                                    return false;
                                                }

                                                if (Variable.bungee) {
                                                    List<String> save = MySQL.getOP(p.getName());
                                                    Boolean Check = false;
                                                    if (save == null) {
                                                        save = new ArrayList<>();
                                                    }

                                                    for (int i = 0; i < save.size(); i++) {
                                                        if (save.get(i).equalsIgnoreCase(Name)) {
                                                            Check = true;
                                                        }
                                                    }

                                                    if (Check) {
                                                        for (int i = 0; i < save.size(); i++) {
                                                            if (save.get(i).equalsIgnoreCase(Name)) {
                                                                String result = MySQL.getListStringSpiltByDot(
                                                                    MySQL.getOP(p.getWorld().getName().replace(Variable.world_prefix, ""))
                                                                );
                                                                if (result.contains("," + Name)) {
                                                                    result = result.replace("," + Name, "");
                                                                } else if (result.contains(Name)) {
                                                                    result = result.replace(Name, "");
                                                                }

                                                                MySQL.setOP(p.getWorld().getName().replace(Variable.world_prefix, ""), result);
                                                                Player pt = Bukkit.getPlayer(Name);
                                                                if (pt != null
                                                                    && pt.getWorld().getName().replace(Variable.world_prefix, "").equalsIgnoreCase(p.getName())
                                                                    )
                                                                 {
                                                                    String bekicked = Main.JavaPlugin.getConfig().getString("BeKickedCommand");
                                                                    if (bekicked.contains("<Name>")) {
                                                                        bekicked = bekicked.replace("<Name>", pt.getName());
                                                                    }

                                                                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), bekicked);
                                                                    String temp = Variable.Lang_YML.getString("BeKicked");
                                                                    pt.sendMessage("§a§l§m--------------" + Variable.Prefix + "§a§l§m--------------");
                                                                    pt.sendMessage(temp);
                                                                    pt.sendMessage("§a§l§m--------------" + Variable.Prefix + "§a§l§m--------------");
                                                                }

                                                                String temp = Variable.Lang_YML.getString("KickSuccess");
                                                                if (temp.contains("<Name>")) {
                                                                    temp = temp.replace("<Name>", Name);
                                                                }

                                                                sender.sendMessage(Variable.Lang_YML.getString("HeadLineTtitle"));
                                                                sender.sendMessage(temp);
                                                                sender.sendMessage(Variable.Lang_YML.getString("BottomLineTtitle"));
                                                            }
                                                        }
                                                    } else {
                                                        String temp = Variable.Lang_YML.getString("KickNotExist");
                                                        sender.sendMessage(Variable.Lang_YML.getString("HeadLineTtitle"));
                                                        sender.sendMessage(temp);
                                                        sender.sendMessage(Variable.Lang_YML.getString("BottomLineTtitle"));
                                                    }
                                                } else {
                                                    File f2 = new File(
                                                        Variable.Tempf, String.valueOf(p.getWorld().getName().replace(Variable.world_prefix, "")) + ".yml"
                                                    );
                                                    YamlConfiguration yamlConfiguration = YamlConfiguration.loadConfiguration(f2);
                                                    List<String> save = yamlConfiguration.getStringList("OP");
                                                    Boolean Check = false;
                                                    if (save == null) {
                                                        save = new ArrayList<>();
                                                    }

                                                    for (int i = 0; i < save.size(); i++) {
                                                        if (save.get(i).equalsIgnoreCase(Name)) {
                                                            Check = true;
                                                        }
                                                    }

                                                    if (Check) {
                                                        for (int i = 0; i < save.size(); i++) {
                                                            if (save.get(i).equalsIgnoreCase(Name)) {
                                                                save.remove(i);
                                                                yamlConfiguration.set("OP", save);

                                                                try {
                                                                    yamlConfiguration.save(f2);
                                                                } catch (Exception e) {
                                                                    e.printStackTrace();
                                                                }

                                                                Player pt = Bukkit.getPlayer(Name);
                                                                if (pt != null
                                                                    && pt.getWorld().getName().replace(Variable.world_prefix, "").equalsIgnoreCase(p.getName())
                                                                    )
                                                                 {
                                                                    String bekicked = Main.JavaPlugin.getConfig().getString("BeKickedCommand");
                                                                    if (bekicked.contains("<Name>")) {
                                                                        bekicked = bekicked.replace("<Name>", pt.getName());
                                                                    }

                                                                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), bekicked);
                                                                    String temp = Variable.Lang_YML.getString("BeKicked");
                                                                    pt.sendMessage("§a§l§m--------------" + Variable.Prefix + "§a§l§m--------------");
                                                                    pt.sendMessage(temp);
                                                                    pt.sendMessage("§a§l§m--------------" + Variable.Prefix + "§a§l§m--------------");
                                                                }

                                                                String temp = Variable.Lang_YML.getString("KickSuccess");
                                                                if (temp.contains("<Name>")) {
                                                                    temp = temp.replace("<Name>", Name);
                                                                }

                                                                sender.sendMessage(Variable.Lang_YML.getString("HeadLineTtitle"));
                                                                sender.sendMessage(temp);
                                                                sender.sendMessage(Variable.Lang_YML.getString("BottomLineTtitle"));
                                                            }
                                                        }
                                                    } else {
                                                        String temp = Variable.Lang_YML.getString("KickNotExist");
                                                        sender.sendMessage(Variable.Lang_YML.getString("HeadLineTtitle"));
                                                        sender.sendMessage(temp);
                                                        sender.sendMessage(Variable.Lang_YML.getString("BottomLineTtitle"));
                                                    }
                                                }

                                                return false;
                                            }
                                        } else if (!Main.JavaPlugin.getConfig().getBoolean("Permission.CommandUser")
                                            && !p.hasPermission("SelfHome.Quit")
                                            && !p.hasPermission("SelfHome.command.user")) {
                                            String tip = Variable.Lang_YML.getString("NoPermissionCheck");
                                            if (tip.contains("<Permission>")) {
                                                tip = tip.replace("<Permission>", "SelfHome.Quit");
                                            }

                                            p.sendMessage(tip);
                                            return false;
                                        } else if (Variable.bungee) {
                                            boolean has_been_quit = MySQL.PlayerQuitHome(p.getName());
                                            if (!has_been_quit) {
                                                String Message = Variable.Lang_YML.getString("QuitButNoJoin");
                                                p.sendMessage(Message);
                                                return false;
                                            } else {
                                                String Message = Variable.Lang_YML.getString("QuitSuccess");
                                                p.sendMessage(Message);
                                                return false;
                                            }
                                        } else {
                                            File folder = new File(Variable.Tempf);
                                            boolean has_been_quit = false;
                                            File[] arrayOfFile;
                                            int j = (arrayOfFile = folder.listFiles()).length;

                                            label5439:
                                            for (int b = 0; b < j; b++) {
                                                File temp = arrayOfFile[b];
                                                String want_to = temp.getPath()
                                                    .replace(Variable.Tempf, "")
                                                    .replace(".yml", "")
                                                    .replace(Variable.file_loc_prefix, "");
                                                YamlConfiguration yamlConfiguration = YamlConfiguration.loadConfiguration(temp);
                                                List<String> Ops = yamlConfiguration.getStringList("OP");

                                                for (int i = 0; i < Ops.size(); i++) {
                                                    String temp_str = (String)yamlConfiguration.getStringList("OP").get(i);
                                                    if (temp_str.equalsIgnoreCase(p.getName())) {
                                                        has_been_quit = true;
                                                        String Message = Variable.Lang_YML.getString("QuitSuccess");
                                                        if (Message.contains("<Name>")) {
                                                            Message = Message.replace("<Name>", want_to);
                                                        }

                                                        if (Bukkit.getPlayer(want_to) != null && Bukkit.getPlayer(want_to) != null) {
                                                            String ManagerQuitTip = Variable.Lang_YML.getString("QuitManager");
                                                            if (ManagerQuitTip.contains("<Name>")) {
                                                                ManagerQuitTip = ManagerQuitTip.replace("<Name>", p.getName());
                                                            }

                                                            Bukkit.getPlayer(want_to).sendMessage(ManagerQuitTip);
                                                        }

                                                        p.sendMessage(Message);
                                                        Ops.remove(i);
                                                        yamlConfiguration.set("OP", Ops);

                                                        try {
                                                            yamlConfiguration.save(temp);
                                                        } catch (Exception e) {
                                                            e.printStackTrace();
                                                        }
                                                        break label5439;
                                                    }
                                                }
                                            }

                                            if (!has_been_quit) {
                                                String Message = Variable.Lang_YML.getString("QuitButNoJoin");
                                                p.sendMessage(Message);
                                                return false;
                                            } else {
                                                return false;
                                            }
                                        }
                                    } else {
                                        String Name = args[1];
                                        File f2 = new File(Variable.Tempf, String.valueOf(p.getWorld().getName().replace(Variable.world_prefix, "")) + ".yml");
                                        if (!Util.CheckOwnerAndManagerAndOP(p, p.getWorld().getName().replace(Variable.world_prefix, ""))) {
                                            String temp = Variable.Lang_YML.getString("NoOwnerAndManagerPermission");
                                            sender.sendMessage(Variable.Lang_YML.getString("HeadLineTtitle"));
                                            sender.sendMessage(temp);
                                            sender.sendMessage(Variable.Lang_YML.getString("BottomLineTtitle"));
                                            return false;
                                        }

                                        if (Variable.bungee) {
                                            List<String> save = MySQL.getMembers(p.getWorld().getName().replace(Variable.world_prefix, ""));
                                            Boolean Check = false;
                                            if (save == null) {
                                                save = new ArrayList<>();
                                            }

                                            for (int i = 0; i < save.size(); i++) {
                                                if (save.get(i).equalsIgnoreCase(Name)) {
                                                    Check = true;
                                                }
                                            }

                                            if (Check) {
                                                for (int i = 0; i < save.size(); i++) {
                                                    if (save.get(i).equalsIgnoreCase(Name)) {
                                                        String result = MySQL.getListStringSpiltByDot(
                                                            MySQL.getMembers(p.getWorld().getName().replace(Variable.world_prefix, ""))
                                                        );
                                                        result = result.replace("," + save.get(i), "");
                                                        result = result.replace(save.get(i), "");
                                                        MySQL.setMembers(p.getWorld().getName().replace(Variable.world_prefix, ""), result);
                                                        String temp = Variable.Lang_YML.getString("RemoveTrustPlayer");
                                                        if (temp.contains("<Name>")) {
                                                            temp = temp.replace("<Name>", args[1]);
                                                        }

                                                        sender.sendMessage(Variable.Lang_YML.getString("HeadLineTtitle"));
                                                        sender.sendMessage(temp);
                                                        sender.sendMessage(Variable.Lang_YML.getString("BottomLineTtitle"));
                                                    }
                                                }
                                            } else {
                                                String temp = Variable.Lang_YML.getString("NoTrustExist");
                                                sender.sendMessage(Variable.Lang_YML.getString("HeadLineTtitle"));
                                                sender.sendMessage(temp);
                                                sender.sendMessage(Variable.Lang_YML.getString("BottomLineTtitle"));
                                            }
                                        } else {
                                            YamlConfiguration yamlConfiguration = YamlConfiguration.loadConfiguration(f2);
                                            List<String> save = yamlConfiguration.getStringList("Members");
                                            Boolean Check = false;
                                            if (save == null) {
                                                save = new ArrayList<>();
                                            }

                                            for (int i = 0; i < save.size(); i++) {
                                                if (save.get(i).equalsIgnoreCase(Name)) {
                                                    Check = true;
                                                }
                                            }

                                            if (Check) {
                                                for (int i = 0; i < save.size(); i++) {
                                                    if (save.get(i).equalsIgnoreCase(Name)) {
                                                        save.remove(i);
                                                        yamlConfiguration.set("Members", save);

                                                        try {
                                                            yamlConfiguration.save(f2);
                                                        } catch (Exception e) {
                                                            e.printStackTrace();
                                                        }

                                                        String temp = Variable.Lang_YML.getString("RemoveTrustPlayer");
                                                        if (temp.contains("<Name>")) {
                                                            temp = temp.replace("<Name>", args[1]);
                                                        }

                                                        sender.sendMessage(Variable.Lang_YML.getString("HeadLineTtitle"));
                                                        sender.sendMessage(temp);
                                                        sender.sendMessage(Variable.Lang_YML.getString("BottomLineTtitle"));
                                                    }
                                                }
                                            } else {
                                                String temp = Variable.Lang_YML.getString("NoTrustExist");
                                                sender.sendMessage(Variable.Lang_YML.getString("HeadLineTtitle"));
                                                sender.sendMessage(temp);
                                                sender.sendMessage(Variable.Lang_YML.getString("BottomLineTtitle"));
                                            }
                                        }

                                        return false;
                                    }
                                } else {
                                    String Name = args[1];
                                    if (!Main.JavaPlugin.getConfig().getBoolean("Permission.CommandUser")
                                        && !p.hasPermission("SelfHome.Trust")
                                        && !p.hasPermission("SelfHome.command.user")) {
                                        String tip = Variable.Lang_YML.getString("NoPermissionCheck");
                                        if (tip.contains("<Permission>")) {
                                            tip = tip.replace("<Permission>", "SelfHome.Trust");
                                        }

                                        p.sendMessage(tip);
                                        return false;
                                    } else {
                                        File f2 = new File(Variable.Tempf, String.valueOf(p.getWorld().getName().replace(Variable.world_prefix, "")) + ".yml");
                                        if (!Util.CheckOwnerAndManagerAndOP(p, p.getWorld().getName().replace(Variable.world_prefix, ""))) {
                                            String temp = Variable.Lang_YML.getString("NoOwnerPermission");
                                            sender.sendMessage(Variable.Lang_YML.getString("HeadLineTtitle"));
                                            sender.sendMessage(temp);
                                            sender.sendMessage(Variable.Lang_YML.getString("BottomLineTtitle"));
                                            return false;
                                        }

                                        if (args[1].equalsIgnoreCase(p.getName())) {
                                            String temp = Variable.Lang_YML.getString("AddOwnerToTrust");
                                            sender.sendMessage(Variable.Lang_YML.getString("HeadLineTtitle"));
                                            sender.sendMessage(temp);
                                            sender.sendMessage(Variable.Lang_YML.getString("BottomLineTtitle"));
                                            return false;
                                        }

                                        if (Variable.bungee) {
                                            List<String> blacklist = MySQL.getDenys(p.getWorld().getName().replace(Variable.world_prefix, ""));
                                            if (blacklist == null) {
                                                blacklist = new ArrayList<>();
                                            }

                                            for (int i = 0; i < blacklist.size(); i++) {
                                                if (blacklist.get(i).equalsIgnoreCase(args[1])) {
                                                    String temp = Variable.Lang_YML.getString("HasAlreadyInBlack");
                                                    sender.sendMessage(Variable.Lang_YML.getString("HeadLineTtitle"));
                                                    sender.sendMessage(temp);
                                                    sender.sendMessage(Variable.Lang_YML.getString("BottomLineTtitle"));
                                                    return false;
                                                }
                                            }

                                            List<String> save = MySQL.getMembers(p.getWorld().getName().replace(Variable.world_prefix, ""));
                                            if (save == null) {
                                                save = new ArrayList<>();
                                            }

                                            Boolean CheckSame = false;

                                            for (int i = 0; i < save.size(); i++) {
                                                if (save.get(i).equalsIgnoreCase(Name)) {
                                                    String temp = Variable.Lang_YML.getString("HasAlreadyTrust");
                                                    CheckSame = true;
                                                    sender.sendMessage(Variable.Lang_YML.getString("HeadLineTtitle"));
                                                    sender.sendMessage(temp);
                                                    sender.sendMessage(Variable.Lang_YML.getString("BottomLineTtitle"));
                                                }
                                            }

                                            if (!CheckSame) {
                                                if (save.size() + 1 > Main.JavaPlugin.getConfig().getInt("MaxJoin")) {
                                                    int max_player = Main.JavaPlugin.getConfig().getInt("MaxJoin");

                                                    for (int i = Main.JavaPlugin.getConfig().getInt("MaxJoin") * 3;
                                                        i > Main.JavaPlugin.getConfig().getInt("MaxJoin");
                                                        i--
                                                    ) {
                                                        if (p.hasPermission("SelfHome.MaxJoin." + i)) {
                                                            max_player = i;
                                                            break;
                                                        }
                                                    }

                                                    if (save.size() + 2 > max_player) {
                                                        String temp = Variable.Lang_YML.getString("MaxJoinMembers");
                                                        if (temp.contains("<MaxAmount>")) {
                                                            temp = temp.replace("<MaxAmount>", String.valueOf(max_player));
                                                        }

                                                        sender.sendMessage(Variable.Lang_YML.getString("HeadLineTtitle"));
                                                        sender.sendMessage(temp);
                                                        sender.sendMessage(Variable.Lang_YML.getString("BottomLineTtitle"));
                                                    } else {
                                                        String result = MySQL.getListStringSpiltByDot(
                                                            MySQL.getMembers(p.getWorld().getName().replace(Variable.world_prefix, ""))
                                                        );
                                                        if (result != null && !result.equalsIgnoreCase("")) {
                                                            result = String.valueOf(result) + "," + Name;
                                                        } else {
                                                            result = Name;
                                                        }

                                                        MySQL.setMembers(p.getWorld().getName().replace(Variable.world_prefix, ""), result);
                                                        String temp = Variable.Lang_YML.getString("AddTrustSuccess");
                                                        if (temp.contains("<Name>")) {
                                                            temp = temp.replace("<Name>", Name);
                                                        }

                                                        sender.sendMessage(Variable.Lang_YML.getString("HeadLineTtitle"));
                                                        sender.sendMessage(temp);
                                                        sender.sendMessage(Variable.Lang_YML.getString("BottomLineTtitle"));
                                                    }

                                                    return false;
                                                }

                                                String result = MySQL.getListStringSpiltByDot(
                                                    MySQL.getMembers(p.getWorld().getName().replace(Variable.world_prefix, ""))
                                                );
                                                if (result != null && !result.equalsIgnoreCase("")) {
                                                    result = String.valueOf(result) + "," + Name;
                                                } else {
                                                    result = Name;
                                                }

                                                MySQL.setMembers(p.getWorld().getName().replace(Variable.world_prefix, ""), result);
                                                String temp = Variable.Lang_YML.getString("AddTrustSuccess");
                                                if (temp.contains("<Name>")) {
                                                    temp = temp.replace("<Name>", Name);
                                                }

                                                sender.sendMessage(Variable.Lang_YML.getString("HeadLineTtitle"));
                                                sender.sendMessage(temp);
                                                sender.sendMessage(Variable.Lang_YML.getString("BottomLineTtitle"));
                                            }
                                        } else {
                                            YamlConfiguration yamlConfiguration = YamlConfiguration.loadConfiguration(f2);
                                            List<String> blacklist = yamlConfiguration.getStringList("Denys");
                                            if (blacklist == null) {
                                                blacklist = new ArrayList<>();
                                            }

                                            for (int i = 0; i < blacklist.size(); i++) {
                                                if (blacklist.get(i).equalsIgnoreCase(args[1])) {
                                                    String temp = Variable.Lang_YML.getString("HasAlreadyInBlack");
                                                    sender.sendMessage(Variable.Lang_YML.getString("HeadLineTtitle"));
                                                    sender.sendMessage(temp);
                                                    sender.sendMessage(Variable.Lang_YML.getString("BottomLineTtitle"));
                                                    return false;
                                                }
                                            }

                                            List<String> save = yamlConfiguration.getStringList("Members");
                                            if (save == null) {
                                                save = new ArrayList<>();
                                            }

                                            Boolean CheckSame = false;

                                            for (int i = 0; i < save.size(); i++) {
                                                if (save.get(i).equalsIgnoreCase(Name)) {
                                                    String temp = Variable.Lang_YML.getString("HasAlreadyTrust");
                                                    CheckSame = true;
                                                    sender.sendMessage(Variable.Lang_YML.getString("HeadLineTtitle"));
                                                    sender.sendMessage(temp);
                                                    sender.sendMessage(Variable.Lang_YML.getString("BottomLineTtitle"));
                                                }
                                            }

                                            if (!CheckSame) {
                                                if (save.size() + 2 > Main.JavaPlugin.getConfig().getInt("MaxJoin")) {
                                                    int max_player = Main.JavaPlugin.getConfig().getInt("MaxJoin");

                                                    for (int i = Main.JavaPlugin.getConfig().getInt("MaxJoin") * 3;
                                                        i > Main.JavaPlugin.getConfig().getInt("MaxJoin");
                                                        i--
                                                    ) {
                                                        if (p.hasPermission("SelfHome.MaxJoin." + i)) {
                                                            max_player = i;
                                                            break;
                                                        }
                                                    }

                                                    if (save.size() + 2 > max_player) {
                                                        String temp = Variable.Lang_YML.getString("MaxJoinMembers");
                                                        if (temp.contains("<MaxAmount>")) {
                                                            temp = temp.replace("<MaxAmount>", String.valueOf(max_player));
                                                        }

                                                        sender.sendMessage(Variable.Lang_YML.getString("HeadLineTtitle"));
                                                        sender.sendMessage(temp);
                                                        sender.sendMessage(Variable.Lang_YML.getString("BottomLineTtitle"));
                                                    } else {
                                                        save.add(Name);
                                                        yamlConfiguration.set("Members", save);

                                                        try {
                                                            yamlConfiguration.save(f2);
                                                        } catch (Exception e) {
                                                            e.printStackTrace();
                                                        }

                                                        String temp = Variable.Lang_YML.getString("AddTrustSuccess");
                                                        if (temp.contains("<Name>")) {
                                                            temp = temp.replace("<Name>", Name);
                                                        }

                                                        sender.sendMessage(Variable.Lang_YML.getString("HeadLineTtitle"));
                                                        sender.sendMessage(temp);
                                                        sender.sendMessage(Variable.Lang_YML.getString("BottomLineTtitle"));
                                                    }

                                                    return false;
                                                }

                                                save.add(Name);
                                                yamlConfiguration.set("Members", save);

                                                try {
                                                    yamlConfiguration.save(f2);
                                                } catch (Exception e) {
                                                    e.printStackTrace();
                                                }

                                                String temp = Variable.Lang_YML.getString("AddTrustSuccess");
                                                if (temp.contains("<Name>")) {
                                                    temp = temp.replace("<Name>", Name);
                                                }

                                                sender.sendMessage(Variable.Lang_YML.getString("HeadLineTtitle"));
                                                sender.sendMessage(temp);
                                                sender.sendMessage(Variable.Lang_YML.getString("BottomLineTtitle"));
                                            }
                                        }

                                        return false;
                                    }
                                }
                            } else {
                                String Name = args[1];
                                if (Variable.bungee) {
                                    if (!Main.JavaPlugin.getConfig().getBoolean("Permission.CommandUser")
                                        && !p.hasPermission("SelfHome.Invite")
                                        && !p.hasPermission("SelfHome.command.user")) {
                                        String tip = Variable.Lang_YML.getString("NoPermissionCheck");
                                        if (tip.contains("<Permission>")) {
                                            tip = tip.replace("<Permission>", "SelfHome.Invite");
                                        }

                                        p.sendMessage(tip);
                                        return false;
                                    }

                                    if (!MySQL.alreadyhastheplayerhome(p.getName())) {
                                        String temp = Variable.Lang_YML.getString("NoHome");
                                        sender.sendMessage(Variable.Lang_YML.getString("HeadLineTtitle"));
                                        sender.sendMessage(temp);
                                        sender.sendMessage(Variable.Lang_YML.getString("BottomLineTtitle"));
                                        return false;
                                    }

                                    if (args[1].equalsIgnoreCase(p.getName())) {
                                        String temp = Variable.Lang_YML.getString("InviteMySelf");
                                        sender.sendMessage(Variable.Lang_YML.getString("HeadLineTtitle"));
                                        sender.sendMessage(temp);
                                        sender.sendMessage(Variable.Lang_YML.getString("BottomLineTtitle"));
                                        return false;
                                    }

                                    List<String> blacklist = MySQL.getDenys(p.getName());
                                    if (blacklist == null) {
                                        blacklist = new ArrayList<>();
                                    }

                                    for (int i = 0; i < blacklist.size(); i++) {
                                        if (blacklist.get(i).equalsIgnoreCase(args[1])) {
                                            String temp = Variable.Lang_YML.getString("HasAlreadyInBlack");
                                            sender.sendMessage(Variable.Lang_YML.getString("HeadLineTtitle"));
                                            sender.sendMessage(temp);
                                            sender.sendMessage(Variable.Lang_YML.getString("BottomLineTtitle"));
                                            return false;
                                        }
                                    }

                                    List<String> memberlist = MySQL.getMembers(p.getName());
                                    if (memberlist == null) {
                                        memberlist = new ArrayList<>();
                                    }

                                    for (int i = 0; i < memberlist.size(); i++) {
                                        if (memberlist.get(i).equalsIgnoreCase(args[1])) {
                                            String temp = Variable.Lang_YML.getString("HasAlreadyTrust");
                                            sender.sendMessage(Variable.Lang_YML.getString("HeadLineTtitle"));
                                            sender.sendMessage(temp);
                                            sender.sendMessage(Variable.Lang_YML.getString("BottomLineTtitle"));
                                            return false;
                                        }
                                    }

                                    List<String> OP_List = MySQL.getOP(p.getName());
                                    if (OP_List == null) {
                                        OP_List = new ArrayList<>();
                                    }

                                    Boolean CheckSame = false;

                                    for (int i = 0; i < OP_List.size(); i++) {
                                        if (OP_List.get(i).equalsIgnoreCase(Name)) {
                                            CheckSame = true;
                                            String temp = Variable.Lang_YML.getString("HasAlreadyOP");
                                            sender.sendMessage(Variable.Lang_YML.getString("HeadLineTtitle"));
                                            sender.sendMessage(temp);
                                            sender.sendMessage(Variable.Lang_YML.getString("BottomLineTtitle"));
                                            return false;
                                        }
                                    }

                                    if (!CheckSame) {
                                        if (MySQL.alreadyhastheplayerhome(args[1])) {
                                            String temp = Variable.Lang_YML.getString("InvitePlayerWhoHasCreateHome");
                                            sender.sendMessage(Variable.Lang_YML.getString("HeadLineTtitle"));
                                            sender.sendMessage(temp);
                                            sender.sendMessage(Variable.Lang_YML.getString("BottomLineTtitle"));
                                            return false;
                                        }

                                        if (MySQL.alreadyhastheplayerjoin(args[1])) {
                                            String temp = Variable.Lang_YML.getString("InvitePlayerWhoHasJoinOthers");
                                            sender.sendMessage(Variable.Lang_YML.getString("HeadLineTtitle"));
                                            sender.sendMessage(temp);
                                            sender.sendMessage(Variable.Lang_YML.getString("BottomLineTtitle"));
                                            return false;
                                        }

                                        if (Variable.invite_list.containsKey(p.getName())) {
                                            String temp = Variable.Lang_YML.getString("HasInInviteCooldown");
                                            sender.sendMessage(Variable.Lang_YML.getString("HeadLineTtitle"));
                                            sender.sendMessage(temp);
                                            sender.sendMessage(Variable.Lang_YML.getString("BottomLineTtitle"));
                                            return false;
                                        }

                                        if (Variable.invite_list.containsValue(args[1])) {
                                            String temp = Variable.Lang_YML.getString("InvitePlayerWhoHasBeenAlreadyInvited");
                                            sender.sendMessage(Variable.Lang_YML.getString("HeadLineTtitle"));
                                            sender.sendMessage(temp);
                                            sender.sendMessage(Variable.Lang_YML.getString("BottomLineTtitle"));
                                            return false;
                                        }

                                        if (OP_List.size() + 2 > Main.JavaPlugin.getConfig().getInt("MaxOP")) {
                                            int max_player = Main.JavaPlugin.getConfig().getInt("MaxOP");

                                            for (int i = Main.JavaPlugin.getConfig().getInt("MaxOP") * 3; i > Main.JavaPlugin.getConfig().getInt("MaxOP"); i--) {
                                                if (p.hasPermission("SelfHome.MaxOP." + i)) {
                                                    max_player = i;
                                                    break;
                                                }
                                            }

                                            if (OP_List.size() + 2 > max_player) {
                                                String temp = Variable.Lang_YML.getString("ReachMaxOP");
                                                if (temp.contains("<MaxAmount>")) {
                                                    temp = temp.replace("<MaxAmount>", String.valueOf(max_player));
                                                }

                                                sender.sendMessage(Variable.Lang_YML.getString("HeadLineTtitle"));
                                                sender.sendMessage(temp);
                                                sender.sendMessage(Variable.Lang_YML.getString("BottomLineTtitle"));
                                            } else {
                                                Player be_invite = Bukkit.getPlayer(args[1]);
                                                if (be_invite == null) {
                                                    String temp = Variable.Lang_YML.getString("NoPlayerExist");
                                                    sender.sendMessage(Variable.Lang_YML.getString("HeadLineTtitle"));
                                                    sender.sendMessage(temp);
                                                    sender.sendMessage(Variable.Lang_YML.getString("BottomLineTtitle"));
                                                    return false;
                                                }

                                                Variable.invite_list.put(p.getName(), args[1]);
                                                this.invite_guoqi(p);
                                                String temp = Variable.Lang_YML.getString("SendInviteToPlayer");
                                                if (temp.contains("<Name>")) {
                                                    temp = temp.replace("<Name>", args[1]);
                                                }

                                                sender.sendMessage(Variable.Lang_YML.getString("HeadLineTtitle"));
                                                sender.sendMessage(temp);
                                                sender.sendMessage(Variable.Lang_YML.getString("BottomLineTtitle"));
                                                if (be_invite != null) {
                                                    String temp2 = Variable.Lang_YML.getString("InviteMessage");
                                                    if (temp2.contains("<player>")) {
                                                        temp2 = temp2.replace("<player>", p.getName());
                                                    }

                                                    if (Variable.has_no_click_message) {
                                                        be_invite.sendMessage(temp2);
                                                    } else {
                                                        TextComponent Click_Message = new TextComponent(temp2);
                                                        Click_Message.setClickEvent(new ClickEvent(Action.RUN_COMMAND, "/sh accept"));
                                                        be_invite.spigot().sendMessage(Click_Message);
                                                    }
                                                }
                                            }

                                            return false;
                                        }

                                        Player be_invite = Bukkit.getPlayer(args[1]);
                                        if (be_invite == null) {
                                            String temp = Variable.Lang_YML.getString("NoPlayerExist");
                                            sender.sendMessage(Variable.Lang_YML.getString("HeadLineTtitle"));
                                            sender.sendMessage(temp);
                                            sender.sendMessage(Variable.Lang_YML.getString("BottomLineTtitle"));
                                            return false;
                                        }

                                        Variable.invite_list.put(p.getName(), args[1]);
                                        this.invite_guoqi(p);
                                        String temp = Variable.Lang_YML.getString("SendInviteToPlayer");
                                        if (temp.contains("<Name>")) {
                                            temp = temp.replace("<Name>", args[1]);
                                        }

                                        sender.sendMessage(Variable.Lang_YML.getString("HeadLineTtitle"));
                                        sender.sendMessage(temp);
                                        sender.sendMessage(Variable.Lang_YML.getString("BottomLineTtitle"));
                                        if (be_invite != null) {
                                            String temp2 = Variable.Lang_YML.getString("InviteMessage");
                                            if (temp2.contains("<player>")) {
                                                temp2 = temp2.replace("<player>", p.getName());
                                            }

                                            if (Variable.has_no_click_message) {
                                                be_invite.sendMessage(temp2);
                                            } else {
                                                TextComponent Click_Message = new TextComponent(temp2);
                                                Click_Message.setClickEvent(new ClickEvent(Action.RUN_COMMAND, "/sh accept"));
                                                be_invite.spigot().sendMessage(Click_Message);
                                            }
                                        }
                                    }
                                } else {
                                    File f2 = new File(Variable.Tempf, String.valueOf(p.getName()) + ".yml");
                                    if (!f2.exists()) {
                                        String temp = Variable.Lang_YML.getString("NoHome");
                                        sender.sendMessage(Variable.Lang_YML.getString("HeadLineTtitle"));
                                        sender.sendMessage(temp);
                                        sender.sendMessage(Variable.Lang_YML.getString("BottomLineTtitle"));
                                        return false;
                                    }

                                    if (!Main.JavaPlugin.getConfig().getBoolean("Permission.CommandUser")
                                        && !p.hasPermission("SelfHome.Invite")
                                        && !p.hasPermission("SelfHome.command.user")) {
                                        String tip = Variable.Lang_YML.getString("NoPermissionCheck");
                                        if (tip.contains("<Permission>")) {
                                            tip = tip.replace("<Permission>", "SelfHome.Invite");
                                        }

                                        p.sendMessage(tip);
                                        return false;
                                    }

                                    YamlConfiguration yamlConfiguration = YamlConfiguration.loadConfiguration(f2);
                                    if (args[1].equalsIgnoreCase(p.getName())) {
                                        String temp = Variable.Lang_YML.getString("InviteMySelf");
                                        sender.sendMessage(Variable.Lang_YML.getString("HeadLineTtitle"));
                                        sender.sendMessage(temp);
                                        sender.sendMessage(Variable.Lang_YML.getString("BottomLineTtitle"));
                                        return false;
                                    }

                                    List<String> blacklist = yamlConfiguration.getStringList("Denys");
                                    if (blacklist == null) {
                                        blacklist = new ArrayList<>();
                                    }

                                    for (int i = 0; i < blacklist.size(); i++) {
                                        if (blacklist.get(i).equalsIgnoreCase(args[1])) {
                                            String temp = Variable.Lang_YML.getString("HasAlreadyInBlack");
                                            sender.sendMessage(Variable.Lang_YML.getString("HeadLineTtitle"));
                                            sender.sendMessage(temp);
                                            sender.sendMessage(Variable.Lang_YML.getString("BottomLineTtitle"));
                                            return false;
                                        }
                                    }

                                    List<String> memberlist = yamlConfiguration.getStringList("Members");
                                    if (memberlist == null) {
                                        memberlist = new ArrayList<>();
                                    }

                                    for (int i = 0; i < memberlist.size(); i++) {
                                        if (memberlist.get(i).equalsIgnoreCase(args[1])) {
                                            String temp = Variable.Lang_YML.getString("HasAlreadyTrust");
                                            sender.sendMessage(Variable.Lang_YML.getString("HeadLineTtitle"));
                                            sender.sendMessage(temp);
                                            sender.sendMessage(Variable.Lang_YML.getString("BottomLineTtitle"));
                                            return false;
                                        }
                                    }

                                    List<String> OP_List = yamlConfiguration.getStringList("OP");
                                    if (OP_List == null) {
                                        OP_List = new ArrayList<>();
                                    }

                                    Boolean CheckSame = false;

                                    for (int i = 0; i < OP_List.size(); i++) {
                                        if (OP_List.get(i).equalsIgnoreCase(Name)) {
                                            CheckSame = true;
                                            String temp = Variable.Lang_YML.getString("HasAlreadyOP");
                                            sender.sendMessage(Variable.Lang_YML.getString("HeadLineTtitle"));
                                            sender.sendMessage(temp);
                                            sender.sendMessage(Variable.Lang_YML.getString("BottomLineTtitle"));
                                            return false;
                                        }
                                    }

                                    if (!CheckSame) {
                                        File be_invite_file = new File(Variable.Tempf, String.valueOf(args[1]) + ".yml");
                                        if (be_invite_file.exists()) {
                                            String temp = Variable.Lang_YML.getString("InvitePlayerWhoHasCreateHome");
                                            sender.sendMessage(Variable.Lang_YML.getString("HeadLineTtitle"));
                                            sender.sendMessage(temp);
                                            sender.sendMessage(Variable.Lang_YML.getString("BottomLineTtitle"));
                                            return false;
                                        }

                                        boolean has_been_join = false;
                                        File folder = new File(Variable.Tempf);
                                        File[] arrayOfFile;
                                        int j = (arrayOfFile = folder.listFiles()).length;

                                        for (int b = 0; b < j; b++) {
                                            File temp = arrayOfFile[b];
                                            String want_to = temp.getPath()
                                                .replace(Variable.Tempf, "")
                                                .replace(".yml", "")
                                                .replace(Variable.file_loc_prefix, "");
                                            YamlConfiguration yamlConfiguration1 = YamlConfiguration.loadConfiguration(temp);

                                            for (int i = 0; i < yamlConfiguration1.getStringList("OP").size(); i++) {
                                                String temp_str = (String)yamlConfiguration1.getStringList("OP").get(i);
                                                if (temp_str.equalsIgnoreCase(args[1])) {
                                                    has_been_join = true;
                                                    break;
                                                }
                                            }
                                        }

                                        if (has_been_join) {
                                            String temp = Variable.Lang_YML.getString("InvitePlayerWhoHasJoinOthers");
                                            sender.sendMessage(Variable.Lang_YML.getString("HeadLineTtitle"));
                                            sender.sendMessage(temp);
                                            sender.sendMessage(Variable.Lang_YML.getString("BottomLineTtitle"));
                                            return false;
                                        }

                                        if (Variable.invite_list.containsKey(p.getName())) {
                                            String temp = Variable.Lang_YML.getString("HasInInviteCooldown");
                                            sender.sendMessage(Variable.Lang_YML.getString("HeadLineTtitle"));
                                            sender.sendMessage(temp);
                                            sender.sendMessage(Variable.Lang_YML.getString("BottomLineTtitle"));
                                            return false;
                                        }

                                        if (Variable.invite_list.containsValue(args[1])) {
                                            String temp = Variable.Lang_YML.getString("InvitePlayerWhoHasBeenAlreadyInvited");
                                            sender.sendMessage(Variable.Lang_YML.getString("HeadLineTtitle"));
                                            sender.sendMessage(temp);
                                            sender.sendMessage(Variable.Lang_YML.getString("BottomLineTtitle"));
                                            return false;
                                        }

                                        if (OP_List.size() + 2 > Main.JavaPlugin.getConfig().getInt("MaxOP")) {
                                            int max_player = Main.JavaPlugin.getConfig().getInt("MaxOP");

                                            for (int i = Main.JavaPlugin.getConfig().getInt("MaxOP") * 3; i > Main.JavaPlugin.getConfig().getInt("MaxOP"); i--) {
                                                if (p.hasPermission("SelfHome.MaxOP." + i)) {
                                                    max_player = i;
                                                    break;
                                                }
                                            }

                                            if (OP_List.size() + 2 > max_player) {
                                                String temp = Variable.Lang_YML.getString("ReachMaxOP");
                                                if (temp.contains("<MaxAmount>")) {
                                                    temp = temp.replace("<MaxAmount>", String.valueOf(max_player));
                                                }

                                                sender.sendMessage(Variable.Lang_YML.getString("HeadLineTtitle"));
                                                sender.sendMessage(temp);
                                                sender.sendMessage(Variable.Lang_YML.getString("BottomLineTtitle"));
                                            } else {
                                                Player be_invite = Bukkit.getPlayer(args[1]);
                                                if (be_invite == null) {
                                                    String temp = Variable.Lang_YML.getString("NoPlayerExist");
                                                    sender.sendMessage(Variable.Lang_YML.getString("HeadLineTtitle"));
                                                    sender.sendMessage(temp);
                                                    sender.sendMessage(Variable.Lang_YML.getString("BottomLineTtitle"));
                                                    return false;
                                                }

                                                Variable.invite_list.put(p.getName(), args[1]);
                                                this.invite_guoqi(p);
                                                String temp = Variable.Lang_YML.getString("SendInviteToPlayer");
                                                if (temp.contains("<Name>")) {
                                                    temp = temp.replace("<Name>", args[1]);
                                                }

                                                sender.sendMessage(Variable.Lang_YML.getString("HeadLineTtitle"));
                                                sender.sendMessage(temp);
                                                sender.sendMessage(Variable.Lang_YML.getString("BottomLineTtitle"));
                                                if (be_invite != null) {
                                                    String temp2 = Variable.Lang_YML.getString("InviteMessage");
                                                    if (temp2.contains("<player>")) {
                                                        temp2 = temp2.replace("<player>", p.getName());
                                                    }

                                                    if (Variable.has_no_click_message) {
                                                        be_invite.sendMessage(temp2);
                                                    } else {
                                                        TextComponent Click_Message = new TextComponent(temp2);
                                                        Click_Message.setClickEvent(new ClickEvent(Action.RUN_COMMAND, "/sh accept"));
                                                        be_invite.spigot().sendMessage(Click_Message);
                                                    }
                                                }
                                            }

                                            return false;
                                        }

                                        Player be_invite = Bukkit.getPlayer(args[1]);
                                        if (be_invite == null) {
                                            String temp = Variable.Lang_YML.getString("NoPlayerExist");
                                            sender.sendMessage(Variable.Lang_YML.getString("HeadLineTtitle"));
                                            sender.sendMessage(temp);
                                            sender.sendMessage(Variable.Lang_YML.getString("BottomLineTtitle"));
                                            return false;
                                        }

                                        Variable.invite_list.put(p.getName(), args[1]);
                                        this.invite_guoqi(p);
                                        String temp = Variable.Lang_YML.getString("SendInviteToPlayer");
                                        if (temp.contains("<Name>")) {
                                            temp = temp.replace("<Name>", args[1]);
                                        }

                                        sender.sendMessage(Variable.Lang_YML.getString("HeadLineTtitle"));
                                        sender.sendMessage(temp);
                                        sender.sendMessage(Variable.Lang_YML.getString("BottomLineTtitle"));
                                        if (be_invite != null) {
                                            String temp2 = Variable.Lang_YML.getString("InviteMessage");
                                            if (temp2.contains("<player>")) {
                                                temp2 = temp2.replace("<player>", p.getName());
                                            }

                                            if (Variable.has_no_click_message) {
                                                be_invite.sendMessage(temp2);
                                            } else {
                                                TextComponent Click_Message = new TextComponent(temp2);
                                                Click_Message.setClickEvent(new ClickEvent(Action.RUN_COMMAND, "/sh accept"));
                                                be_invite.spigot().sendMessage(Click_Message);
                                            }
                                        }
                                    }
                                }

                                return false;
                            }
                        } else if (!Main.JavaPlugin.getConfig().getBoolean("Permission.Visit") && !p.hasPermission("SelfHome.Visit")) {
                            String tip = Variable.Lang_YML.getString("NoPermissionCheck");
                            if (tip.contains("<Permission>")) {
                                tip = tip.replace("<Permission>", "SelfHome.Visit");
                            }

                            p.sendMessage(tip);
                            return false;
                        } else {
                            if (Variable.bungee) {
                                if (Util.CheckIsHome(args[1])) {
                                    int amount = 0;

                                    for (Player count_p : Bukkit.getOnlinePlayers()) {
                                        if (count_p.getWorld().getName().replace(Variable.world_prefix, "").equalsIgnoreCase(args[1])) {
                                            amount++;
                                        }
                                    }

                                    boolean has_been_load = false;
                                    if (Main.JavaPlugin.getConfig().getBoolean("MoveWorldAfterUnLoad")
                                        && Bukkit.getWorld(Variable.world_prefix + args[1]) != null) {
                                        has_been_load = true;
                                    }

                                    if (Main.JavaPlugin.getConfig().getBoolean("AutoMoveWorldFilesToOther")
                                        && !has_been_load
                                        && !Variable.wait_to_command.containsKey(p.getName())
                                        && Main.JavaPlugin.getConfig().getBoolean("BungeeCord")
                                        && !Variable.has_already_move_world.contains(p.getName())
                                        && amount == 0) {
                                        if (Main.JavaPlugin.getConfig().getString("DecideBy").equalsIgnoreCase("Player")) {
                                            if (!MySQL.getLowerstLagServer().equalsIgnoreCase(MySQL.getServer(args[1]))
                                                && MySQL.getServerAmount(MySQL.getLowerstLagServer()) != Bukkit.getOnlinePlayers().size()) {
                                                try {
                                                    /* excluded Channel */
                                                } catch (Exception e) {
                                                    e.printStackTrace();
                                                }

                                                p.sendMessage(Variable.Lang_YML.getString("StartLowestLagServer"));
                                                /* excluded Channel */
                                                return false;
                                            }
                                        } else if (!MySQL.getHighestTPSServer().equalsIgnoreCase(MySQL.getServer(args[1]))) {
                                            double now = 0.0;
                                            if (Bukkit.getVersion().contains("1.7.10")) {
                                                now = Bukkit.getTPS()[0];
                                            } else {
                                                double se1 = Double.valueOf(PlaceholderAPI.setPlaceholders(null, "%server_tps_1%").replace("*", ""));
                                                double se2 = Double.valueOf(PlaceholderAPI.setPlaceholders(null, "%server_tps_5%").replace("*", ""));
                                                double se3 = Double.valueOf(PlaceholderAPI.setPlaceholders(null, "%server_tps_15%").replace("*", ""));
                                                now = (se1 + se2 + se3) / 3.0;
                                            }

                                            if (MySQL.getServerAmount(MySQL.getLowerstLagServer()) != now) {
                                                try {
                                                    /* excluded Channel */
                                                } catch (Exception e) {
                                                    e.printStackTrace();
                                                }

                                                p.sendMessage(Variable.Lang_YML.getString("StartLowestLagServer"));
                                                /* excluded Channel */
                                                return false;
                                            }
                                        }
                                    }

                                    if (MySQL.getServer(args[1]).equalsIgnoreCase(Main.JavaPlugin.getConfig().getString("Server"))) {
                                        World world = Bukkit.getWorld(Variable.world_prefix + args[1]);
                                        WorldCreator creator = new WorldCreator(Variable.world_prefix + args[1]);
                                        Variable.create_list_home.add(Variable.world_prefix + args[1]);
                                        Bukkit.createWorld(creator);
                                        world = Bukkit.getWorld(Variable.world_prefix + args[1]);
                                        Location loc = world.getSpawnLocation();
                                        loc = Util.getNotAir(loc);
                                        loc.setX(Double.valueOf(MySQL.getX(args[1])));
                                        loc.setY(Double.valueOf(MySQL.getY(args[1])));
                                        loc.setZ(Double.valueOf(MySQL.getZ(args[1])));
                                        p.teleport(loc);
                                    } else {
                                        try {
                                            /* excluded Channel */
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        }

                                        /* excluded Channel */
                                    }
                                } else {
                                    String temp = Variable.Lang_YML.getString("TpNotExist");
                                    sender.sendMessage(Variable.Lang_YML.getString("HeadLineTtitle"));
                                    sender.sendMessage(temp);
                                    sender.sendMessage(Variable.Lang_YML.getString("BottomLineTtitle"));
                                }
                            } else {
                                File f = new File(Variable.Tempf, String.valueOf(args[1]) + ".yml");
                                if (f.exists()) {
                                    YamlConfiguration yamlConfiguration = YamlConfiguration.loadConfiguration(f);
                                    World world = Bukkit.getWorld(Variable.world_prefix + args[1]);
                                    WorldCreator creator = new WorldCreator(Variable.world_prefix + args[1]);
                                    Variable.create_list_home.add(Variable.world_prefix + args[1]);
                                    Bukkit.createWorld(creator);
                                    world = Bukkit.getWorld(Variable.world_prefix + args[1]);
                                    Location loc = world.getSpawnLocation();
                                    loc = Util.getNotAir(loc);
                                    loc.setX(yamlConfiguration.getDouble("X"));
                                    loc.setY(yamlConfiguration.getDouble("Y"));
                                    loc.setZ(yamlConfiguration.getDouble("Z"));
                                    p.teleport(loc);
                                } else {
                                    String temp = Variable.Lang_YML.getString("TpNotExist");
                                    sender.sendMessage(Variable.Lang_YML.getString("HeadLineTtitle"));
                                    sender.sendMessage(temp);
                                    sender.sendMessage(Variable.Lang_YML.getString("BottomLineTtitle"));
                                }
                            }

                            return false;
                        }
                    } else {
                        if (Variable.bungee) {
                            if (!MySQL.alreadyhastheplayerjoin(p.getName()) && !MySQL.alreadyhastheplayerhome(p.getName())) {
                                String temp = Variable.Lang_YML.getString("NoCreateOrJoin");
                                sender.sendMessage(Variable.Lang_YML.getString("HeadLineTtitle"));
                                sender.sendMessage(temp);
                                sender.sendMessage(Variable.Lang_YML.getString("BottomLineTtitle"));
                                return false;
                            }

                            if (MySQL.alreadyhastheplayerjoin(p.getName())) {
                                int amount = 0;

                                for (Player count_p : Bukkit.getOnlinePlayers()) {
                                    if (count_p.getWorld().getName().replace(Variable.world_prefix, "").equalsIgnoreCase(MySQL.getJoinHome(p.getName()))) {
                                        amount++;
                                    }
                                }

                                boolean has_been_load = false;
                                if (Main.JavaPlugin.getConfig().getBoolean("MoveWorldAfterUnLoad")
                                    && Bukkit.getWorld(Variable.world_prefix + MySQL.getJoinHome(p.getName())) != null) {
                                    has_been_load = true;
                                }

                                if (Main.JavaPlugin.getConfig().getBoolean("AutoMoveWorldFilesToOther")
                                    && !has_been_load
                                    && !Variable.wait_to_command.containsKey(p.getName())
                                    && Main.JavaPlugin.getConfig().getBoolean("BungeeCord")
                                    && !Variable.has_already_move_world.contains(p.getName())
                                    && amount == 0) {
                                    if (Main.JavaPlugin.getConfig().getString("DecideBy").equalsIgnoreCase("Player")) {
                                        if (!MySQL.getLowerstLagServer().equalsIgnoreCase(MySQL.getServer(MySQL.getJoinHome(p.getName())))
                                            && MySQL.getServerAmount(MySQL.getLowerstLagServer()) != Bukkit.getOnlinePlayers().size()) {
                                            try {
                                                /* excluded Channel */
                                            } catch (Exception e) {
                                                e.printStackTrace();
                                            }

                                            p.sendMessage(Variable.Lang_YML.getString("StartLowestLagServer"));
                                            /* excluded Channel */
                                            return false;
                                        }
                                    } else if (!MySQL.getHighestTPSServer().equalsIgnoreCase(MySQL.getServer(MySQL.getJoinHome(p.getName())))) {
                                        double now = 0.0;
                                        if (Bukkit.getVersion().contains("1.7.10")) {
                                            now = Bukkit.getTPS()[0];
                                        } else {
                                            double se1 = Double.valueOf(PlaceholderAPI.setPlaceholders(null, "%server_tps_1%").replace("*", ""));
                                            double se2 = Double.valueOf(PlaceholderAPI.setPlaceholders(null, "%server_tps_5%").replace("*", ""));
                                            double se3 = Double.valueOf(PlaceholderAPI.setPlaceholders(null, "%server_tps_15%").replace("*", ""));
                                            now = (se1 + se2 + se3) / 3.0;
                                        }

                                        if (MySQL.getServerAmount(MySQL.getLowerstLagServer()) != now) {
                                            try {
                                                /* excluded Channel */
                                            } catch (Exception e) {
                                                e.printStackTrace();
                                            }

                                            p.sendMessage(Variable.Lang_YML.getString("StartLowestLagServer"));
                                            /* excluded Channel */
                                            return false;
                                        }
                                    }
                                }

                                if (!MySQL.getJoinServer(p.getName()).equalsIgnoreCase(Main.JavaPlugin.getConfig().getString("Server"))) {
                                    try {
                                        /* excluded Channel */
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }

                                    /* excluded Channel */
                                    return false;
                                }
                            }

                            if (MySQL.alreadyhastheplayerhome(p.getName())) {
                                int amount = 0;

                                for (Player count_p : Bukkit.getOnlinePlayers()) {
                                    if (count_p.getWorld().getName().replace(Variable.world_prefix, "").equalsIgnoreCase(p.getName())) {
                                        amount++;
                                    }
                                }

                                boolean has_been_load = false;
                                if (Main.JavaPlugin.getConfig().getBoolean("MoveWorldAfterUnLoad")
                                    && Bukkit.getWorld(Variable.world_prefix + p.getName()) != null) {
                                    has_been_load = true;
                                }

                                if (Main.JavaPlugin.getConfig().getBoolean("AutoMoveWorldFilesToOther")
                                    && !has_been_load
                                    && !Variable.wait_to_command.containsKey(p.getName())
                                    && Main.JavaPlugin.getConfig().getBoolean("BungeeCord")
                                    && !Variable.has_already_move_world.contains(p.getName())
                                    && amount == 0) {
                                    if (Main.JavaPlugin.getConfig().getString("DecideBy").equalsIgnoreCase("Player")) {
                                        if (!MySQL.getLowerstLagServer().equalsIgnoreCase(MySQL.getServer(p.getName()))
                                            && MySQL.getServerAmount(MySQL.getLowerstLagServer()) != Bukkit.getOnlinePlayers().size()) {
                                            try {
                                                /* excluded Channel */
                                            } catch (Exception e) {
                                                e.printStackTrace();
                                            }

                                            p.sendMessage(Variable.Lang_YML.getString("StartLowestLagServer"));
                                            /* excluded Channel */
                                            return false;
                                        }
                                    } else if (!MySQL.getHighestTPSServer().equalsIgnoreCase(MySQL.getServer(p.getName()))) {
                                        double now = 0.0;
                                        if (Bukkit.getVersion().contains("1.7.10")) {
                                            now = Bukkit.getTPS()[0];
                                        } else {
                                            double se1 = Double.valueOf(PlaceholderAPI.setPlaceholders(null, "%server_tps_1%").replace("*", ""));
                                            double se2 = Double.valueOf(PlaceholderAPI.setPlaceholders(null, "%server_tps_5%").replace("*", ""));
                                            double se3 = Double.valueOf(PlaceholderAPI.setPlaceholders(null, "%server_tps_15%").replace("*", ""));
                                            now = (se1 + se2 + se3) / 3.0;
                                        }

                                        if (MySQL.getServerAmount(MySQL.getLowerstLagServer()) != now) {
                                            try {
                                                /* excluded Channel */
                                            } catch (Exception e) {
                                                e.printStackTrace();
                                            }

                                            p.sendMessage(Variable.Lang_YML.getString("StartLowestLagServer"));
                                            /* excluded Channel */
                                            return false;
                                        }
                                    }
                                }

                                if (!MySQL.getServer(p.getName()).equalsIgnoreCase(Main.JavaPlugin.getConfig().getString("Server"))) {
                                    try {
                                        /* excluded Channel */
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }

                                    /* excluded Channel */
                                    return false;
                                }
                            }

                            if (MySQL.alreadyhastheplayerjoin(p.getName())) {
                                World world = Bukkit.getWorld(Variable.world_prefix + MySQL.getJoinHome(p.getName()));
                                if (world == null) {
                                    WorldCreator creator = new WorldCreator(Variable.world_prefix + MySQL.getJoinHome(p.getName()));
                                    Variable.create_list_home.add(Variable.world_prefix + MySQL.getJoinHome(p.getName()));
                                    Bukkit.createWorld(creator);
                                }

                                world = Bukkit.getWorld(Variable.world_prefix + MySQL.getJoinHome(p.getName()));
                                Location loc = world.getSpawnLocation();
                                loc = Util.getNotAir(loc);
                                loc.setX(Double.valueOf(MySQL.getX(MySQL.getJoinHome(p.getName()))));
                                loc.setY(Double.valueOf(MySQL.getY(MySQL.getJoinHome(p.getName()))));
                                loc.setZ(Double.valueOf(MySQL.getZ(MySQL.getJoinHome(p.getName()))));
                                p.teleport(loc);
                            } else {
                                World world = Bukkit.getWorld(Variable.world_prefix + p.getName());
                                if (world == null) {
                                    WorldCreator creator = new WorldCreator(Variable.world_prefix + p.getName());
                                    Variable.create_list_home.add(Variable.world_prefix + p.getName());
                                    Bukkit.createWorld(creator);
                                }

                                world = Bukkit.getWorld(Variable.world_prefix + p.getName());
                                Location loc = world.getSpawnLocation();
                                loc = Util.getNotAir(loc);
                                loc.setX(Double.valueOf(MySQL.getX(p.getName())));
                                loc.setY(Double.valueOf(MySQL.getY(p.getName())));
                                loc.setZ(Double.valueOf(MySQL.getZ(p.getName())));
                                p.teleport(loc);
                            }
                        } else {
                            String what_has_been_join = "";
                            boolean has_been_join = false;
                            File folder = new File(Variable.Tempf);
                            File[] arrayOfFile;
                            int j = (arrayOfFile = folder.listFiles()).length;

                            for (int b = 0; b < j; b++) {
                                File temp = arrayOfFile[b];
                                String want_to = temp.getPath().replace(Variable.Tempf, "").replace(".yml", "").replace(Variable.file_loc_prefix, "");
                                YamlConfiguration yamlConfiguration = YamlConfiguration.loadConfiguration(temp);

                                for (int i = 0; i < yamlConfiguration.getStringList("OP").size(); i++) {
                                    String temp_str = (String)yamlConfiguration.getStringList("OP").get(i);
                                    if (temp_str.equalsIgnoreCase(p.getName())) {
                                        what_has_been_join = want_to;
                                        has_been_join = true;
                                        break;
                                    }
                                }
                            }

                            File f2 = new File(Variable.Tempf, String.valueOf(p.getName()) + ".yml");
                            if (!f2.exists() && !has_been_join) {
                                String temp = Variable.Lang_YML.getString("NoCreateOrJoin");
                                sender.sendMessage(Variable.Lang_YML.getString("HeadLineTtitle"));
                                sender.sendMessage(temp);
                                sender.sendMessage(Variable.Lang_YML.getString("BottomLineTtitle"));
                                return false;
                            }

                            if (!what_has_been_join.equalsIgnoreCase("")) {
                                World world = Bukkit.getWorld(Variable.world_prefix + what_has_been_join);
                                if (world == null) {
                                    WorldCreator creator = new WorldCreator(Variable.world_prefix + what_has_been_join);
                                    Variable.create_list_home.add(Variable.world_prefix + what_has_been_join);
                                    Bukkit.createWorld(creator);
                                }

                                world = Bukkit.getWorld(Variable.world_prefix + what_has_been_join);
                                Location loc = world.getSpawnLocation();
                                loc = Util.getNotAir(loc);
                                File tp_set = new File(Variable.Tempf, String.valueOf(world.getName().replace(Variable.world_prefix, "")) + ".yml");
                                YamlConfiguration yamlConfiguration = YamlConfiguration.loadConfiguration(tp_set);
                                loc.setX(yamlConfiguration.getDouble("X"));
                                loc.setY(yamlConfiguration.getDouble("Y"));
                                loc.setZ(yamlConfiguration.getDouble("Z"));
                                loc = Util.getNotAir(loc);
                                p.teleport(loc);
                            } else {
                                World world = Bukkit.getWorld(Variable.world_prefix + p.getName());
                                if (world == null) {
                                    WorldCreator creator = new WorldCreator(Variable.world_prefix + p.getName());
                                    Variable.create_list_home.add(Variable.world_prefix + p.getName());
                                    Bukkit.createWorld(creator);
                                }

                                world = Bukkit.getWorld(Variable.world_prefix + p.getName());
                                Location loc = world.getSpawnLocation();
                                loc = Util.getNotAir(loc);
                                File tp_set = new File(Variable.Tempf, String.valueOf(p.getName()) + ".yml");
                                YamlConfiguration yamlConfiguration = YamlConfiguration.loadConfiguration(tp_set);
                                loc.setX(yamlConfiguration.getDouble("X"));
                                loc.setY(yamlConfiguration.getDouble("Y"));
                                loc.setZ(yamlConfiguration.getDouble("Z"));
                                loc = Util.getNotAir(loc);
                                p.teleport(loc);
                            }
                        }

                        return false;
                    }
                }
            } else {
                MainGui gui = new MainGui(p);
                p.openInventory(gui.getInventory());
                return false;
            }
        }
    }

    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1) {
            List<String> list = new ArrayList<>();
            list.add("open");
            list.add("create");
            list.add("look");
            list.add("tpSet");
            list.add("invite");
            list.add("accept");
            list.add("deny");
            list.add("add");
            list.add("pvp");
            list.add("drop");
            list.add("pickup");
            list.add("public");
            list.add("setspawn");
            list.add("kick");
            list.add("remove");
            list.add("check");
            list.add("rank");
            list.add("sun");
            list.add("rain");
            list.add("night");
            list.add("day");
            list.add("lockTime");
            list.add("lockWeather");
            list.add("reload");
            list.add("mobs");
            list.add("nbt");
            list.add("admin");
            list.add("wholedelete");
            list.add("forceDelete");
            list.add("unLoad");
            list.add("MobSpawn");
            list.add("GameMode");
            list.add("flower");
            list.add("popularity");
            list.add("gift");
            list.add("icon");
            list.add("info");
            list.add("setBiome");
            return list;
        }

        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("info")) {
                List<String> list = new ArrayList<>();
                list.add("第一,第二,第三行[逗号为分隔符]");
                return list;
            }

            if (args[0].equalsIgnoreCase("create")) {
                List<String> list = new ArrayList<>();
                list.add("1");
                list.add("2");
                list.add("其他类型");
                return list;
            }

            if (args[0].equalsIgnoreCase("invite")) {
                List<String> list = new ArrayList<>();

                for (Player p : Bukkit.getOnlinePlayers()) {
                    list.add(p.getName());
                }

                return list;
            }

            if (args[0].equalsIgnoreCase("kick")) {
                List<String> list = new ArrayList<>();

                for (Player p : Bukkit.getOnlinePlayers()) {
                    list.add(p.getName());
                }

                return list;
            }

            if (args[0].equalsIgnoreCase("add")) {
                List<String> list = new ArrayList<>();

                for (Player p : Bukkit.getOnlinePlayers()) {
                    list.add(p.getName());
                }

                return list;
            }

            if (args[0].equalsIgnoreCase("remove")) {
                List<String> list = new ArrayList<>();

                for (Player p : Bukkit.getOnlinePlayers()) {
                    list.add(p.getName());
                }

                return list;
            }

            if (args[0].equalsIgnoreCase("deny")) {
                List<String> list = new ArrayList<>();

                for (Player p : Bukkit.getOnlinePlayers()) {
                    list.add(p.getName());
                }

                return list;
            }

            if (args[0].equalsIgnoreCase("SetBiome")) {
                List<String> list = new ArrayList<>();
                Biome[] var9;
                int var8 = (var9 = Biome.values()).length;

                for (int var49 = 0; var49 < var8; var49++) {
                    Biome b = var9[var49];
                    list.add(b.toString());
                }

                return list;
            }

            if (args[0].equalsIgnoreCase("undeny")) {
                List<String> list = new ArrayList<>();

                for (Player p : Bukkit.getOnlinePlayers()) {
                    list.add(p.getName());
                }

                return list;
            }

            if (args[0].equalsIgnoreCase("rank")) {
                List<String> list = new ArrayList<>();
                list.add("1");
                list.add("2");
                list.add("3");
                list.add("4");
                list.add("5");
                list.add("6");
                return list;
            }

            if (args[0].equalsIgnoreCase("unLoad")) {
                List<String> list = new ArrayList<>();

                for (World p : Bukkit.getWorlds()) {
                    list.add(p.getName());
                }

                return list;
            }

            if (args[0].equalsIgnoreCase("forcedelete")) {
                List<String> list = new ArrayList<>();

                for (World p : Bukkit.getWorlds()) {
                    list.add(p.getName().replaceAll(Variable.world_prefix, ""));
                }

                return list;
            }

            if (args[0].equalsIgnoreCase("admin")) {
                List<String> list = new ArrayList<>();
                list.add("setSpawn");
                list.add("resetDelete");
                list.add("dimension");
                list.add("export");
                list.add("import");
                list.add("setlevel");
                list.add("pwp");
                return list;
            }

            if (args[0].equalsIgnoreCase("gift")) {
                List<String> list = new ArrayList<>();
                list.add("open");
                list.add("send");
                list.add("inv");
                return list;
            }

            if (args[0].equalsIgnoreCase("flower")) {
                List<String> list = new ArrayList<>();
                list.add("add");
                return list;
            }

            if (args[0].equalsIgnoreCase("popularity")) {
                List<String> list = new ArrayList<>();
                list.add("add");
                return list;
            }

            if (args[0].equalsIgnoreCase("GameMode")) {
                List<String> list = new ArrayList<>();
                list.add("EASY");
                list.add("HARD");
                list.add("PEACEFUL");
                return list;
            }
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("admin") && args[1].equalsIgnoreCase("resetdelete")) {
            List<String> list = new ArrayList<>();
            if (!sender.hasPermission("SelfHome.Admin.ResetDelete")) {
                return list;
            }

            YamlConfiguration yamlConfiguration = YamlConfiguration.loadConfiguration(Variable.f_log);
            for (String deleteTime : yamlConfiguration.getStringList("DeleteTimes")) {
                String[] entry = deleteTime.split(",", 2);
                if (entry.length > 0 && !entry[0].isEmpty()) {
                    list.add(entry[0]);
                }
            }
            return list;
        } else if (args.length == 3 && args[0].equalsIgnoreCase("gift") && args[1].equalsIgnoreCase("send")) {
            List<String> list = new ArrayList<>();

            for (Player p : Bukkit.getOnlinePlayers()) {
                list.add(p.getName());
            }

            return list;
        } else if (args.length == 3 && args[0].equalsIgnoreCase("gift") && args[1].equalsIgnoreCase("inv")) {
            List<String> list = new ArrayList<>();

            for (Player p : Bukkit.getOnlinePlayers()) {
                list.add(p.getName());
            }

            return list;
        } else if (args.length == 3 && args[0].equalsIgnoreCase("flower") && args[1].equalsIgnoreCase("add")) {
            List<String> list = new ArrayList<>();

            for (Player p : Bukkit.getOnlinePlayers()) {
                list.add(p.getName());
            }

            return list;
        } else if (args.length == 3 && args[0].equalsIgnoreCase("popularity") && args[1].equalsIgnoreCase("add")) {
            List<String> list = new ArrayList<>();

            for (Player p : Bukkit.getOnlinePlayers()) {
                list.add(p.getName());
            }

            return list;
        } else {
            return null;
        }
    }
}
