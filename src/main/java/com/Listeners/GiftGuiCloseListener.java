package com.Listeners;

import com.GUI.GiftGui;
import com.SelfHome.Main;
import com.SelfHome.Variable;
import com.Util.Home;
import com.Util.HomeAPI;
import com.comphenix.protocol.utility.StreamSerializer;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

public class GiftGuiCloseListener implements Listener {
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onClose(final InventoryCloseEvent event) throws IOException {
        if (event.getInventory().getHolder() instanceof GiftGui) {
            (new BukkitRunnable() {
                public void run() {
                    String home_name = "";

                    for (Entry<String, String> entry : Variable.has_open_gifts_list.entrySet()) {
                        if (entry.getValue().equalsIgnoreCase(event.getPlayer().getName())) {
                            home_name = entry.getKey();
                            Variable.has_open_gifts_list.remove(entry.getKey());
                            break;
                        }
                    }

                    StreamSerializer ss = new StreamSerializer();
                    int amount = 0;
                    Home home = HomeAPI.getHome(home_name);
                    List<String> gifts = new ArrayList<>();
                    ItemStack[] arrayOfItemStack;
                    int j = (arrayOfItemStack = event.getInventory().getContents()).length;

                    for (int b = 0; b < j; b++) {
                        ItemStack i = arrayOfItemStack[b];
                        if (i != null && i.getType() != Material.AIR) {
                            if (++amount > 45) {
                                break;
                            }

                            String str = null;

                            try {
                                str = ss.serializeItemStack(i);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }

                            gifts.add(str);
                        }
                    }

                    try {
                        home.setGifts(gifts);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }).runTaskAsynchronously(Main.JavaPlugin);
        }
    }
}
