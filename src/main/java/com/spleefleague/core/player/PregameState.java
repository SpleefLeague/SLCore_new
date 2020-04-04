/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.spleefleague.core.player;

import com.google.common.collect.Sets;
import com.spleefleague.core.util.database.DBPlayer;
import java.util.Set;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

/**
 * @author NickM13
 */
public class PregameState {
    
    public enum PSFlag {
        LOCATION,
        INVENTORY,
        GAMEMODE,
        ALL
    }
    
    private final DBPlayer dbp;
    private Location location = null;
    private ItemStack[] inventory = null;
    private GameMode gameMode = null;
    
    public PregameState(DBPlayer dbp) {
        this.dbp = dbp;
    }
    
    public void save(PSFlag ... include) {
        Set<PSFlag> flags = Sets.newHashSet(include);
        if (flags.contains(PSFlag.ALL)) {
            location = dbp.getPlayer().getLocation();
            inventory = dbp.getPlayer().getInventory().getContents();
            gameMode = dbp.getPlayer().getGameMode();
        } else {
            if (flags.contains(PSFlag.LOCATION)) {
                location = dbp.getPlayer().getLocation();
            }
            if (flags.contains(PSFlag.INVENTORY)) {
                inventory = dbp.getPlayer().getInventory().getContents();
            }
            if (flags.contains(PSFlag.GAMEMODE)) {
                gameMode = dbp.getPlayer().getGameMode();
            }
        }
    }
    
    public void clear() {
        location = null;
        inventory = null;
    }
    
    public void load() {
        if (location != null) {
            dbp.getPlayer().teleport(location);
            location = null;
        }
        if (inventory != null) {
            dbp.getPlayer().getInventory().setContents(inventory);
        }
        if (gameMode != null) {
            dbp.setGameMode(gameMode);
        }
        clear();
    }
    
    public Location getLocation() {
        return location;
    }
    
    public ItemStack[] getInventory() {
        return inventory;
    }
    
}
