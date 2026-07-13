package com.Listeners;

import com.SelfHome.Variable;
import com.Util.Util;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

public class BlockBreakListener implements Listener {
    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        if (!event.getPlayer().getName().toUpperCase().contains("AS-FAKEPLAYER".toUpperCase())
            && !event.getPlayer().getName().toUpperCase().contains("[MINECRAFT]".toUpperCase())
            && !event.getPlayer().getName().toUpperCase().contains("[MEKANISM]".toUpperCase())
            && !event.getPlayer().getName().toUpperCase().contains("[IF]".toUpperCase())
            && !event.getPlayer().getName().toUpperCase().contains("[IntegratedTunnels]".toUpperCase())
            && !event.getPlayer().getName().toUpperCase().contains("killer joe".toUpperCase())
            && !event.getPlayer().getName().toUpperCase().contains("[depolyer]".toUpperCase())
            && !event.getPlayer().getName().toUpperCase().contains("[XU2FakePlayer]".toUpperCase())
            && !event.getPlayer().getName().toUpperCase().contains("[Modular Routers]".toUpperCase())) {
            if (Util.CheckIsHome(event.getPlayer().getWorld().getName().replace(Variable.world_prefix, ""))) {
                if (!Util.Check(event.getPlayer(), event.getPlayer().getLocation().getWorld().getName().replace(Variable.world_prefix, ""))) {
                    String temp = Variable.Lang_YML.getString("NoPermissionBreakBlock");
                    event.getPlayer().sendMessage(temp);
                    event.setCancelled(true);
                }
            }
        }
    }
}
