package com.Listeners;

import com.GUI.CheckGui;
import com.GUI.CreateGui;
import com.GUI.DenyGui;
import com.GUI.InviteGui;
import com.GUI.MainGui;
import com.GUI.ManageGui;
import com.GUI.ManageGui2;
import com.GUI.TrustGui;
import com.GUI.VisitGui;
import com.SelfHome.Variable;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class InventoryClickListener implements Listener {
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onOpen(InventoryClickEvent event) {
        if (event.getInventory().getHolder() != null) {
            if (event.getSlot() != -1 && event.getSlot() != 999) {
                Player p = (Player)event.getWhoClicked();
                boolean check_gui_in_plugins = false;
                String holder = "";
                if (event.getInventory().getHolder() instanceof CheckGui) {
                    holder = "Check";
                    check_gui_in_plugins = true;
                } else if (event.getInventory().getHolder() instanceof CreateGui) {
                    holder = "Create";
                    check_gui_in_plugins = true;
                } else if (event.getInventory().getHolder() instanceof DenyGui) {
                    holder = "Deny";
                    check_gui_in_plugins = true;
                } else if (event.getInventory().getHolder() instanceof InviteGui) {
                    holder = "Invite";
                    check_gui_in_plugins = true;
                } else if (event.getInventory().getHolder() instanceof MainGui) {
                    holder = "Main";
                    check_gui_in_plugins = true;
                } else if (event.getInventory().getHolder() instanceof ManageGui) {
                    holder = "Manage";
                    check_gui_in_plugins = true;
                } else if (event.getInventory().getHolder() instanceof ManageGui2) {
                    holder = "Manage2";
                    check_gui_in_plugins = true;
                } else if (event.getInventory().getHolder() instanceof TrustGui) {
                    holder = "Trust";
                    check_gui_in_plugins = true;
                } else if (event.getInventory().getHolder() instanceof VisitGui) {
                    holder = "Visit";
                    check_gui_in_plugins = true;
                }

                if (check_gui_in_plugins) {
                    event.setCancelled(true);
                    ItemStack i = event.getCurrentItem();
                    if (i != null) {
                        if (i.hasItemMeta()) {
                            if (i.getItemMeta().getDisplayName() != null) {
                                if (i.getItemMeta().getDisplayName().equalsIgnoreCase(Variable.GUI_YML.getString("Next"))) {
                                    if (event.getClickedInventory().getHolder() instanceof DenyGui) {
                                        DenyGui gui = (DenyGui)event.getClickedInventory().getHolder();
                                        gui.OpenNextInventory(p);
                                    }

                                    if (event.getClickedInventory().getHolder() instanceof InviteGui) {
                                        InviteGui gui = (InviteGui)event.getClickedInventory().getHolder();
                                        gui.OpenNextInventory(p);
                                    }

                                    if (event.getClickedInventory().getHolder() instanceof TrustGui) {
                                        TrustGui gui = (TrustGui)event.getClickedInventory().getHolder();
                                        gui.OpenNextInventory(p);
                                    }

                                    if (event.getClickedInventory().getHolder() instanceof VisitGui) {
                                        VisitGui gui = (VisitGui)event.getClickedInventory().getHolder();
                                        gui.OpenNextInventory(p);
                                    }
                                } else if (i.getItemMeta().getDisplayName().equalsIgnoreCase(Variable.GUI_YML.getString("Prev"))) {
                                    if (event.getClickedInventory().getHolder() instanceof DenyGui) {
                                        DenyGui gui = (DenyGui)event.getClickedInventory().getHolder();
                                        gui.OpenPrevInventory(p);
                                    }

                                    if (event.getClickedInventory().getHolder() instanceof InviteGui) {
                                        InviteGui gui = (InviteGui)event.getClickedInventory().getHolder();
                                        gui.OpenPrevInventory(p);
                                    }

                                    if (event.getClickedInventory().getHolder() instanceof TrustGui) {
                                        TrustGui gui = (TrustGui)event.getClickedInventory().getHolder();
                                        gui.OpenPrevInventory(p);
                                    }

                                    if (event.getClickedInventory().getHolder() instanceof VisitGui) {
                                        VisitGui gui = (VisitGui)event.getClickedInventory().getHolder();
                                        gui.OpenPrevInventory(p);
                                    }
                                }

                                String name = i.getItemMeta().getDisplayName();
                                if (name.contains(Variable.Lang_YML.getString("VisitGuiHomeSuffix"))
                                    && name.contains(Variable.Lang_YML.getString("VisitGuiHomePrefix"))) {
                                    name = name.replace(Variable.Lang_YML.getString("VisitGuiHomePrefix"), "");
                                    name = name.replace(Variable.Lang_YML.getString("VisitGuiHomeSuffix"), "");
                                    Bukkit.dispatchCommand(p, "sh v " + name);
                                } else if (name.contains(Variable.Lang_YML.getString("CheckGuiHomePrefix"))
                                    && name.contains(Variable.Lang_YML.getString("CheckGuiHomeSuffix"))) {
                                    name = name.replace(Variable.Lang_YML.getString("CheckGuiHomePrefix"), "");
                                    name = name.replace(Variable.Lang_YML.getString("CheckGuiHomeSuffix"), "");
                                    Bukkit.dispatchCommand(p, "sh v " + name);
                                } else if (name.contains(Variable.Lang_YML.getString("TrustGuiPrefix"))) {
                                    name = name.replace(Variable.Lang_YML.getString("TrustGuiPrefix"), "");
                                    if (event.getClick() == ClickType.LEFT) {
                                        Bukkit.dispatchCommand(p, "sh trust " + name);
                                    } else {
                                        Bukkit.dispatchCommand(p, "sh remove " + name);
                                    }
                                } else if (name.contains(Variable.Lang_YML.getString("InviteGuiPrefix"))) {
                                    name = name.replace(Variable.Lang_YML.getString("InviteGuiPrefix"), "");
                                    if (event.getClick() == ClickType.LEFT) {
                                        Bukkit.dispatchCommand(p, "sh invite " + name);
                                    } else {
                                        Bukkit.dispatchCommand(p, "sh kick " + name);
                                    }
                                } else if (name.contains(Variable.Lang_YML.getString("DenyGuiPrefix"))) {
                                    name = name.replace(Variable.Lang_YML.getString("DenyGuiPrefix"), "");
                                    if (event.getClick() == ClickType.LEFT) {
                                        Bukkit.dispatchCommand(p, "sh deny " + name);
                                    } else {
                                        Bukkit.dispatchCommand(p, "sh undeny " + name);
                                    }
                                } else if (getBtnID(i, holder) != null) {
                                    String BTNID = getBtnID(i, holder);
                                    if (event.getClick() == ClickType.LEFT) {
                                        Bukkit.dispatchCommand(p, Variable.GUI_YML.getString(String.valueOf(BTNID) + ".LeftInTo"));
                                        if (Variable.GUI_YML.getString(String.valueOf(BTNID) + ".LeftInTo") != null
                                            && !Variable.GUI_YML.getString(String.valueOf(BTNID) + ".LeftInTo").equalsIgnoreCase("")
                                            && !Variable.GUI_YML.getBoolean(String.valueOf(BTNID) + ".KeepOpen")) {
                                            p.closeInventory();
                                        }
                                    } else {
                                        Bukkit.dispatchCommand(p, Variable.GUI_YML.getString(String.valueOf(BTNID) + ".RightInTo"));
                                        if (event.getClick() == ClickType.RIGHT
                                            && Variable.GUI_YML.getString(String.valueOf(BTNID) + ".LeftInTo") != null
                                            && !Variable.GUI_YML.getString(String.valueOf(BTNID) + ".RightInTo").equalsIgnoreCase("")
                                            && !Variable.GUI_YML.getBoolean(String.valueOf(BTNID) + ".KeepOpen")) {
                                            p.closeInventory();
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public static String getBtnID(ItemStack i, String now) {
        String result = null;
        ConfigurationSection cs = Variable.GUI_YML.getConfigurationSection("");

        for (String temp : cs.getKeys(false)) {
            if (Variable.GUI_YML.getString(String.valueOf(temp) + ".InMenu") != null
                && Variable.GUI_YML.getString(String.valueOf(temp) + ".CustomName").equalsIgnoreCase(i.getItemMeta().getDisplayName())
                && now.equalsIgnoreCase(Variable.GUI_YML.getString(String.valueOf(temp) + ".InMenu"))) {
                result = temp;
                break;
            }
        }

        return result;
    }
}
