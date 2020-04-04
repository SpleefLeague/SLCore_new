/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.spleefleague.core.listener;

import com.spleefleague.core.Core;
import com.spleefleague.core.menu.InventoryMenu;
import com.spleefleague.core.player.CorePlayer;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;

/**
 * @author NickM13
 */
public class MenuListener implements Listener {
    
    private static Listener instance;
    
    private MenuListener() {}
    
    public static void init() {
        if (instance == null) {
            instance = new MenuListener();
            Bukkit.getPluginManager().registerEvents(instance, Core.getInstance());
        }
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent e) {
        CorePlayer cp = Core.getInstance().getPlayers().get(e.getPlayer());
        
        if ((e.getAction().equals(Action.RIGHT_CLICK_AIR) || e.getAction().equals(Action.RIGHT_CLICK_BLOCK))
                && e.getItem() != null) {
            for (InventoryMenu.InvMenuType type : InventoryMenu.InvMenuType.values()) {
                InventoryMenu menu = InventoryMenu.getHotbarMenu(e.getItem());
                if (menu != null) {
                    menu.call(cp);
                }
            }
        }
    }
    
    @EventHandler
    public void onSwapHandItems(PlayerSwapHandItemsEvent e) {
        CorePlayer cp = Core.getInstance().getPlayers().get(e.getPlayer());
        if (!cp.canBuild() || cp.isAfk()) {
            e.setCancelled(true);
        }
        cp.setLastAction();
    }
    
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent e) {
        CorePlayer cp = Core.getInstance().getPlayers().get(e.getPlayer().getName());
        if (cp != null) {
            cp.closeInventoryMenu();
            cp.setLastAction();
        }
    }
    
    @EventHandler(priority = EventPriority.NORMAL)
    public void onInventoryInteract(InventoryClickEvent e) {
        if (e.getClickedInventory() == null) return;
        CorePlayer cp = Core.getInstance().getPlayers().get(e.getWhoClicked().getName());
        
        if (cp.isAfk()) {
            cp.setLastAction();
            if (e.getCurrentItem() != null && e.getCurrentItem().getItemMeta() != null) {
                String name = e.getCurrentItem().getItemMeta().getDisplayName();
                if (name != null && ChatColor.stripColor(name).equalsIgnoreCase("afk")) {
                    e.setCurrentItem(null);
                }
            }
            e.setCancelled(true);
            return;
        }
        
        boolean isHotbarMenu = false;
        if (e.getCurrentItem() != null
                && !e.getCurrentItem().getType().equals(Material.AIR)) {
            isHotbarMenu = InventoryMenu.isHotbarMenu(e.getCurrentItem());
        }
        
        if (e.getCurrentItem() != null &&
                isHotbarMenu &&
                !cp.canBuild()) {
            e.setCancelled(true);
        } else if (cp.getInventoryMenu() != null) {
            if (e.getClickedInventory().getType() == InventoryType.PLAYER) {
                if (!cp.getInventoryMenu().isEditting()) {
                    e.setCancelled(true);
                }
            }
            else if (e.getClickedInventory().getType() == InventoryType.CHEST) {
                InventoryMenu menu = cp.getInventoryMenu();
                InventoryMenu clicked = menu.getMenuItem(cp, e.getSlot());
                if (!menu.isEditting()) {
                    e.setCancelled(true);
                    if (clicked != null && cp.getRank().hasPermission(clicked.getRank())) {
                        Bukkit.getScheduler().runTask(Core.getInstance(), () -> {
                            clicked.call(cp);
                            if (clicked.closeOnAction() && clicked.isAvailable(cp)) {
                                cp.setInventoryMenu(clicked);
                            } else {
                                cp.setInventoryMenu(cp.getInventoryMenu());
                            }
                        });
                    }
                } else {
                    if (e.getSlot() < InventoryMenu.MENU_COUNT) {
                        e.setCancelled(true);
                        if (clicked != null) {
                            menu.removeMenuItem(e.getSlot());
                        }
                        if (!e.getCursor().getType().equals(Material.AIR)) {
                            menu.addMenuItem(InventoryMenu.createItem(e.getCursor()), e.getSlot() + cp.getPage() * InventoryMenu.MENU_COUNT);
                        }
                        cp.getPlayer().setItemOnCursor(e.getCurrentItem());
                        Bukkit.getScheduler().runTaskLater(Core.getInstance(), () -> {
                            cp.setInventoryMenu(cp.getInventoryMenu());
                            cp.savePage();
                        }, 3L);
                    } else {
                        e.setCancelled(true);
                        if (clicked != null && cp.getRank().hasPermission(clicked.getRank())) {
                            Bukkit.getScheduler().runTask(Core.getInstance(), () -> {
                                clicked.call(cp);
                                if (clicked.closeOnAction()) {
                                    cp.setInventoryMenu(clicked);
                                } else {
                                    cp.setInventoryMenu(cp.getInventoryMenu());
                                }
                            });
                        } else {
                            cp.setInventoryMenu(cp.getInventoryMenu());
                        }
                    }
                }
            }
        } else if (!cp.canBuild()) {
            e.setCancelled(true);
        }
    }
    
    @EventHandler
    public void onAnvilPrepare(PrepareAnvilEvent e) {
        e.getInventory();
    }
    
}
