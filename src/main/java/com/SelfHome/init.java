package com.SelfHome;

import com.Util.Home;
import com.Util.HomeAPI;
import com.Util.MySQL;
import com.Util.StaticsTick;
import com.Util.Util;
import com.Util.ZIP;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

public class init implements Listener {
    public static HashMap<String, List<String>> OPS_redis = new HashMap<>();
    public static HashMap<String, List<String>> MEMBERS_redis = new HashMap<>();

    public static void refreshWorldStatics(boolean broad) {
        Variable.list_home.clear();
        if (Variable.bungee) {
            for (String str : MySQL.getAllWorlds()) {
                Variable.list_home.add(str);
            }
        } else {
            File folder = new File(String.valueOf(Main.JavaPlugin.getDataFolder().getPath().toString()) + Variable.file_loc_prefix + "playerdata");
            File[] arrayOfFile;
            int k = (arrayOfFile = folder.listFiles()).length;

            for (int b = 0; b < k; b++) {
                File temp = arrayOfFile[b];
                String want_to = temp.getPath()
                    .replace(String.valueOf(Main.JavaPlugin.getDataFolder().getPath().toString()) + Variable.file_loc_prefix + "playerdata", "")
                    .replace(Variable.file_loc_prefix, "")
                    .replace(".yml", "");
                Variable.list_home.add(want_to);
            }
        }

        Variable.world_StaticsTick.clear();
        boolean check_has = false;

        for (World world : Bukkit.getWorlds()) {
            if (Util.CheckIsHome(world.getName())) {
                check_has = true;
                int chunks = 0;
                int tiles = 0;
                int entity = 0;
                int dropitem = 0;
                Chunk[] arrayOfChunk;
                int k = (arrayOfChunk = world.getLoadedChunks()).length;

                for (int b = 0; b < k; b++) {
                    Chunk chunk = arrayOfChunk[b];
                    chunks++;
                    BlockState[] arrayOfBlockState;
                    int m = (arrayOfBlockState = chunk.getTileEntities()).length;

                    int b1;
                    for (b1 = 0; b1 < m; b1++) {
                        BlockState bs = arrayOfBlockState[b1];
                        tiles++;
                    }

                    Entity[] arrayOfEntity;
                    for (int e = (arrayOfEntity = chunk.getEntities()).length; b1 < e; b1++) {
                        Entity et = arrayOfEntity[b1];
                        if (et.getType() != EntityType.ITEM) {
                            entity++;
                        } else {
                            Item i = (Item)et;
                            dropitem += i.getItemStack().getAmount();
                        }
                    }
                }

                double calc_tps = tiles * Main.JavaPlugin.getConfig().getDouble("OneTileTick")
                    + entity * Main.JavaPlugin.getConfig().getDouble("OneEntityTick")
                    + dropitem * Main.JavaPlugin.getConfig().getDouble("OneDropTick")
                    + chunks * Main.JavaPlugin.getConfig().getDouble("OneChunkTick");
                StaticsTick temp = new StaticsTick(world.getName().replaceAll(Variable.world_prefix, ""), tiles, chunks, entity, dropitem, calc_tps);
                Variable.world_StaticsTick.add(temp);
            }
        }

        if (check_has) {
            if (broad) {
                for (int c = 0; c < Main.JavaPlugin.getConfig().getStringList("StatisticsTop").size(); c++) {
                    String a = (String)Main.JavaPlugin.getConfig().getStringList("StatisticsTop").get(c);
                    Bukkit.broadcastMessage(a);
                }

                for (int i = 0; i < Variable.world_StaticsTick.size() - 1; i++) {
                    for (int j = 0; j < Variable.world_StaticsTick.size() - 1 - i; j++) {
                        if (Variable.world_StaticsTick.get(j).tps < Variable.world_StaticsTick.get(j + 1).tps) {
                            StaticsTick temp = Variable.world_StaticsTick.get(j);
                            Variable.world_StaticsTick.set(j, Variable.world_StaticsTick.get(j + 1));
                            Variable.world_StaticsTick.set(j + 1, temp);
                        }
                    }
                }

                for (int i = 0; i < Variable.world_StaticsTick.size() && i < Main.JavaPlugin.getConfig().getInt("ShowAmount"); i++) {
                    StaticsTick s = Variable.world_StaticsTick.get(i);
                    if (s.tps != 0.0) {
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

                        Bukkit.broadcastMessage(temp);
                    }
                }

                for (int c = 0; c < Main.JavaPlugin.getConfig().getStringList("StatisticsEnd").size(); c++) {
                    String a = (String)Main.JavaPlugin.getConfig().getStringList("StatisticsEnd").get(c);
                    Bukkit.broadcastMessage(a);
                }
            }
        }
    }

    public static void init() {
        if (Main.JavaPlugin.getConfig().getBoolean("BorderSwitch")) {
            (new BukkitRunnable() {
                    public void run() {
                        for (World world : Bukkit.getWorlds()) {
                            if (Util.CheckIsHome(world.getName().replace(Variable.world_prefix, ""))) {
                                Home home = HomeAPI.getHome(world.getName().replace(Variable.world_prefix, ""));
                                if (Main.JavaPlugin.getConfig().getBoolean("BorderSwitch")) {
                                    try {
                                        world.getWorldBorder().setCenter(world.getSpawnLocation());
                                        world.getWorldBorder()
                                            .setSize(
                                                Main.JavaPlugin.getConfig().getInt("WorldBoard")
                                                    + (home.getLevel() - 1) * Main.JavaPlugin.getConfig().getInt("UpdateRadius")
                                            );

                                        for (Player p : world.getPlayers()) {
                                            if (Variable.has_already_hide_border.contains(p.getName())) {
                                            } else {
                                            }
                                        }
                                    } catch (NoSuchMethodError e) {
                                        Bukkit.getConsoleSender().sendMessage(Variable.Lang_YML.getString("BorderException"));
                                    }
                                }
                            }
                        }
                    }
                })
                .runTaskTimer(Main.JavaPlugin, 0L, 60L);
        }

        (new BukkitRunnable() {
            public void run() {
                Calendar cal = Calendar.getInstance();
                int hour = cal.getTime().getHours();
                int minute = cal.getTime().getMinutes();
                int seconds = cal.getTime().getSeconds();
                if (hour == 0 && minute == 0 && seconds == 0) {
                    Variable.popularity_list.clear();
                    Variable.flowers_list.clear();
                }
            }
        }).runTaskTimerAsynchronously(Main.JavaPlugin, 0L, 20L);
        (new BukkitRunnable() {
                public void run() {
                    Variable.list_home.clear();
                    if (Variable.bungee) {
                        for (String str : MySQL.getAllWorlds()) {
                            Variable.list_home.add(str);
                        }
                    } else {
                        File folder = new File(String.valueOf(Main.JavaPlugin.getDataFolder().getPath().toString()) + Variable.file_loc_prefix + "playerdata");
                        File[] arrayOfFile;
                        int i = (arrayOfFile = folder.listFiles()).length;

                        for (int b = 0; b < i; b++) {
                            File temp = arrayOfFile[b];
                            String want_to = temp.getPath()
                                .replace(String.valueOf(Main.JavaPlugin.getDataFolder().getPath().toString()) + Variable.file_loc_prefix + "playerdata", "")
                                .replace(Variable.file_loc_prefix, "")
                                .replace(".yml", "");
                            Variable.list_home.add(want_to);
                        }
                    }
                }
            })
            .runTaskTimer(Main.JavaPlugin, 0L, 100L);
        if (!Main.JavaPlugin.getConfig().getBoolean("DisableFunctionButTeleport")
            && Main.JavaPlugin.getConfig().getBoolean("EnableBlackItemsUseInNoPermission")) {
            (new BukkitRunnable() {
                    public void run() {
                        for (World world : Bukkit.getWorlds()) {
                            if (Util.CheckIsHome(world.getName().replace(Variable.world_prefix, ""))) {
                                label57:
                                for (Player p : world.getPlayers()) {
                                    if (!Util.Check(p, world.getName().replace(Variable.world_prefix, ""))) {
                                        ItemStack[] arrayOfItemStack;
                                        int k = (arrayOfItemStack = p.getInventory().getContents()).length;

                                        for (int b = 0; b < k; b++) {
                                            ItemStack i = arrayOfItemStack[b];
                                            String nbt = Util.getItemNBTString(i);

                                            for (int j = 0; j < Main.JavaPlugin.getConfig().getStringList("BlackItems").size(); j++) {
                                                if (nbt.toUpperCase()
                                                    .contains(((String)Main.JavaPlugin.getConfig().getStringList("BlackItems").get(j)).toUpperCase())) {
                                                    String command = Main.JavaPlugin.getConfig().getString("BeKickedCommand");
                                                    if (command.contains("<Name>")) {
                                                        command = command.replace("<Name>", p.getName());
                                                    }

                                                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
                                                    String message = Variable.Lang_YML.getString("TakeBlackItemsInNoPermissionHome");
                                                    if (message.contains("<type>")) {
                                                        message = message.replace(
                                                            "<type>", ((String)Main.JavaPlugin.getConfig().getStringList("BlackItems").get(j)).toUpperCase()
                                                        );
                                                    }

                                                    p.sendMessage(message);
                                                    continue label57;
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                })
                .runTaskTimerAsynchronously(Main.JavaPlugin, 0L, 20L);
        }

        if (!Main.JavaPlugin.getConfig().getBoolean("DisableFunctionButTeleport") && Main.JavaPlugin.getConfig().getBoolean("CustomEntityMax")) {
            (new BukkitRunnable() {
                public void run() {
                    for (World world : Bukkit.getWorlds()) {
                        if (Util.CheckIsHome(world.getName().replace(Variable.world_prefix, ""))) {
                            HashMap<String, Integer> entity_map = new HashMap<>();

                            for (Entity entity : world.getEntities()) {
                                String type = null;
                                type = entity.getType().toString().toUpperCase();

                                if (entity instanceof Animals) {
                                    type = "Animals";
                                }

                                if (!entity_map.containsKey(type)) {
                                    entity_map.put(type, 1);
                                } else {
                                    int now_amount = entity_map.get(type);

                                    for (int c = 0; c < Main.JavaPlugin.getConfig().getStringList("EntityList").size(); c++) {
                                        String[] args = ((String)Main.JavaPlugin.getConfig().getStringList("EntityList").get(c)).split("\\|");
                                        if (args[0].toUpperCase().contains(type.toUpperCase())) {
                                            int Max_Amount = Integer.valueOf(args[1]);
                                            if (now_amount > Max_Amount) {
                                                entity.remove();
                                            } else {
                                                entity_map.put(type, now_amount + 1);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }).runTaskTimer(Main.JavaPlugin, 0L, Main.JavaPlugin.getConfig().getLong("CheckEntityInterval") * 20L);
        }

        if (!Main.JavaPlugin.getConfig().getBoolean("DisableFunctionButTeleport")
            && Main.JavaPlugin.getConfig().getBoolean("EnableTilesAndChunksAndDropItemsStatisticsTop")) {
            (new BukkitRunnable() {
                public void run() {
                    init.refreshWorldStatics(true);
                }
            }).runTaskTimer(Main.JavaPlugin, 0L, Main.JavaPlugin.getConfig().getLong("ShowTimes") * 20L);
        }

        if (!Main.JavaPlugin.getConfig().getBoolean("DisableFunctionButTeleport")) {
            if (Main.JavaPlugin.getConfig().getLong("SaveTime") != 0L) {
                Bukkit.getConsoleSender().sendMessage(Variable.Lang_YML.getString("EnableAutoSaveWorld"));
                (new BukkitRunnable() {
                    public void run() {
                        for (World temp : Bukkit.getWorlds()) {
                            boolean is_jump = false;

                            for (int i = 0; i < Main.JavaPlugin.getConfig().getStringList("UnAutoSaveWorlds").size(); i++) {
                                String str = (String)Main.JavaPlugin.getConfig().getStringList("UnAutoSaveWorlds").get(i);
                                if (str.equalsIgnoreCase(temp.getName().replace(Variable.world_prefix, ""))) {
                                    is_jump = true;
                                    break;
                                }
                            }

                            if (!is_jump) {
                                temp.save();
                            }
                        }

                        Bukkit.getConsoleSender().sendMessage(Variable.Lang_YML.getString("AutoSaveSuccess"));
                    }
                }).runTaskTimer(Main.JavaPlugin, 0L, Main.JavaPlugin.getConfig().getLong("SaveTime") * 20L);
            } else {
                Bukkit.getConsoleSender().sendMessage(Variable.Lang_YML.getString("DisableAutoSaveWorld"));
            }
        }

        if (!Main.JavaPlugin.getConfig().getBoolean("DisableFunctionButTeleport")) {
            (new BukkitRunnable() {
                public void run() {
                    if (Main.JavaPlugin.getConfig().getInt("ArmorStand") != -1) {
                        for (World world : Bukkit.getWorlds()) {
                            if (Util.CheckIsHome(world.getName().replaceAll(Variable.world_prefix, ""))) {
                                int amount = 0;

                                for (Entity entity : world.getEntities()) {
                                    if (entity.getType() == EntityType.ARMOR_STAND && ++amount > Main.JavaPlugin.getConfig().getInt("ArmorStand")) {
                                        entity.remove();
                                        amount--;
                                    }
                                }
                            }
                        }
                    }
                }
            }).runTaskTimer(Main.JavaPlugin, 0L, 100L);
        }

        if (!Main.JavaPlugin.getConfig().getBoolean("DisableFunctionButTeleport")) {
            if (Main.JavaPlugin.getConfig().getLong("AutoBackup") != 0L) {
                (new BukkitRunnable() {
                    public void run() {
                        if (Variable.check_first_start) {
                            Bukkit.getConsoleSender().sendMessage(Variable.Lang_YML.getString("EnableAutoBackup"));
                            Bukkit.getConsoleSender().sendMessage(Variable.Lang_YML.getString("EnableAutoBackupButFirstTime"));
                            Variable.check_first_start = false;
                        } else {
                            LocalDateTime now = LocalDateTime.now();
                            String time = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm"));
                            if (Variable.bungee) {
                                File f = null;
                                String OriginalBackup_location = String.valueOf(Variable.custom_autobackup_location) + Variable.file_loc_prefix + time;
                                if (!Main.JavaPlugin.getConfig().getString("CustomBackupLocation").equalsIgnoreCase("")) {
                                    OriginalBackup_location = String.valueOf(Main.JavaPlugin.getConfig().getString("CustomBackupLocation")) + time;
                                }

                                boolean check_has_copy = true;
                                String folderToCompress = "";

                                for (String worldname : MySQL.getAllWorlds()) {
                                    if (MySQL.getServer(worldname).equalsIgnoreCase(Main.JavaPlugin.getConfig().getString("Server"))) {
                                        if (MySQL.getVisitTime(worldname).equalsIgnoreCase("")) {
                                            MySQL.setVisitTime(worldname, String.valueOf(System.currentTimeMillis()));
                                        }

                                        long before_time = Long.valueOf(MySQL.getVisitTime(worldname));
                                        long distance = (System.currentTimeMillis() - before_time) / 86400000L;
                                        if (distance <= Main.JavaPlugin.getConfig().getLong("NoBackup")) {
                                            if (Variable.world_prefix.equalsIgnoreCase("")) {
                                                if (Bukkit.getVersion().toString().toUpperCase().contains("ARCLIGHT")) {
                                                    f = new File(String.valueOf(Variable.single_server_gen) + Variable.world_prefix + worldname);
                                                } else {
                                                    f = new File(String.valueOf(Variable.single_server_gen) + "world" + Variable.file_loc_prefix + worldname);
                                                }
                                            } else {
                                                f = new File(String.valueOf(Variable.single_server_gen) + Variable.world_prefix + worldname);
                                            }

                                            String oldDir = OriginalBackup_location + Variable.file_loc_prefix + worldname;

                                            try {
                                                Util.copyDir(f.getPath(), oldDir);
                                                folderToCompress = String.valueOf(Variable.custom_autobackup_location) + Variable.file_loc_prefix + time;
                                            } catch (Exception e) {
                                                check_has_copy = false;
                                            }
                                        }
                                    }
                                }

                                if (check_has_copy) {
                                    String zipFileName = String.valueOf(OriginalBackup_location) + ".zip";

                                    try {
                                        ZIP.zipFolder(OriginalBackup_location, zipFileName);
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }

                                    Util.deleteFile(new File(OriginalBackup_location));
                                    Bukkit.getConsoleSender().sendMessage(Variable.Lang_YML.getString("BungeeCordModuleAutoBackupSuccess"));
                                }
                            } else {
                                File folder = new File(Variable.Tempf);
                                String OriginalBackup_location = String.valueOf(Variable.custom_autobackup_location) + Variable.file_loc_prefix + time;
                                if (!Main.JavaPlugin.getConfig().getString("CustomBackupLocation").equalsIgnoreCase("")) {
                                    OriginalBackup_location = String.valueOf(Main.JavaPlugin.getConfig().getString("CustomBackupLocation")) + time;
                                }

                                String folderToCompress = null;
                                boolean check_has_copy = true;
                                File[] arrayOfFile;
                                int i = (arrayOfFile = folder.listFiles()).length;

                                for (int b = 0; b < i; b++) {
                                    File temp = arrayOfFile[b];
                                    long lastModified = temp.lastModified();
                                    long nowlong = System.currentTimeMillis();
                                    long distance = (nowlong - lastModified) / 86400000L;
                                    if (distance <= Main.JavaPlugin.getConfig().getLong("NoBackup")) {
                                        String want_to = temp.getPath().replace(Variable.Tempf, "").replace(".yml", "").replace(Variable.file_loc_prefix, "");
                                        if (Bukkit.getWorld(String.valueOf(Variable.world_prefix) + want_to) != null) {
                                            Bukkit.getWorld(String.valueOf(Variable.world_prefix) + want_to).save();
                                        }

                                        String oldDir = OriginalBackup_location + Variable.file_loc_prefix + want_to;
                                        File f = null;
                                        if (Variable.world_prefix.equalsIgnoreCase("")) {
                                            if (Bukkit.getVersion().toString().toUpperCase().contains("ARCLIGHT")) {
                                                f = new File(String.valueOf(Variable.single_server_gen) + Variable.world_prefix + want_to);
                                            } else {
                                                f = new File(String.valueOf(Variable.single_server_gen) + "world" + Variable.file_loc_prefix + want_to);
                                            }
                                        } else {
                                            f = new File(String.valueOf(Variable.single_server_gen) + Variable.world_prefix + want_to);
                                        }

                                        try {
                                            Util.copyDir(f.getPath(), oldDir);
                                            folderToCompress = String.valueOf(Variable.custom_autobackup_location) + Variable.file_loc_prefix + time;
                                        } catch (Exception e) {
                                            check_has_copy = false;
                                        }
                                    }
                                }

                                if (check_has_copy) {
                                    String zipFileName = String.valueOf(OriginalBackup_location) + ".zip";

                                    try {
                                        ZIP.zipFolder(OriginalBackup_location, zipFileName);
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }

                                    Util.deleteFile(new File(OriginalBackup_location));
                                    Bukkit.getConsoleSender().sendMessage(Variable.Lang_YML.getString("SingleServerModuleAutoBackupSuccess"));
                                }
                            }
                        }
                    }
                }).runTaskTimer(Main.JavaPlugin, 0L, Main.JavaPlugin.getConfig().getLong("AutoBackup") * 20L);
            } else {
                Bukkit.getConsoleSender().sendMessage(Variable.Lang_YML.getString("DisableAutoBackup"));
            }
        }

        if (!Main.JavaPlugin.getConfig().getBoolean("DisableFunctionButTeleport") && Main.JavaPlugin.getConfig().getLong("OptimizeTime") != 0L) {
            (new BukkitRunnable() {
                    public void run() {
                        boolean has_been_solve = false;

                        for (World world : Bukkit.getWorlds()) {
                            if (Util.CheckIsHome(world.getName().replace(Variable.world_prefix, ""))) {
                                boolean in_whitelist = false;

                                for (int i = 0; i < Main.JavaPlugin.getConfig().getStringList("UnOptimizeWorlds").size(); i++) {
                                    if (((String)Main.JavaPlugin.getConfig().getStringList("UnOptimizeWorlds").get(i))
                                        .equalsIgnoreCase(world.getName().replace(Variable.world_prefix, ""))) {
                                        in_whitelist = true;
                                        break;
                                    }
                                }

                                if (!in_whitelist) {
                                    if (Main.JavaPlugin.getConfig().getInt("OptimizeType") == 1) {
                                        if (world.getPlayers().size() == 0) {
                                            has_been_solve = true;
                                            Bukkit.unloadWorld(world, true);
                                        }
                                    } else if (Main.JavaPlugin.getConfig().getInt("OptimizeType") == 2) {
                                        Chunk[] arrayOfChunk;
                                        int j = (arrayOfChunk = world.getLoadedChunks()).length;

                                        for (int b = 0; b < j; b++) {
                                            Chunk temp_chunk = arrayOfChunk[b];
                                            boolean check_player = false;
                                            boolean check_cable = false;
                                            BlockState[] arrayOfBlockState;
                                            int k = (arrayOfBlockState = temp_chunk.getTileEntities()).length;

                                            int b1;
                                            for (b1 = 0; b1 < k; b1++) {
                                                BlockState bs = arrayOfBlockState[b1];

                                                try {
                                                    if (Util.getNBTString(bs).toUpperCase().contains("IC2:CABLE")) {
                                                        check_cable = true;
                                                        break;
                                                    }
                                                } catch (NoClassDefFoundError e) {
                                                    check_cable = false;
                                                }
                                            }

                                            Entity[] arrayOfEntity;
                                            for (int ke = (arrayOfEntity = temp_chunk.getEntities()).length; b1 < ke; b1++) {
                                                Entity ee = arrayOfEntity[b1];
                                                if (ee instanceof Player) {
                                                    check_player = true;
                                                    break;
                                                }
                                            }

                                            if (!check_player && !check_cable) {
                                                has_been_solve = true;
                                                temp_chunk.unload(true);
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        if (has_been_solve) {
                            if (Main.JavaPlugin.getConfig().getInt("OptimizeType") == 1) {
                                Bukkit.getConsoleSender().sendMessage(Variable.Lang_YML.getString("OptimizeTypeOne"));
                            } else if (Main.JavaPlugin.getConfig().getInt("OptimizeType") == 2) {
                                Bukkit.getConsoleSender().sendMessage(Variable.Lang_YML.getString("OptimizeTypeTwo"));
                            }
                        }
                    }
                })
                .runTaskTimer(Main.JavaPlugin, 0L, Main.JavaPlugin.getConfig().getLong("OptimizeTime") * 20L);
        }

        if (!Main.JavaPlugin.getConfig().getBoolean("DisableFunctionButTeleport")) {
            if (Main.JavaPlugin.getConfig().getLong("CheckTime") != 0L) {
                Bukkit.getConsoleSender().sendMessage(Variable.Lang_YML.getString("EnableHomeTileCheck"));
                (new BukkitRunnable() {
                    int i = 0;

                    public void run() {
                        List<String> WarnList = new ArrayList<>();
                        List<String> UnLoadList = new ArrayList<>();
                        String WarnStr = "";
                        String UnLoadStr = "";

                        for (StaticsTick st : Variable.world_StaticsTick) {
                            World world = Bukkit.getWorld(String.valueOf(Variable.world_prefix) + st.name);
                            int tiles = st.tile;
                            if (tiles >= Main.JavaPlugin.getConfig().getInt("UnLoadTiles")) {
                                this.i++;
                                UnLoadList.add(world.getName().replace(Variable.world_prefix, ""));
                                if (world.getName() != null) {
                                    for (Player p : world.getPlayers()) {
                                        p.teleport(Bukkit.getWorld("world").getSpawnLocation());
                                        Bukkit.getConsoleSender().sendMessage(Variable.Lang_YML.getString("PlayerBeKickedByBanHome"));
                                    }

                                    Bukkit.unloadWorld(world.getName().replace(Variable.world_prefix, ""), true);
                                }
                            } else if (tiles >= Main.JavaPlugin.getConfig().getInt("MaxTiles")) {
                                this.i++;
                                WarnList.add(world.getName().replace(Variable.world_prefix, ""));
                            }
                        }

                        if (WarnList.size() != 0 || UnLoadList.size() != 0) {
                            if (Main.JavaPlugin.getConfig().getBoolean("CheckTipToAllPlayers")) {
                                Bukkit.broadcastMessage("§a§l§m--------------" + Variable.Prefix + "§a§l§m--------------");
                            } else {
                                Bukkit.getConsoleSender().sendMessage("§a§l§m--------------" + Variable.Prefix + "§a§l§m--------------");
                            }

                            if (Main.JavaPlugin.getConfig().getBoolean("CheckTipToAllPlayers")) {
                                if (WarnList.size() != 0) {
                                    for (int i = 0; i < Variable.Lang_YML.getStringList("WarnLanguage").size(); i++) {
                                        String message = (String)Variable.Lang_YML.getStringList("WarnLanguage").get(i);
                                        if (message.contains("<WarnList>")) {
                                            message = message.replace("<WarnList>", WarnList.toString());
                                        }

                                        Bukkit.broadcastMessage(message);
                                    }
                                }

                                if (UnLoadList.size() != 0) {
                                    for (int i = 0; i < Variable.Lang_YML.getStringList("UnLoadLanguage").size(); i++) {
                                        String message = (String)Variable.Lang_YML.getStringList("UnLoadLanguage").get(i);
                                        if (message.contains("<UnLoadList>")) {
                                            message = message.replace("<UnLoadList>", UnLoadList.toString());
                                        }

                                        Bukkit.broadcastMessage(message);
                                    }
                                }
                            } else {
                                if (WarnList.size() != 0) {
                                    for (int i = 0; i < Variable.Lang_YML.getStringList("WarnLanguage").size(); i++) {
                                        String message = (String)Variable.Lang_YML.getStringList("WarnLanguage").get(i);
                                        if (message.contains("<WarnList>")) {
                                            message = message.replace("<WarnList>", WarnList.toString());
                                        }

                                        Bukkit.getConsoleSender().sendMessage(message);
                                    }
                                }

                                if (UnLoadList.size() != 0) {
                                    for (int i = 0; i < Variable.Lang_YML.getStringList("UnLoadLanguage").size(); i++) {
                                        String message = (String)Variable.Lang_YML.getStringList("UnLoadLanguage").get(i);
                                        if (message.contains("<UnLoadList>")) {
                                            message = message.replace("<UnLoadList>", UnLoadList.toString());
                                        }

                                        Bukkit.getConsoleSender().sendMessage(message);
                                    }
                                }
                            }

                            if (Main.JavaPlugin.getConfig().getBoolean("CheckTipToAllPlayers")) {
                                Bukkit.broadcastMessage("§a§l§m--------------" + Variable.Prefix + "§a§l§m--------------");
                            } else {
                                Bukkit.getConsoleSender().sendMessage("§a§l§m--------------" + Variable.Prefix + "§a§l§m--------------");
                            }
                        }
                    }
                }).runTaskTimer(Main.JavaPlugin, 0L, Main.JavaPlugin.getConfig().getLong("CheckTime") * 20L);
            }

            if (!Main.JavaPlugin.getConfig().getBoolean("DisableFunctionButTeleport")) {
                (new BukkitRunnable() {
                    public void run() {
                        for (World temp : Bukkit.getWorlds()) {
                            String check_world_is_home = temp.getName().replace(Variable.world_prefix, "");
                            if (Util.CheckIsHome(check_world_is_home)) {
                                Integer Amount = 0;
                                Boolean Check = false;
                                Integer Del = 0;

                                for (Entity entity : temp.getEntities()) {
                                    if (entity instanceof LivingEntity) {
                                        boolean check_white = false;

                                        for (int c = 0; c < Main.JavaPlugin.getConfig().getStringList("WhiteEntities").size(); c++) {
                                            String white = (String)Main.JavaPlugin.getConfig().getStringList("WhiteEntities").get(c);
                                            if (white.equalsIgnoreCase(entity.getType().toString())) {
                                                check_white = true;
                                                break;
                                            }
                                        }

                                        if (!check_white) {
                                            Amount = Amount + 1;
                                            if (Amount > Main.JavaPlugin.getConfig().getInt("DeleteEntities") && !(entity instanceof Player)) {
                                                entity.remove();
                                                Check = true;
                                                Del = Del + 1;
                                            }
                                        }
                                    }
                                }

                                if (Check) {
                                    String temp5 = Variable.Lang_YML.getString("ClearEntity");
                                    if (temp5.contains("<Name>")) {
                                        temp5 = temp5.replace("<Name>", temp.getName());
                                    }

                                    if (temp5.contains("<Amount>")) {
                                        temp5 = temp5.replace("<Amount>", String.valueOf(Del));
                                    }

                                    Bukkit.broadcastMessage(temp5);
                                }
                            }
                        }
                    }
                }).runTaskTimer(Main.JavaPlugin, 0L, Main.JavaPlugin.getConfig().getLong("CheckTime") * 20L);
            }

            if (!Main.JavaPlugin.getConfig().getBoolean("DisableFunctionButTeleport")) {
                (new BukkitRunnable() {
                    public void run() {
                        for (World temp : Bukkit.getWorlds()) {
                            String check_world_is_home = temp.getName().replace(Variable.world_prefix, "");
                            if (Util.CheckIsHome(check_world_is_home)) {
                                Integer Amount = 0;
                                Boolean Check = false;
                                Integer Del = 0;

                                for (Entity entity : temp.getEntities()) {
                                    if (entity.getType() == EntityType.ITEM) {
                                        Amount = Amount + 1;
                                        if (Amount > Main.JavaPlugin.getConfig().getInt("DeleteItems")) {
                                            Check = true;
                                            Del = Del + 1;
                                        }
                                    }
                                }

                                if (Check) {
                                    String temp5 = Variable.Lang_YML.getString("ClearDropItems");
                                    if (temp5.contains("<Name>")) {
                                        temp5 = temp5.replace("<Name>", temp.getName());
                                    }

                                    if (temp5.contains("<Amount>")) {
                                        temp5 = temp5.replace("<Amount>", String.valueOf(Del));
                                    }

                                    Bukkit.broadcastMessage(temp5);
                                }
                            }
                        }
                    }
                }).runTaskTimer(Main.JavaPlugin, 0L, Main.JavaPlugin.getConfig().getLong("CheckTime") * 20L);
            } else {
                Bukkit.getConsoleSender().sendMessage(Variable.Lang_YML.getString("DisableHomeTileCheck"));
            }
        }

        if (!Main.JavaPlugin.getConfig().getBoolean("DisableFunctionButTeleport")) {
            (new BukkitRunnable() {
                public void run() {
                    if (!Main.JavaPlugin.getConfig().getBoolean("EnableTimeLock")) {
                        this.cancel();
                    } else {
                        if (Variable.bungee) {
                            for (String worldname : MySQL.getAllWorlds()) {
                                if (Bukkit.getWorld(String.valueOf(Variable.world_prefix) + worldname) != null) {
                                    World world = Bukkit.getWorld(String.valueOf(Variable.world_prefix) + worldname);
                                    if (MySQL.getlocktime(worldname).equalsIgnoreCase("true")) {
                                        world.setTime(Long.valueOf(MySQL.gettime(worldname)));
                                    }
                                }
                            }
                        } else {
                            File folder = new File(Variable.Tempf);
                            if (folder.listFiles() == null) {
                                return;
                            }

                            File[] arrayOfFile;
                            int i = (arrayOfFile = folder.listFiles()).length;

                            for (int b = 0; b < i; b++) {
                                File temp = arrayOfFile[b];
                                String want_to = temp.getPath().replace(Variable.Tempf, "").replace(".yml", "").replace(Variable.file_loc_prefix, "");
                                if (Bukkit.getWorld(String.valueOf(Variable.world_prefix) + want_to) != null) {
                                    World world = Bukkit.getWorld(String.valueOf(Variable.world_prefix) + want_to);
                                    YamlConfiguration yamlConfiguration = YamlConfiguration.loadConfiguration(temp);
                                    if (yamlConfiguration.getBoolean("locktime")) {
                                        world.setTime(yamlConfiguration.getLong("time"));
                                    }
                                }
                            }
                        }
                    }
                }
            }).runTaskTimer(Main.JavaPlugin, 0L, 60L);
        }

        if (!Main.JavaPlugin.getConfig().getBoolean("DisableFunctionButTeleport")) {
            (new BukkitRunnable() {
                public void run() {
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        if (Variable.DispathCommand.contains(p.getName())) {
                            String temp5 = Variable.Lang_YML.getString("OverSomeBorderTip");
                            if (!temp5.equalsIgnoreCase("")) {
                                p.sendMessage("§a§l§m--------------" + Variable.Prefix + "§a§l§m--------------");
                                p.sendMessage(temp5);
                                p.sendMessage("§a§l§m--------------" + Variable.Prefix + "§a§l§m--------------");
                                Bukkit.dispatchCommand(p, Main.JavaPlugin.getConfig().getString("BorderCommand"));
                                Variable.DispathCommand.remove(p.getName());
                            }
                        }
                    }
                }
            }).runTaskTimer(Main.JavaPlugin, 0L, 20L);
        }

        if (!Main.JavaPlugin.getConfig().getBoolean("DisableFunctionButTeleport")) {
            (new BukkitRunnable() {
                public void run() {
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        if (Variable.AddDebuff.contains(p.getName())) {
                            p.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 100, 10));
                            String temp5 = Variable.Lang_YML.getString("OverBorderTip");
                            p.sendMessage("§a§l§m--------------" + Variable.Prefix + "§a§l§m--------------");
                            p.sendMessage(temp5);
                            p.sendMessage("§a§l§m--------------" + Variable.Prefix + "§a§l§m--------------");
                            Variable.AddDebuff.remove(p.getName());
                        }
                    }
                }
            }).runTaskTimer(Main.JavaPlugin, 0L, 20L);
        }

        if (!Main.JavaPlugin.getConfig().getBoolean("DisableFunctionButTeleport")) {
            (new BukkitRunnable() {
                public void run() {
                    init.OPS_redis.clear();
                    init.MEMBERS_redis.clear();

                    for (World world : Bukkit.getWorlds()) {
                        Home temp_home = HomeAPI.getHome(world.getName());
                        if (temp_home != null) {
                            init.OPS_redis.put(world.getName().replace(Variable.world_prefix, ""), temp_home.getOPs());
                            init.MEMBERS_redis.put(world.getName().replace(Variable.world_prefix, ""), temp_home.getMembers());
                        }
                    }
                }
            }).runTaskTimer(Main.JavaPlugin, 0L, 60L);
        }

        if (!Main.JavaPlugin.getConfig().getBoolean("DisableFunctionButTeleport")) {
            if (Main.JavaPlugin.getConfig().getBoolean("CustomTileMax")) {
                Bukkit.getConsoleSender().sendMessage(Variable.Lang_YML.getString("EnableCustomTileMaxFunction"));
            } else {
                Bukkit.getConsoleSender().sendMessage(Variable.Lang_YML.getString("DisableCustomTileMaxFunction"));
            }

            if (Main.JavaPlugin.getConfig().getBoolean("EnableBlackEntities")) {
                Bukkit.getConsoleSender().sendMessage(Variable.Lang_YML.getString("EnableBlackEntitiesFunction"));
            } else {
                Bukkit.getConsoleSender().sendMessage(Variable.Lang_YML.getString("DisableBlackEntitiesFunction"));
            }

            if (!Main.JavaPlugin.getConfig().getString("CustomBorder").equalsIgnoreCase("")) {
                Bukkit.getConsoleSender().sendMessage(Variable.Lang_YML.getString("EnableCustomBorder"));
            }

            if (!Main.JavaPlugin.getConfig().getBoolean("KeepInventory")) {
                Bukkit.getConsoleSender().sendMessage(Variable.Lang_YML.getString("DisableWholeKeepInventory"));
            } else if (Main.JavaPlugin.getConfig().getBoolean("KeepInventory")) {
                Bukkit.getConsoleSender().sendMessage(Variable.Lang_YML.getString("EnableWholeKeepInventory"));
            }

            if (!Main.JavaPlugin.getConfig().getBoolean("doMobSpawning")) {
                Bukkit.getConsoleSender().sendMessage(Variable.Lang_YML.getString("DisableMobSpawning"));
            } else if (Main.JavaPlugin.getConfig().getBoolean("doMobSpawning")) {
                Bukkit.getConsoleSender().sendMessage(Variable.Lang_YML.getString("EnabledoMobSpawning"));
            }

            if (!Main.JavaPlugin.getConfig().getBoolean("mobGriefing")) {
                Bukkit.getConsoleSender().sendMessage(Variable.Lang_YML.getString("DisablemobGriefing"));
            } else if (Main.JavaPlugin.getConfig().getBoolean("mobGriefing")) {
                Bukkit.getConsoleSender().sendMessage(Variable.Lang_YML.getString("EnablemobGriefing"));
            }

            if (!Main.JavaPlugin.getConfig().getBoolean("doFireTick")) {
                Bukkit.getConsoleSender().sendMessage(Variable.Lang_YML.getString("DisabledoFireTick"));
            } else if (Main.JavaPlugin.getConfig().getBoolean("doFireTick")) {
                Bukkit.getConsoleSender().sendMessage(Variable.Lang_YML.getString("EnabledoFireTick"));
            }
        }
    }
}
