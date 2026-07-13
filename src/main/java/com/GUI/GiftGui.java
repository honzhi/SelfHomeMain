package com.GUI;

import com.SelfHome.Main;
import com.SelfHome.Variable;
import com.Util.Home;
import com.Util.HomeAPI;
import com.comphenix.protocol.utility.StreamSerializer;
import java.io.IOException;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

public class GiftGui implements InventoryHolder {
    public Inventory MainGui = Bukkit.createInventory(this, 45, Variable.Lang_YML.getString("GiftGuiTitle"));

    public GiftGui(Player p, final String Name) {
        (new BukkitRunnable() {
            public void run() {
                GiftGui.this.MainGui.clear();
                Home home = HomeAPI.getHome(Name);
                List<String> list = home.getGifts();
                if (list != null) {
                    StreamSerializer ss = new StreamSerializer();
                    int amount = 0;

                    for (String str : list) {
                        amount++;
                        ItemStack i = null;
                        if (str != null && !str.equalsIgnoreCase("")) {
                            try {
                                i = ss.deserializeItemStack(str);
                            } catch (IllegalArgumentException e) {
                                continue;
                            } catch (IOException e) {
                                continue;
                            }

                            GiftGui.this.MainGui.addItem(new ItemStack[]{i});
                            if (amount >= 45) {
                                break;
                            }
                        }
                    }
                }
            }
        }).runTaskAsynchronously(Main.JavaPlugin);
    }

    public Inventory getInventory() {
        return this.MainGui;
    }
}
