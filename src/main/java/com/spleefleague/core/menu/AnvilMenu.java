/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.spleefleague.core.menu;

import com.spleefleague.core.Core;
import com.spleefleague.core.player.CorePlayer;
import com.spleefleague.core.menu.anvilgui.AnvilGUI;
import java.util.function.BiConsumer;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

/**
 * @author NickM13
 */
public class AnvilMenu extends InventoryMenu {
    
    private BiConsumer<CorePlayer, String> anvilAction;
    
    public static AnvilMenu create() {
        return new AnvilMenu();
    }
    
    protected AnvilMenu() {
        super(true);
    }
    
    @Override
    public void openInventory(CorePlayer cp) {
        AnvilGUI.Builder builder = new AnvilGUI.Builder();
        builder.onComplete((p, s) -> { anvilAction.accept(cp, s); return AnvilGUI.Response.close(); });
        builder.title(titleFun.apply(cp));
        builder.item(new ItemStack(Material.PLAYER_HEAD));
        builder.plugin(Core.getInstance());
        
        builder.open(cp.getPlayer());
    }
    
    public AnvilMenu setAction(BiConsumer<CorePlayer, String> anvilAction) {
        this.anvilAction = anvilAction;
        return this;
    }
    
}
