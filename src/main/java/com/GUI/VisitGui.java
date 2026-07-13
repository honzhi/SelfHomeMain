package com.GUI;

import com.SelfHome.Main;
import com.SelfHome.Variable;
import com.Util.Home;
import com.Util.HomeAPI;
import com.Util.MySQL;
import com.Util.Util;
import com.Util.VisitStatistic;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.SkullType;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.scheduler.BukkitRunnable;

public class VisitGui implements InventoryHolder {
    public Inventory MainGui = Bukkit.createInventory(this, 54, Variable.GUI_YML.getString("VisitTitle"));
    public int MaxPage = 0;
    public int NowPage = 0;
    public ArrayList<Home> players = new ArrayList<>();
    int item_add_amount = 0;

    public VisitGui() {
        (new BukkitRunnable() {
                public void run() {
                    ConfigurationSection dd = Variable.GUI_YML.getConfigurationSection("");

                    for (String temp : dd.getKeys(false)) {
                        if (Variable.GUI_YML.getString(String.valueOf(temp) + ".InMenu") != null
                            && Variable.GUI_YML.getString(String.valueOf(temp) + ".InMenu").equalsIgnoreCase("Visit")) {
                            VisitGui.this.item_add_amount++;
                        }
                    }

                    VisitGui.this.MaxPage = 0;
                    VisitGui.this.NowPage = 0;
                    VisitGui.this.players.clear();
                    boolean next_page = false;
                    int amount = 0;
                    if (Main.JavaPlugin.getConfig().getBoolean("BungeeCord")) {
                        if (Main.JavaPlugin.getConfig().getString("VisitGuiShowAll").equalsIgnoreCase("Public")) {
                            for (String world : MySQL.getAllWorlds()) {
                                Home home = HomeAPI.getHome(world);
                                if (home != null && home.isAllowStranger()) {
                                    VisitGui.this.players.add(home);
                                }
                            }
                        } else if (Main.JavaPlugin.getConfig().getString("VisitGuiShowAll").equalsIgnoreCase("ALL")) {
                            for (String world : MySQL.getAllWorlds()) {
                                Home home = HomeAPI.getHome(world);
                                if (home != null) {
                                    VisitGui.this.players.add(home);
                                }
                            }
                        } else {
                            for (World world : Bukkit.getWorlds()) {
                                if (Util.CheckIsHome(world.getName())) {
                                    Home home = HomeAPI.getHome(world.getName());
                                    VisitGui.this.players.add(home);
                                }
                            }
                        }

                        VisitGui.this.MaxPage = (int)Math.ceil(Math.ceil(VisitGui.this.players.size()) / (29 - VisitGui.this.item_add_amount) * 1.0);
                    } else {
                        File folder = new File(Variable.Tempf);
                        File[] arrayOfFile;
                        int k = (arrayOfFile = folder.listFiles()).length;

                        for (int b = 0; b < k; b++) {
                            File temp = arrayOfFile[b];
                            String want_to = temp.getPath().replace(Variable.Tempf, "").replace(".yml", "").replace(Variable.file_loc_prefix, "");
                            YamlConfiguration yamlConfiguration = YamlConfiguration.loadConfiguration(temp);
                            if (Main.JavaPlugin.getConfig().getString("VisitGuiShowAll").equalsIgnoreCase("Public")) {
                                if (HomeAPI.getHome(want_to.replace(Variable.world_prefix, "")).isAllowStranger()) {
                                    VisitGui.this.players.add(HomeAPI.getHome(want_to.replace(Variable.world_prefix, "")));
                                }
                            } else if (Main.JavaPlugin.getConfig().getString("VisitGuiShowAll").equalsIgnoreCase("ALL")) {
                                VisitGui.this.players.add(HomeAPI.getHome(want_to.replace(Variable.world_prefix, "")));
                            } else if (Bukkit.getWorld(Variable.world_prefix + want_to) != null) {
                                Home home = HomeAPI.getHome(want_to);
                                VisitGui.this.players.add(home);
                            }
                        }

                        VisitGui.this.MaxPage = (int)Math.ceil(Math.ceil(VisitGui.this.players.size()) / (29 - VisitGui.this.item_add_amount));
                    }

                    List<VisitStatistic> vst_list = new ArrayList<>();

                    for (Home home : VisitGui.this.players) {
                        double value = home.getFlowers() * Main.JavaPlugin.getConfig().getDouble("FlowerAdd")
                            + home.getPopularity() * Main.JavaPlugin.getConfig().getDouble("PopularityAdd");
                        VisitStatistic vst = new VisitStatistic(home, value);
                        vst_list.add(vst);
                    }

                    for (int i = 0; i < vst_list.size() - 1; i++) {
                        for (int j = 0; j < vst_list.size() - 1 - i; j++) {
                            if (vst_list.get(j).value < vst_list.get(j + 1).value) {
                                VisitStatistic temp = vst_list.get(j);
                                vst_list.set(j, vst_list.get(j + 1));
                                vst_list.set(j + 1, temp);
                            }
                        }
                    }

                    VisitGui.this.players.clear();

                    for (int c = 0; c < vst_list.size(); c++) {
                        VisitGui.this.players.add(vst_list.get(c).home);
                    }

                    VisitGui.this.MainGui.clear();
                    if (Variable.GUI_YML.getBoolean("EnableVisitGuiNormalPane")) {
                        try {
                            new ItemStack(Material.valueOf(Variable.GUI_YML.getString("PaneMaterial")));
                        } catch (Exception e) {
                            String temp5 = Variable.Lang_YML.getString("GlassPaneNotFound");
                            if (temp5.contains("<Material>")) {
                                temp5 = temp5.replace("<Material>", Variable.GUI_YML.getString("PaneMaterial"));
                            }

                            Bukkit.getConsoleSender().sendMessage(temp5);
                            return;
                        }

                        ItemStack blb1 = new ItemStack(Material.valueOf(Variable.GUI_YML.getString("PaneMaterial")));
                        blb1.setDurability((short)15);
                        ItemMeta i1 = blb1.getItemMeta();
                        i1.setDisplayName("");
                        blb1.setItemMeta(i1);

                        for (int i = 0; i < 9; i++) {
                            VisitGui.this.MainGui.setItem(i, blb1);
                        }

                        VisitGui.this.MainGui.setItem(9, blb1);
                        VisitGui.this.MainGui.setItem(18, blb1);
                        VisitGui.this.MainGui.setItem(27, blb1);
                        VisitGui.this.MainGui.setItem(17, blb1);
                        VisitGui.this.MainGui.setItem(26, blb1);
                        VisitGui.this.MainGui.setItem(35, blb1);
                        VisitGui.this.MainGui.setItem(36, blb1);
                        VisitGui.this.MainGui.setItem(44, blb1);

                        for (int i = 45; i < 54; i++) {
                            if (i != 49) {
                                VisitGui.this.MainGui.setItem(i, blb1);
                            }
                        }
                    }

                    ItemStack next = new ItemStack(Material.valueOf(Variable.GUI_YML.getString("NextMaterial")));
                    ItemMeta next_meta = next.getItemMeta();
                    next_meta.setDisplayName(Variable.GUI_YML.getString("Next"));
                    next.setItemMeta(next_meta);
                    VisitGui.this.MainGui.setItem(53, next);
                    ItemStack prev = new ItemStack(Material.valueOf(Variable.GUI_YML.getString("PrevMaterial")));
                    ItemMeta prev_meta = next.getItemMeta();
                    prev_meta.setDisplayName(Variable.GUI_YML.getString("Prev"));
                    prev.setItemMeta(prev_meta);
                    VisitGui.this.MainGui.setItem(45, prev);
                    ConfigurationSection cs = Variable.GUI_YML.getConfigurationSection("");

                    for (String temp : cs.getKeys(false)) {
                        if (Variable.GUI_YML.getString(String.valueOf(temp) + ".InMenu") != null
                            && Variable.GUI_YML.getString(String.valueOf(temp) + ".InMenu").equalsIgnoreCase("Visit")) {
                            try {
                                new ItemStack(Material.valueOf(Variable.GUI_YML.getString(String.valueOf(temp) + ".Material")));
                            } catch (Exception e) {
                                String temp5 = Variable.Lang_YML.getString("MaterialNotFound");
                                if (temp5.contains("<Material>")) {
                                    temp5 = temp5.replace("<Material>", Variable.GUI_YML.getString(String.valueOf(temp) + ".Material"));
                                }

                                if (temp5.contains("<ID>")) {
                                    temp5 = temp5.replace("<ID>", temp);
                                }

                                Bukkit.getConsoleSender().sendMessage(temp5);
                                return;
                            }

                            ItemStack item = new ItemStack(Material.valueOf(Variable.GUI_YML.getString(String.valueOf(temp) + ".Material")));
                            if (Variable.GUI_YML.getInt(String.valueOf(temp) + ".SubID") != 0) {
                                item.setDurability((short)Variable.GUI_YML.getInt(String.valueOf(temp) + ".SubID"));
                            }

                            ItemMeta meta = item.getItemMeta();
                            List<String> lores = new ArrayList<>();
                            meta.setDisplayName(Variable.GUI_YML.getString(String.valueOf(temp) + ".CustomName"));

                            for (int i = 0; i < Variable.GUI_YML.getStringList(String.valueOf(temp) + ".Lores").size(); i++) {
                                String tempstr = (String)Variable.GUI_YML.getStringList(String.valueOf(temp) + ".Lores").get(i);

                                try {
                                    tempstr = PlaceholderAPI.setPlaceholders(null, tempstr);
                                } catch (Exception var19) {
                                }

                                lores.add(tempstr);
                            }

                            for (int c = 0; c < Variable.GUI_YML.getStringList(String.valueOf(temp) + ".Enchants").size(); c++) {
                                String[] tempenc = ((String)Variable.GUI_YML.getStringList(String.valueOf(temp) + ".Enchants").get(c)).split("\\,");
                                meta.addEnchant(Enchantment.getByName(tempenc[0]), Integer.valueOf(tempenc[1]), true);
                            }

                            meta.setLore(lores);
                            item.setItemMeta(meta);
                            VisitGui.this.MainGui.setItem(Variable.GUI_YML.getInt(String.valueOf(temp) + ".Index") - 1, item);
                        }
                    }

                    boolean var25 = false;

                    for (int c = VisitGui.this.NowPage * (29 - VisitGui.this.item_add_amount);
                        c < VisitGui.this.players.size() && c < (VisitGui.this.NowPage + 1) * (29 - VisitGui.this.item_add_amount) && c >= 0;
                        c++
                    ) {
                        if (Util.CheckIsHome(VisitGui.this.players.get(c).getName().replace(Variable.world_prefix, ""))) {
                            if (!Variable.GUI_YML.getString("HeadMaterial").toUpperCase().contains("HEAD")
                                && !Variable.GUI_YML.getString("HeadMaterial").toUpperCase().contains("SKULL")) {
                                ItemStack item = new ItemStack(Material.valueOf(Variable.GUI_YML.getString("HeadMaterial")));
                                ItemMeta i_meta = item.getItemMeta();
                                i_meta.setDisplayName(
                                    String.valueOf(Variable.Lang_YML.getString("VisitGuiHomePrefix"))
                                        + VisitGui.this.players.get(c).getName().replace(Variable.world_prefix, "")
                                        + Variable.Lang_YML.getString("VisitGuiHomeSuffix")
                                );
                                Home home = VisitGui.this.players.get(c);
                                List<String> lores = new ArrayList<>();

                                for (int i = 0; i < Variable.GUI_YML.getStringList("VisitGuiLores").size() - 1; i++) {
                                    String temp = ((String)Variable.GUI_YML.getStringList("VisitGuiLores").get(i))
                                        .replace("<Name>", VisitGui.this.players.get(c).getName().replace(Variable.world_prefix, ""));
                                    temp = PlaceholderAPI.setPlaceholders(null, temp);
                                    lores.add(temp);
                                }

                                if (Variable.GUI_YML.getBoolean("VisitSlogan")) {
                                    for (String str : home.getAdvertisement()) {
                                        lores.add(str);
                                    }
                                }

                                String temp = ((String)Variable.GUI_YML
                                        .getStringList("VisitGuiLores")
                                        .get(Variable.GUI_YML.getStringList("VisitGuiLores").size() - 1))
                                    .replace("<Name>", VisitGui.this.players.get(c).getName().replace(Variable.world_prefix, ""));
                                temp = PlaceholderAPI.setPlaceholders(null, temp);
                                lores.add(temp);
                                i_meta.setLore(lores);
                                item.setItemMeta(i_meta);
                                if (!home.getIcon().equalsIgnoreCase("")) {
                                    String[] item_info = home.getIcon().split(":");
                                    item.setType(Material.valueOf(item_info[0]));
                                    item.setDurability(Short.valueOf(item_info[1]));
                                }

                                item.setAmount(1);
                                VisitGui.this.MainGui.addItem(new ItemStack[]{item});
                            } else {
                                try {
                                    new ItemStack(Material.valueOf(Variable.GUI_YML.getString("HeadMaterial")), 1, (short)SkullType.PLAYER.ordinal());
                                } catch (Exception e) {
                                    String temp5 = Variable.Lang_YML.getString("PlayerHeadMaterialNotFound");
                                    if (temp5.contains("<Material>")) {
                                        temp5 = temp5.replace("<Material>", Variable.GUI_YML.getString("HeadMaterial"));
                                    }

                                    Bukkit.getConsoleSender().sendMessage(temp5);
                                    return;
                                }

                                ItemStack skull = new ItemStack(
                                    Material.valueOf(Variable.GUI_YML.getString("HeadMaterial")), 1, (short)SkullType.PLAYER.ordinal()
                                );
                                SkullMeta player_SKULL = (SkullMeta)skull.getItemMeta();
                                Player temp_p = Bukkit.getPlayer(VisitGui.this.players.get(c).getName().replace(Variable.world_prefix, ""));
                                if (Variable.GUI_YML.getBoolean("EnableSkullSkin") && temp_p != null) {
                                    try {
                                        player_SKULL.setOwningPlayer(temp_p);
                                    } catch (Exception var18) {
                                    }
                                }

                                player_SKULL.setDisplayName(
                                    String.valueOf(Variable.Lang_YML.getString("VisitGuiHomePrefix"))
                                        + VisitGui.this.players.get(c).getName().replace(Variable.world_prefix, "")
                                        + Variable.Lang_YML.getString("VisitGuiHomeSuffix")
                                );
                                Home home = VisitGui.this.players.get(c);
                                List<String> lores = new ArrayList<>();

                                for (int i = 0; i < Variable.GUI_YML.getStringList("VisitGuiLores").size() - 1; i++) {
                                    String temp = ((String)Variable.GUI_YML.getStringList("VisitGuiLores").get(i))
                                        .replace("<Name>", VisitGui.this.players.get(c).getName().replace(Variable.world_prefix, ""));
                                    temp = PlaceholderAPI.setPlaceholders(null, temp);
                                    lores.add(temp);
                                }

                                if (Variable.GUI_YML.getBoolean("VisitSlogan")) {
                                    for (String str : home.getAdvertisement()) {
                                        lores.add(str);
                                    }
                                }

                                String temp = ((String)Variable.GUI_YML
                                        .getStringList("VisitGuiLores")
                                        .get(Variable.GUI_YML.getStringList("VisitGuiLores").size() - 1))
                                    .replace("<Name>", VisitGui.this.players.get(c).getName().replace(Variable.world_prefix, ""));
                                temp = PlaceholderAPI.setPlaceholders(null, temp);
                                lores.add(temp);
                                player_SKULL.setLore(lores);
                                skull.setItemMeta(player_SKULL);
                                if (!home.getIcon().equalsIgnoreCase("")) {
                                    String[] item_info = home.getIcon().split(":");
                                    skull.setType(Material.valueOf(item_info[0]));
                                    skull.setDurability(Short.valueOf(item_info[1]));
                                }

                                skull.setAmount(1);
                                VisitGui.this.MainGui.addItem(new ItemStack[]{skull});
                            }
                        }
                    }
                }
            })
            .runTaskAsynchronously(Main.JavaPlugin);
    }

    public void OpenNextInventory(final Player p) {
        (new BukkitRunnable() {
                public void run() {
                    if (VisitGui.this.NowPage + 2 <= VisitGui.this.MaxPage) {
                        VisitGui.this.NowPage++;
                        VisitGui.this.MainGui.clear();
                        if (Variable.GUI_YML.getBoolean("EnableVisitGuiNormalPane")) {
                            try {
                                new ItemStack(Material.valueOf(Variable.GUI_YML.getString("PaneMaterial")));
                            } catch (Exception e) {
                                String temp5 = Variable.Lang_YML.getString("GlassPaneNotFound");
                                if (temp5.contains("<Material>")) {
                                    temp5 = temp5.replace("<Material>", Variable.GUI_YML.getString("PaneMaterial"));
                                }

                                Bukkit.getConsoleSender().sendMessage(temp5);
                                return;
                            }

                            ItemStack blb1 = new ItemStack(Material.valueOf(Variable.GUI_YML.getString("PaneMaterial")));
                            blb1.setDurability((short)15);
                            ItemMeta i1 = blb1.getItemMeta();
                            i1.setDisplayName("");
                            blb1.setItemMeta(i1);

                            for (int i = 0; i < 9; i++) {
                                VisitGui.this.MainGui.setItem(i, blb1);
                            }

                            VisitGui.this.MainGui.setItem(9, blb1);
                            VisitGui.this.MainGui.setItem(18, blb1);
                            VisitGui.this.MainGui.setItem(27, blb1);
                            VisitGui.this.MainGui.setItem(17, blb1);
                            VisitGui.this.MainGui.setItem(26, blb1);
                            VisitGui.this.MainGui.setItem(35, blb1);
                            VisitGui.this.MainGui.setItem(36, blb1);
                            VisitGui.this.MainGui.setItem(44, blb1);

                            for (int i = 45; i < 54; i++) {
                                if (i != 49) {
                                    VisitGui.this.MainGui.setItem(i, blb1);
                                }
                            }
                        }

                        ItemStack next = new ItemStack(Material.valueOf(Variable.GUI_YML.getString("NextMaterial")));
                        ItemMeta next_meta = next.getItemMeta();
                        next_meta.setDisplayName(Variable.GUI_YML.getString("Next"));
                        next.setItemMeta(next_meta);
                        VisitGui.this.MainGui.setItem(53, next);
                        ItemStack prev = new ItemStack(Material.valueOf(Variable.GUI_YML.getString("PrevMaterial")));
                        ItemMeta prev_meta = next.getItemMeta();
                        prev_meta.setDisplayName(Variable.GUI_YML.getString("Prev"));
                        prev.setItemMeta(prev_meta);
                        VisitGui.this.MainGui.setItem(45, prev);
                        ConfigurationSection cs = Variable.GUI_YML.getConfigurationSection("");

                        for (String temp : cs.getKeys(false)) {
                            if (Variable.GUI_YML.getString(String.valueOf(temp) + ".InMenu") != null
                                && Variable.GUI_YML.getString(String.valueOf(temp) + ".InMenu").equalsIgnoreCase("Visit")) {
                                try {
                                    new ItemStack(Material.valueOf(Variable.GUI_YML.getString(String.valueOf(temp) + ".Material")));
                                } catch (Exception e) {
                                    String temp5 = Variable.Lang_YML.getString("MaterialNotFound");
                                    if (temp5.contains("<Material>")) {
                                        temp5 = temp5.replace("<Material>", Variable.GUI_YML.getString(String.valueOf(temp) + ".Material"));
                                    }

                                    if (temp5.contains("<ID>")) {
                                        temp5 = temp5.replace("<ID>", temp);
                                    }

                                    Bukkit.getConsoleSender().sendMessage(temp5);
                                    return;
                                }

                                ItemStack item = new ItemStack(Material.valueOf(Variable.GUI_YML.getString(String.valueOf(temp) + ".Material")));
                                if (Variable.GUI_YML.getInt(String.valueOf(temp) + ".SubID") != 0) {
                                    item.setDurability((short)Variable.GUI_YML.getInt(String.valueOf(temp) + ".SubID"));
                                }

                                ItemMeta meta = item.getItemMeta();
                                List<String> lores = new ArrayList<>();
                                meta.setDisplayName(Variable.GUI_YML.getString(String.valueOf(temp) + ".CustomName"));

                                for (int i = 0; i < Variable.GUI_YML.getStringList(String.valueOf(temp) + ".Lores").size(); i++) {
                                    String tempstr = (String)Variable.GUI_YML.getStringList(String.valueOf(temp) + ".Lores").get(i);

                                    try {
                                        tempstr = PlaceholderAPI.setPlaceholders(p, tempstr);
                                    } catch (Exception var16) {
                                    }

                                    lores.add(tempstr);
                                }

                                for (int c = 0; c < Variable.GUI_YML.getStringList(String.valueOf(temp) + ".Enchants").size(); c++) {
                                    String[] tempenc = ((String)Variable.GUI_YML.getStringList(String.valueOf(temp) + ".Enchants").get(c)).split("\\,");
                                    meta.addEnchant(Enchantment.getByName(tempenc[0]), Integer.valueOf(tempenc[1]), true);
                                }

                                meta.setLore(lores);
                                item.setItemMeta(meta);
                                VisitGui.this.MainGui.setItem(Variable.GUI_YML.getInt(String.valueOf(temp) + ".Index") - 1, item);
                            }
                        }

                        int amount = 0;

                        for (int c = VisitGui.this.NowPage * (29 - VisitGui.this.item_add_amount);
                            c < VisitGui.this.players.size() && c < (VisitGui.this.NowPage + 1) * (29 - VisitGui.this.item_add_amount) && c >= 0;
                            c++
                        ) {
                            if (Util.CheckIsHome(VisitGui.this.players.get(c).getName().replace(Variable.world_prefix, ""))) {
                                if (!Variable.GUI_YML.getString("HeadMaterial").toUpperCase().contains("HEAD")
                                    && !Variable.GUI_YML.getString("HeadMaterial").toUpperCase().contains("SKULL")) {
                                    ItemStack item = new ItemStack(Material.valueOf(Variable.GUI_YML.getString("HeadMaterial")));
                                    ItemMeta i_meta = item.getItemMeta();
                                    i_meta.setDisplayName(
                                        String.valueOf(Variable.Lang_YML.getString("VisitGuiHomePrefix"))
                                            + VisitGui.this.players.get(c).getName().replace(Variable.world_prefix, "")
                                            + Variable.Lang_YML.getString("VisitGuiHomeSuffix")
                                    );
                                    Home home = VisitGui.this.players.get(c);
                                    List<String> lores = new ArrayList<>();

                                    for (int i = 0; i < Variable.GUI_YML.getStringList("VisitGuiLores").size() - 1; i++) {
                                        String temp = ((String)Variable.GUI_YML.getStringList("VisitGuiLores").get(i))
                                            .replace("<Name>", VisitGui.this.players.get(c).getName().replace(Variable.world_prefix, ""));
                                        temp = PlaceholderAPI.setPlaceholders(null, temp);
                                        lores.add(temp);
                                    }

                                    if (Variable.GUI_YML.getBoolean("VisitSlogan")) {
                                        for (String str : home.getAdvertisement()) {
                                            lores.add(str);
                                        }
                                    }

                                    lores.add(
                                        PlaceholderAPI.setPlaceholders(
                                            null,
                                            (String)Variable.GUI_YML
                                                .getStringList("VisitGuiLores")
                                                .get(Variable.GUI_YML.getStringList("VisitGuiLores").size() - 1)
                                        )
                                    );
                                    String temp = ((String)Variable.GUI_YML
                                            .getStringList("VisitGuiLores")
                                            .get(Variable.GUI_YML.getStringList("VisitGuiLores").size() - 1))
                                        .replace("<Name>", VisitGui.this.players.get(c).getName().replace(Variable.world_prefix, ""));
                                    temp = PlaceholderAPI.setPlaceholders(null, temp);
                                    lores.add(temp);
                                    item.setItemMeta(i_meta);
                                    if (!home.getIcon().equalsIgnoreCase("")) {
                                        String[] item_info = home.getIcon().split(":");
                                        item.setType(Material.valueOf(item_info[0]));
                                        item.setDurability(Short.valueOf(item_info[1]));
                                    }

                                    item.setAmount(1);
                                    VisitGui.this.MainGui.addItem(new ItemStack[]{item});
                                } else {
                                    try {
                                        new ItemStack(Material.valueOf(Variable.GUI_YML.getString("HeadMaterial")), 1, (short)SkullType.PLAYER.ordinal());
                                    } catch (Exception e) {
                                        String temp5 = Variable.Lang_YML.getString("PlayerHeadMaterialNotFound");
                                        if (temp5.contains("<Material>")) {
                                            temp5 = temp5.replace("<Material>", Variable.GUI_YML.getString("HeadMaterial"));
                                        }

                                        Bukkit.getConsoleSender().sendMessage(temp5);
                                        return;
                                    }

                                    ItemStack skull = new ItemStack(
                                        Material.valueOf(Variable.GUI_YML.getString("HeadMaterial")), 1, (short)SkullType.PLAYER.ordinal()
                                    );
                                    SkullMeta player_SKULL = (SkullMeta)skull.getItemMeta();
                                    Player temp_p = Bukkit.getPlayer(VisitGui.this.players.get(c).getName().replace(Variable.world_prefix, ""));
                                    if (Variable.GUI_YML.getBoolean("EnableSkullSkin") && temp_p != null) {
                                        try {
                                            player_SKULL.setOwningPlayer(temp_p);
                                        } catch (Exception var15) {
                                        }
                                    }

                                    player_SKULL.setDisplayName(
                                        String.valueOf(Variable.Lang_YML.getString("VisitGuiHomePrefix"))
                                            + VisitGui.this.players.get(c).getName().replace(Variable.world_prefix, "")
                                            + Variable.Lang_YML.getString("VisitGuiHomeSuffix")
                                    );
                                    Home home = VisitGui.this.players.get(c);
                                    List<String> lores = new ArrayList<>();

                                    for (int i = 0; i < Variable.GUI_YML.getStringList("VisitGuiLores").size() - 1; i++) {
                                        String temp = ((String)Variable.GUI_YML.getStringList("VisitGuiLores").get(i))
                                            .replace("<Name>", VisitGui.this.players.get(c).getName().replace(Variable.world_prefix, ""));
                                        temp = PlaceholderAPI.setPlaceholders(null, temp);
                                        lores.add(temp);
                                    }

                                    if (Variable.GUI_YML.getBoolean("VisitSlogan")) {
                                        for (String str : home.getAdvertisement()) {
                                            lores.add(str);
                                        }
                                    }

                                    String temp = ((String)Variable.GUI_YML
                                            .getStringList("VisitGuiLores")
                                            .get(Variable.GUI_YML.getStringList("VisitGuiLores").size() - 1))
                                        .replace("<Name>", VisitGui.this.players.get(c).getName().replace(Variable.world_prefix, ""));
                                    temp = PlaceholderAPI.setPlaceholders(null, temp);
                                    lores.add(temp);
                                    player_SKULL.setLore(lores);
                                    skull.setItemMeta(player_SKULL);
                                    if (!home.getIcon().equalsIgnoreCase("")) {
                                        String[] item_info = home.getIcon().split(":");
                                        skull.setType(Material.valueOf(item_info[0]));
                                        skull.setDurability(Short.valueOf(item_info[1]));
                                    }

                                    skull.setAmount(1);
                                    VisitGui.this.MainGui.addItem(new ItemStack[]{skull});
                                }
                            }
                        }

                        (new BukkitRunnable() {
                            public void run() {
                                p.openInventory(VisitGui.this.MainGui);
                            }
                        }).runTask(Main.JavaPlugin);
                    }
                }
            })
            .runTaskAsynchronously(Main.JavaPlugin);
    }

    public void OpenPrevInventory(final Player p) {
        (new BukkitRunnable() {
                public void run() {
                    if (VisitGui.this.NowPage - 1 >= 0) {
                        VisitGui.this.NowPage--;
                        VisitGui.this.MainGui.clear();
                        if (Variable.GUI_YML.getBoolean("EnableVisitGuiNormalPane")) {
                            try {
                                new ItemStack(Material.valueOf(Variable.GUI_YML.getString("PaneMaterial")));
                            } catch (Exception e) {
                                String temp5 = Variable.Lang_YML.getString("GlassPaneNotFound");
                                if (temp5.contains("<Material>")) {
                                    temp5 = temp5.replace("<Material>", Variable.GUI_YML.getString("PaneMaterial"));
                                }

                                Bukkit.getConsoleSender().sendMessage(temp5);
                                return;
                            }

                            ItemStack blb1 = new ItemStack(Material.valueOf(Variable.GUI_YML.getString("PaneMaterial")));
                            blb1.setDurability((short)15);
                            ItemMeta i1 = blb1.getItemMeta();
                            i1.setDisplayName("");
                            blb1.setItemMeta(i1);

                            for (int i = 0; i < 9; i++) {
                                VisitGui.this.MainGui.setItem(i, blb1);
                            }

                            VisitGui.this.MainGui.setItem(9, blb1);
                            VisitGui.this.MainGui.setItem(18, blb1);
                            VisitGui.this.MainGui.setItem(27, blb1);
                            VisitGui.this.MainGui.setItem(17, blb1);
                            VisitGui.this.MainGui.setItem(26, blb1);
                            VisitGui.this.MainGui.setItem(35, blb1);
                            VisitGui.this.MainGui.setItem(36, blb1);
                            VisitGui.this.MainGui.setItem(44, blb1);

                            for (int i = 45; i < 54; i++) {
                                if (i != 49) {
                                    VisitGui.this.MainGui.setItem(i, blb1);
                                }
                            }
                        }

                        ItemStack next = new ItemStack(Material.valueOf(Variable.GUI_YML.getString("NextMaterial")));
                        ItemMeta next_meta = next.getItemMeta();
                        next_meta.setDisplayName(Variable.GUI_YML.getString("Next"));
                        next.setItemMeta(next_meta);
                        VisitGui.this.MainGui.setItem(53, next);
                        ItemStack prev = new ItemStack(Material.valueOf(Variable.GUI_YML.getString("PrevMaterial")));
                        ItemMeta prev_meta = next.getItemMeta();
                        prev_meta.setDisplayName(Variable.GUI_YML.getString("Prev"));
                        prev.setItemMeta(prev_meta);
                        VisitGui.this.MainGui.setItem(45, prev);
                        ConfigurationSection cs = Variable.GUI_YML.getConfigurationSection("");

                        for (String temp : cs.getKeys(false)) {
                            if (Variable.GUI_YML.getString(String.valueOf(temp) + ".InMenu") != null
                                && Variable.GUI_YML.getString(String.valueOf(temp) + ".InMenu").equalsIgnoreCase("Visit")) {
                                try {
                                    new ItemStack(Material.valueOf(Variable.GUI_YML.getString(String.valueOf(temp) + ".Material")));
                                } catch (Exception e) {
                                    String temp5 = Variable.Lang_YML.getString("MaterialNotFound");
                                    if (temp5.contains("<Material>")) {
                                        temp5 = temp5.replace("<Material>", Variable.GUI_YML.getString(String.valueOf(temp) + ".Material"));
                                    }

                                    if (temp5.contains("<ID>")) {
                                        temp5 = temp5.replace("<ID>", temp);
                                    }

                                    Bukkit.getConsoleSender().sendMessage(temp5);
                                    return;
                                }

                                ItemStack item = new ItemStack(Material.valueOf(Variable.GUI_YML.getString(String.valueOf(temp) + ".Material")));
                                if (Variable.GUI_YML.getInt(String.valueOf(temp) + ".SubID") != 0) {
                                    item.setDurability((short)Variable.GUI_YML.getInt(String.valueOf(temp) + ".SubID"));
                                }

                                ItemMeta meta = item.getItemMeta();
                                List<String> lores = new ArrayList<>();
                                meta.setDisplayName(Variable.GUI_YML.getString(String.valueOf(temp) + ".CustomName"));

                                for (int i = 0; i < Variable.GUI_YML.getStringList(String.valueOf(temp) + ".Lores").size(); i++) {
                                    String tempstr = (String)Variable.GUI_YML.getStringList(String.valueOf(temp) + ".Lores").get(i);

                                    try {
                                        tempstr = PlaceholderAPI.setPlaceholders(p, tempstr);
                                    } catch (Exception var16) {
                                    }

                                    lores.add(tempstr);
                                }

                                for (int c = 0; c < Variable.GUI_YML.getStringList(String.valueOf(temp) + ".Enchants").size(); c++) {
                                    String[] tempenc = ((String)Variable.GUI_YML.getStringList(String.valueOf(temp) + ".Enchants").get(c)).split("\\,");
                                    meta.addEnchant(Enchantment.getByName(tempenc[0]), Integer.valueOf(tempenc[1]), true);
                                }

                                meta.setLore(lores);
                                item.setItemMeta(meta);
                                VisitGui.this.MainGui.setItem(Variable.GUI_YML.getInt(String.valueOf(temp) + ".Index") - 1, item);
                            }
                        }

                        int amount = 0;

                        for (int c = VisitGui.this.NowPage * (29 - VisitGui.this.item_add_amount);
                            c < VisitGui.this.players.size() && c < (VisitGui.this.NowPage + 1) * (29 - VisitGui.this.item_add_amount) && c >= 0;
                            c++
                        ) {
                            if (Util.CheckIsHome(VisitGui.this.players.get(c).getName().replace(Variable.world_prefix, ""))) {
                                if (!Variable.GUI_YML.getString("HeadMaterial").toUpperCase().contains("HEAD")
                                    && !Variable.GUI_YML.getString("HeadMaterial").toUpperCase().contains("SKULL")) {
                                    ItemStack item = new ItemStack(Material.valueOf(Variable.GUI_YML.getString("HeadMaterial")));
                                    ItemMeta i_meta = item.getItemMeta();
                                    i_meta.setDisplayName(
                                        String.valueOf(Variable.Lang_YML.getString("VisitGuiHomePrefix"))
                                            + VisitGui.this.players.get(c).getName().replace(Variable.world_prefix, "")
                                            + Variable.Lang_YML.getString("VisitGuiHomeSuffix")
                                    );
                                    Home home = VisitGui.this.players.get(c);
                                    List<String> lores = new ArrayList<>();

                                    for (int i = 0; i < Variable.GUI_YML.getStringList("VisitGuiLores").size() - 1; i++) {
                                        String temp = ((String)Variable.GUI_YML.getStringList("VisitGuiLores").get(i))
                                            .replace("<Name>", VisitGui.this.players.get(c).getName().replace(Variable.world_prefix, ""));
                                        temp = PlaceholderAPI.setPlaceholders(null, temp);
                                        lores.add(temp);
                                    }

                                    if (Variable.GUI_YML.getBoolean("VisitSlogan")) {
                                        for (String str : home.getAdvertisement()) {
                                            lores.add(str);
                                        }
                                    }

                                    String temp = ((String)Variable.GUI_YML
                                            .getStringList("VisitGuiLores")
                                            .get(Variable.GUI_YML.getStringList("VisitGuiLores").size() - 1))
                                        .replace("<Name>", VisitGui.this.players.get(c).getName().replace(Variable.world_prefix, ""));
                                    temp = PlaceholderAPI.setPlaceholders(null, temp);
                                    lores.add(temp);
                                    i_meta.setLore(lores);
                                    item.setItemMeta(i_meta);
                                    if (!home.getIcon().equalsIgnoreCase("")) {
                                        String[] item_info = home.getIcon().split(":");
                                        item.setType(Material.valueOf(item_info[0]));
                                        item.setDurability(Short.valueOf(item_info[1]));
                                    }

                                    item.setAmount(1);
                                    VisitGui.this.MainGui.addItem(new ItemStack[]{item});
                                } else {
                                    try {
                                        new ItemStack(Material.valueOf(Variable.GUI_YML.getString("HeadMaterial")), 1, (short)SkullType.PLAYER.ordinal());
                                    } catch (Exception e) {
                                        String temp5 = Variable.Lang_YML.getString("PlayerHeadMaterialNotFound");
                                        if (temp5.contains("<Material>")) {
                                            temp5 = temp5.replace("<Material>", Variable.GUI_YML.getString("HeadMaterial"));
                                        }

                                        Bukkit.getConsoleSender().sendMessage(temp5);
                                        return;
                                    }

                                    ItemStack skull = new ItemStack(
                                        Material.valueOf(Variable.GUI_YML.getString("HeadMaterial")), 1, (short)SkullType.PLAYER.ordinal()
                                    );
                                    SkullMeta player_SKULL = (SkullMeta)skull.getItemMeta();
                                    Player temp_p = Bukkit.getPlayer(VisitGui.this.players.get(c).getName().replace(Variable.world_prefix, ""));
                                    if (Variable.GUI_YML.getBoolean("EnableSkullSkin") && temp_p != null) {
                                        try {
                                            player_SKULL.setOwningPlayer(temp_p);
                                        } catch (Exception var15) {
                                        }
                                    }

                                    player_SKULL.setDisplayName(
                                        String.valueOf(Variable.Lang_YML.getString("VisitGuiHomePrefix"))
                                            + VisitGui.this.players.get(c).getName().replace(Variable.world_prefix, "")
                                            + Variable.Lang_YML.getString("VisitGuiHomeSuffix")
                                    );
                                    Home home = VisitGui.this.players.get(c);
                                    List<String> lores = new ArrayList<>();

                                    for (int i = 0; i < Variable.GUI_YML.getStringList("VisitGuiLores").size() - 1; i++) {
                                        String temp = ((String)Variable.GUI_YML.getStringList("VisitGuiLores").get(i))
                                            .replace("<Name>", VisitGui.this.players.get(c).getName().replace(Variable.world_prefix, ""));
                                        temp = PlaceholderAPI.setPlaceholders(null, temp);
                                        lores.add(temp);
                                    }

                                    if (Variable.GUI_YML.getBoolean("VisitSlogan")) {
                                        for (String str : home.getAdvertisement()) {
                                            lores.add(str);
                                        }
                                    }

                                    String temp = ((String)Variable.GUI_YML
                                            .getStringList("VisitGuiLores")
                                            .get(Variable.GUI_YML.getStringList("VisitGuiLores").size() - 1))
                                        .replace("<Name>", VisitGui.this.players.get(c).getName().replace(Variable.world_prefix, ""));
                                    temp = PlaceholderAPI.setPlaceholders(null, temp);
                                    lores.add(temp);
                                    player_SKULL.setLore(lores);
                                    skull.setItemMeta(player_SKULL);
                                    if (!home.getIcon().equalsIgnoreCase("")) {
                                        String[] item_info = home.getIcon().split(":");
                                        skull.setType(Material.valueOf(item_info[0]));
                                        skull.setDurability(Short.valueOf(item_info[1]));
                                    }

                                    skull.setAmount(1);
                                    VisitGui.this.MainGui.addItem(new ItemStack[]{skull});
                                }
                            }
                        }

                        (new BukkitRunnable() {
                            public void run() {
                                p.openInventory(VisitGui.this.MainGui);
                            }
                        }).runTask(Main.JavaPlugin);
                    }
                }
            })
            .runTaskAsynchronously(Main.JavaPlugin);
    }

    public Inventory getInventory() {
        return this.MainGui;
    }
}
