package com.Listeners;

import com.SelfHome.Variable;
import com.Util.Util;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

public class HomeProtectInteractListener implements Listener {
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        try {
            event.getClickedBlock().getType();
            Material.valueOf(Variable.Soil);
        } catch (Exception e) {
            return;
        }

        if (!event.getClickedBlock().getType().toString().toUpperCase().contains("SIGN")) {
            if (event.getClickedBlock().getType() != Material.valueOf(Variable.Soil) || event.getAction() != Action.PHYSICAL) {
                if (Util.CheckIsHome(event.getPlayer().getWorld().getName().replaceAll(Variable.world_prefix, ""))) {
                    if (!Util.Check(event.getPlayer(), event.getPlayer().getLocation().getWorld().getName().replaceAll(Variable.world_prefix, ""))) {
                        String temp = Variable.Lang_YML.getString("NoPermissionInteract");
                        event.getPlayer().sendMessage(temp);
                        event.setCancelled(true);
                    }
                }
            }
        }
    }
}
