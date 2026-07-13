package com.Util;

import com.SelfHome.Main;
import com.SelfHome.Redis;
import com.SelfHome.Variable;
import de.tr7zw.nbtapi.NBTItem;
import de.tr7zw.nbtapi.NBTTileEntity;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

public class Util {
    public static void clearCache(final String p, final String papi_name) {
        (new BukkitRunnable() {
            public void run() {
                List<Redis> list = Variable.cache;

                for (int i = 0; i < list.size(); i++) {
                    Redis redis = list.get(i);
                    if (redis != null && redis.name.equalsIgnoreCase(p) && redis.papi_name.equalsIgnoreCase(papi_name)) {
                        list.remove(redis);
                    }
                }

                Variable.cache = list;
            }
        }).runTaskLater(Main.JavaPlugin, Variable.Lang_YML.getLong("PlaceHolders.RefreshTime") * 20L);
    }

    public static Location getAir(Location loc) {
        Location clo = loc.clone();

        double i;
        for (i = 255.0; i >= 0.0; i--) {
            Location temp = clo.clone();
            temp.setY(i + 20.0);
            if (loc.getWorld().getBlockAt(temp).getType() != Material.AIR) {
                break;
            }
        }

        if (i != 0.0) {
            return clo;
        }

        loc.setY(i);
        return loc;
    }

    public static Location getNotAir(Location loc) {
        return loc;
    }

    public static List<Chunk> getchunkmap(Location loc1, Location loc2, Location loc3, Location loc4) {
        List<Chunk> chunkmap = new ArrayList<>();
        double Ax = loc1.getX();
        double Az = loc1.getZ();
        double Bx = loc2.getX();
        double Bz = loc2.getZ();
        double Dx = loc3.getX();
        double Dz = loc3.getZ();
        double Ex = loc4.getX();
        double Ez = loc4.getZ();
        double minX = Math.min(Ax, Math.min(Bx, Math.min(Dx, Ex)));
        double maxX = Math.max(Ax, Math.max(Bx, Math.max(Dx, Ex)));
        double minZ = Math.min(Az, Math.min(Bz, Math.min(Dz, Ez)));
        double maxZ = Math.max(Az, Math.max(Bz, Math.max(Dz, Ez)));

        for (double x = minX; x <= maxX; x += 16.0) {
            for (double z = minZ; z <= maxZ; z += 16.0) {
                Location temp = loc1.getWorld().getSpawnLocation();
                temp.setX(x);
                temp.setZ(z);
                Chunk chunk = loc1.getWorld().getBlockAt(temp).getChunk();
                chunkmap.add(chunk);
            }
        }

        return chunkmap;
    }

    public static String getNBTString(BlockState state) {
        NBTTileEntity tent = new NBTTileEntity(state);
        String name = "";

        try {
            name = "id:" + state.getType().toString().toUpperCase() + ":" + state.getData() + ",nbt:" + tent.asNBTString().toUpperCase();
        } catch (Exception e) {
            name = String.valueOf(state.getType().toString().toUpperCase()) + ":" + state.getData();
        }

        return name;
    }

    public static String getItemNBTString(ItemStack i) {
        if (i == null) {
            return "AIR";
        }

        if (i.getType() == Material.AIR) {
            return "AIR";
        }

        NBTItem nbti = new NBTItem(i);
        String name = "";

        try {
            name = "id:" + i.getType().toString().toUpperCase() + ":" + i.getDurability() + ",nbt:" + nbti.asNBTString().toUpperCase();
        } catch (Exception e) {
            name = String.valueOf(i.getType().toString().toUpperCase()) + ":" + i.getDurability();
        }

        return name;
    }

    public static String getAliasName(String name) {
        String result = null;
        if (Variable.Lang_YML.getStringList("PlaceHolders.OtherWorldAlias") == null) {
            result = name;
        } else {
            for (int e = 0; e < Variable.Lang_YML.getStringList("PlaceHolders.OtherWorldAlias").size(); e++) {
                String[] temp = ((String)Variable.Lang_YML.getStringList("PlaceHolders.OtherWorldAlias").get(e)).split(",");
                if (temp[0].equalsIgnoreCase(name)) {
                    result = temp[1];
                }
            }
        }

        return result;
    }

    public static boolean CheckIsHome(String name) {
        return Variable.list_home.contains(name.replace(Variable.world_prefix, ""));
    }

    public static void deleteFile(File file) {
        if (file.exists()) {
            try {
                if (file.isDirectory()) {
                    File[] files = file.listFiles();
                    if (files.length > 0) {
                        File[] arrayOfFile = files;
                        int i = files.length;

                        for (int b = 0; b < i; b++) {
                            File aFile = arrayOfFile[b];
                            deleteFile(aFile);
                        }
                    }
                }

                file.delete();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void copyDir(String oldDir, String newDir) {
        File srcDir = new File(oldDir);
        if (srcDir.exists() && srcDir.isDirectory()) {
            File destDir = new File(newDir);
            if (!destDir.exists() && destDir.mkdirs()) {
                File[] files = srcDir.listFiles();
                File[] arrayOfFile = files;
                int i = files.length;

                for (int b = 0; b < i; b++) {
                    File f = arrayOfFile[b];
                    if (f.isFile()) {
                        copyFile(f, new File(newDir, f.getName()));
                    } else if (f.isDirectory()) {
                        copyDir(String.valueOf(oldDir) + File.separator + f.getName(), String.valueOf(newDir) + File.separator + f.getName());
                    }
                }
            }
        }
    }

    public static void copyFile(File oldDir, File newDir) {
        BufferedInputStream bufferedInputStream = null;
        BufferedOutputStream bufferedOutputStream = null;
        byte[] b = new byte[1024];

        try {
            bufferedInputStream = new BufferedInputStream(new FileInputStream(oldDir));
            bufferedOutputStream = new BufferedOutputStream(new FileOutputStream(newDir));

            int len;
            while ((len = bufferedInputStream.read(b)) > -1) {
                bufferedOutputStream.write(b, 0, len);
            }

            bufferedOutputStream.flush();
        } catch (IOException var19) {
        } finally {
            if (bufferedInputStream != null) {
                try {
                    bufferedInputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            if (bufferedOutputStream != null) {
                try {
                    bufferedOutputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static Boolean CheckOwnerAndManagerAndOP(Player p, String name) {
        boolean return_boolean = false;
        if (p.getName().equalsIgnoreCase(name)) {
            return_boolean = true;
        }

        if (p.isOp()) {
            return_boolean = true;
        }

        if (Variable.bungee) {
            List<String> ops = MySQL.getOP(name);

            for (int e = 0; e < ops.size(); e++) {
                if (ops.get(e).equalsIgnoreCase(p.getName())) {
                    return_boolean = true;
                    break;
                }
            }
        } else {
            File f = new File(Variable.Tempf, String.valueOf(name) + ".yml");
            if (f.exists()) {
                YamlConfiguration yml = YamlConfiguration.loadConfiguration(f);
                List<String> OP = yml.getStringList("OP");
                Boolean CheckOP = false;
                if (OP == null) {
                    OP = new ArrayList<>();
                }

                for (int i = 0; i < OP.size(); i++) {
                    if (OP.get(i).equalsIgnoreCase(p.getName())) {
                        CheckOP = true;
                    }
                }

                if (CheckOP) {
                    return_boolean = true;
                }
            } else {
                return_boolean = false;
            }
        }

        return return_boolean;
    }

    public static Boolean Check(Player p, String name) {
        name = name.replace(Variable.world_prefix, "");
        boolean result = false;
        File f = new File(Variable.Tempf, String.valueOf(name) + ".yml");
        if (CheckOwnerAndManagerAndOP(p, name)) {
            return true;
        }

        if (Variable.bungee) {
            List<String> ops = MySQL.getMembers(name);

            for (int e = 0; e < ops.size(); e++) {
                if (ops.get(e).equalsIgnoreCase(p.getName()) || ops.get(e).equals("*")) {
                    result = true;
                    break;
                }
            }
        } else if (f.exists()) {
            YamlConfiguration yamlConfiguration = YamlConfiguration.loadConfiguration(f);
            List<String> Members = yamlConfiguration.getStringList("Members");
            Boolean CheckMembers = false;
            if (Members == null) {
                Members = new ArrayList<>();
            }

            for (int i = 0; i < Members.size(); i++) {
                if (Members.get(i).equalsIgnoreCase(p.getName()) || Members.get(i).equals("*")) {
                    result = true;
                    break;
                }
            }
        } else {
            result = false;
        }

        return result;
    }

    public static Boolean CheckBlack(Player p, String name) {
        boolean check = false;
        if (Variable.bungee) {
            List<String> ops = MySQL.getDenys(name);

            for (int e = 0; e < ops.size(); e++) {
                if (ops.get(e).equalsIgnoreCase(p.getName())) {
                    check = true;
                    break;
                }
            }
        } else {
            File f = new File(Variable.Tempf, String.valueOf(name) + ".yml");
            if (f.exists()) {
                YamlConfiguration yamlConfiguration = YamlConfiguration.loadConfiguration(f);
                List<String> Members = yamlConfiguration.getStringList("Denys");
                Boolean CheckMembers = false;
                if (Members == null) {
                    Members = new ArrayList<>();
                }

                for (int i = 0; i < Members.size(); i++) {
                    if (Members.get(i).equalsIgnoreCase(p.getName())) {
                        check = true;
                        break;
                    }
                }
            } else {
                check = false;
            }
        }

        return check;
    }

    public static Boolean CheckIllegalName(Player p) {
        String name = p.getName();
        if (!name.contains("|")
            && !name.contains("&")
            && !name.contains("!")
            && !name.contains("@")
            && !name.contains("^")
            && !name.contains("*")
            && !name.toUpperCase().contains("DIM")) {
            for (int i = 0; i < Main.JavaPlugin.getConfig().getStringList("IlleagalName").size(); i++) {
                String temp = (String)Main.JavaPlugin.getConfig().getStringList("IlleagalName").get(i);
                if (p.getName().equalsIgnoreCase(temp)) {
                    return true;
                }
            }

            return false;
        } else {
            return true;
        }
    }

    public static void refreshBorder(final World world) {
        (new BukkitRunnable() {
            public void run() {
                if (Util.CheckIsHome(world.getName().replace(Variable.world_prefix, ""))) {
                        // HolographicDisplays disabled for Paper 1.21
                        return;

                }
            }
        }).runTask(Main.JavaPlugin);
    }
}
