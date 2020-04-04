/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.spleefleague.core.menu;

import com.spleefleague.core.Core;
import com.spleefleague.core.chat.Chat;
import com.spleefleague.core.player.CorePlayer;
import com.spleefleague.core.player.Rank;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

/**
 * @author NickM13
 */
public class InventoryMenu {

    /**
     * Inventory Menus are the base component of the menu system,
     * they contain a list of InventoryMenus and InventoryMenuItems
     * to be displayed and linked to
    */
    
    private class InventoryMenuControl {
        int slot;
        InventoryMenu inventoryMenu;
        
        InventoryMenuControl(int slot, InventoryMenu inventoryMenu) {
            this.slot = slot;
            this.inventoryMenu = inventoryMenu;
        }
    }
    
    public static final int MENU_COUNT = 4 * 9;
    
    public enum InvMenuType {
        SLMENU(0),
        HELD(8);
        
        int slot;
        
        InvMenuType(int slot) {
            this.slot = slot;
        }
        
        public int getSlot() {
            return slot;
        }
    }
    
    private static Map<InvMenuType, InventoryMenu> hotbarMenus = new HashMap<>();
    
    public static void init() {
        hotbarMenus.put(InvMenuType.SLMENU, InventoryMenu.createMenu()
                .setTitle("Main Menu")
                .setName(ChatColor.RESET + "" + Chat.PLUGIN_PREFIX + "" + ChatColor.BOLD + "SpleefLeague Menu")
                .setDisplayItem(new ItemStack(Material.COMPASS))
                .setAction(cp -> { cp.setInventoryMenu(hotbarMenus.get(InvMenuType.SLMENU)); })
                .setHotbar(InvMenuType.SLMENU.toString()));
        hotbarMenus.put(InvMenuType.HELD, InventoryMenu.createItem()
                .setName(cp -> cp.getHeldItem().getDisplayName())
                .setDisplayItem(cp -> cp.getHeldItem().getItem())
                .setDescription(cp -> cp.getHeldItem().getDescription())
                .setAction(cp -> { cp.openHeldItem(); })
                .setHotbar(InvMenuType.HELD.toString()));
    }
    
    public static void giveHotbarMenu(CorePlayer cp, InvMenuType type) {
        cp.getPlayer().getInventory().setItem(type.getSlot(), hotbarMenus.get(type).createMenuItem(cp));
    }
    public static InventoryMenu getHotbarMenu(InvMenuType type) {
        return hotbarMenus.get(type);
    }
    public static InventoryMenu getHotbarMenu(ItemStack item) {
        String name = item.getItemMeta().getPersistentDataContainer().get(new NamespacedKey(Core.getInstance(), "menuitem"), PersistentDataType.STRING);
        if (name != null && InvMenuType.valueOf(name) != null) {
            return hotbarMenus.get(InvMenuType.valueOf(name));
        }
        return null;
    }
    public static boolean isHotbarMenu(ItemStack item) {
        String name = item.getItemMeta().getPersistentDataContainer().get(new NamespacedKey(Core.getInstance(), "menuitem"), PersistentDataType.STRING);
        if (name != null) {
            for (InvMenuType type : InvMenuType.values()) {
                if (type.toString().equalsIgnoreCase(name)) {
                    return true;
                }
            }
        }
        return false;
    }
    public static boolean isMenuItem(ItemStack item) {
        return item.getItemMeta().getPersistentDataContainer().get(new NamespacedKey(Core.getInstance(), "menuitem"), PersistentDataType.STRING) != null;
    }
    
    public static InventoryMenu createMenu() {
        return new InventoryMenu(true);
    }
    public static InventoryMenu createItem() {
        return new InventoryMenu(false);
    }
    public static InventoryMenu createItem(ItemStack item) {
        ItemMeta itemMeta = item.getItemMeta();
        InventoryMenu menu = new InventoryMenu(false)
                .setName(itemMeta.getDisplayName())
                .setDescription(itemMeta.getLore())
                .setDisplayItem(item);
        return menu;
    }
    public static ItemStack createItem(Material displayItem, int damage) {
        ItemStack itemStack = new ItemStack(displayItem);
        ItemMeta itemMeta = itemStack.getItemMeta();
        if (itemMeta instanceof Damageable) {
            ((Damageable)itemMeta).setDamage(damage);
        }
        itemMeta.setUnbreakable(true);
        itemMeta.addItemFlags(ItemFlag.values());
        itemStack.setItemMeta(itemMeta);
        return itemStack;
    }
    public static ItemStack createItem(String name, Material displayItem, int damage) {
        ItemStack item = createItem(displayItem, damage);
        item.getItemMeta().setDisplayName(name);
        return item;
    }
    
    // Inventory Menu vars
    protected String hotbarName;
    protected Function<CorePlayer, String> titleFun;
    protected int size;
    protected List<InventoryMenu> unsortedItems;
    protected HashMap<Integer, InventoryMenu> items;
    //protected HashMap<Integer, InventoryMenu> controlItems;
    protected List<InventoryMenuControl> controlItems;
    protected boolean backButton;
    
    // Inventory Item vars
    protected Function<CorePlayer, String> nameFun;
    protected Rank rank;
    protected Function<CorePlayer, ItemStack> displayItemFun;
    protected Function<CorePlayer, String> descriptionFun;
    protected Function<CorePlayer, Boolean> visibilityFun;
    protected Consumer<CorePlayer> action;
    protected boolean closeOnAction;
    protected Function<CorePlayer, Boolean> availableFun;
    
    protected Consumer<HashMap<Integer, InventoryMenu>> saveFun;
    protected boolean editting;
    
    protected InventoryMenu(boolean hasMenu) {
        this.hotbarName = "";
        this.titleFun = cp -> "";
        this.size = hasMenu ? 6 : 0;
        this.unsortedItems = new ArrayList<>();
        this.items = new HashMap<>();
        this.controlItems = new ArrayList<>();
        this.backButton = false;
        
        this.nameFun = cp -> "";
        this.rank = Rank.DEFAULT;
        this.displayItemFun = null;
        this.descriptionFun = null;
        this.visibilityFun = null;
        this.action = null;
        this.closeOnAction = true;
        this.editting = false;
        this.availableFun = null;
    }
    
    private void initControls() {
        controlItems.add(0, new InventoryMenuControl(6 * 9 - 1, InventoryMenu.createItem()
                .setName("Next Page")
                .setDescription("")
                .setDisplayItem(Material.DIAMOND_AXE, 8)
                .setCloseOnAction(false)
                .setVisibility(cp -> (cp.getPage() < this.getPageCount(cp) - 1 || editting))
                .setAction(cp -> { cp.nextPage(); })));
        
        controlItems.add(0, new InventoryMenuControl(6 * 9 - 9, InventoryMenu.createItem()
                .setName("Prev Page")
                .setDescription("")
                .setDisplayItem(Material.DIAMOND_AXE, 9)
                .setCloseOnAction(false)
                .setVisibility(cp -> cp.getPage() > 0)
                .setAction(cp -> { cp.prevPage(); })));
    }
    
    public InventoryMenu setHotbar(String name) {
        hotbarName = name;
        return this;
    }
    
    public InventoryMenu setTitle(String title) {
        this.titleFun = cp -> title;
        return this;
    }
    public String getTitle(CorePlayer cp) {
        return titleFun.apply(cp);
    }
    public InventoryMenu setTitle(Function<CorePlayer, String> title) {
        this.titleFun = title;
        return this;
    }
    
    public InventoryMenu setAvailable(Function<CorePlayer, Boolean> available) {
        this.availableFun = available;
        return this;
    }
    public boolean isAvailable(CorePlayer cp) {
        if (availableFun == null) {
            return true;
        } else {
            return availableFun.apply(cp);
        }
    }
    
    public InventoryMenu setSize(int size) {
        this.size = size;
        return this;
    }
    
    public InventoryMenu setBackButton(boolean backButton) {
        this.backButton = backButton;
        return this;
    }
    
    public InventoryMenu setName(String name) {
        this.nameFun = cp -> name;
        return this;
    }
    public InventoryMenu setName(Function<CorePlayer, String> name) {
        this.nameFun = name;
        return this;
    }
    public String getName() {
        if (nameFun == null) return "";
        return nameFun.apply(null);
    }
    
    public InventoryMenu setRank(Rank rank) {
        this.rank = rank;
        return this;
    }
    public Rank getRank() {
        return rank;
    }
    
    public InventoryMenu setDisplayItem(ItemStack displayItem) {
        this.displayItemFun = cp -> displayItem;
        return this;
    }
    public InventoryMenu setDisplayItem(Function<CorePlayer, ItemStack> displayItemFun) {
        this.displayItemFun = displayItemFun;
        return this;
    }
    public InventoryMenu setDisplayItem(Material displayItem) {
        ItemStack itemStack = new ItemStack(displayItem);
        ItemMeta itemMeta = itemStack.getItemMeta();
        itemMeta.setUnbreakable(true);
        itemMeta.addItemFlags(ItemFlag.values());
        itemStack.setItemMeta(itemMeta);
        this.displayItemFun = cp -> itemStack;
        return this;
    }
    public InventoryMenu setDisplayItem(Material displayItem, int damage) {
        ItemStack itemStack = new ItemStack(displayItem);
        ItemMeta itemMeta = itemStack.getItemMeta();
        if (itemMeta instanceof Damageable) {
            ((Damageable)itemMeta).setDamage(damage);
        }
        itemMeta.setUnbreakable(true);
        itemMeta.addItemFlags(ItemFlag.values());
        itemStack.setItemMeta(itemMeta);
        this.displayItemFun = cp -> itemStack;
        return this;
    }
    
    public InventoryMenu setDescription(String description) {
        this.descriptionFun = cp -> description;
        return this;
    }
    public InventoryMenu setDescription(List<String> lore) {
        this.descriptionFun = cp -> {
            String description = "";
            for (String line : lore) {
                description += line;
            }
            return description;
        };
        return this;
    }
    public InventoryMenu setDescription(Function<CorePlayer, String> descriptionFun) {
        this.descriptionFun = descriptionFun;
        return this;
    }
    
    public InventoryMenu setVisibility(Function<CorePlayer, Boolean> visibilityFun) {
        this.visibilityFun = visibilityFun;
        return this;
    }
    public boolean isVisible(CorePlayer cp) {
        return (visibilityFun == null || visibilityFun.apply(cp));
    }
    
    public InventoryMenu setAction(Consumer<CorePlayer> action) {
        this.action = action;
        return this;
    }
    
    public InventoryMenu setCloseOnAction(boolean state) {
        this.closeOnAction = state;
        return this;
    }
    
    public void setSave(Consumer<HashMap<Integer, InventoryMenu>> saveFun) {
        this.saveFun = saveFun;
    }
    public void saveEdit() {
        if (editting && saveFun != null) {
            saveFun.accept(items);
        }
    }
    
    public InventoryMenu setEditting(boolean state) {
        this.editting = state;
        if (state == true) {
            initControls();
        }
        return this;
    }
    public boolean isEditting() {
        return editting;
    }
    
    public void openInventory(CorePlayer cp) {
        if (size <= 0) return;
        
        String title = Chat.centerTitle(ChatColor.BLACK + "" + ChatColor.BOLD + titleFun.apply(cp));
        
        Inventory inv = Bukkit.createInventory(null, 9*size, title);
        
        for (Map.Entry<Integer, InventoryMenu> item : items.entrySet()) {
            if (cp.getRank().hasPermission(item.getValue().getRank()) &&
                    item.getKey() >= MENU_COUNT * cp.getPage() &&
                    item.getKey() < MENU_COUNT * (cp.getPage() + 1) &&
                    item.getValue().isVisible(cp)) {
                ItemStack itemStack = item.getValue().createMenuItem(cp);
                inv.setItem(item.getKey() - MENU_COUNT * cp.getPage(), itemStack);
            }
        }
        
        int i = 0;
        for (InventoryMenu item : unsortedItems) {
            if (!item.isVisible(cp)) continue;
            while (items.containsKey(i)
                    && items.get(i).isVisible(cp)) {
                i++;
            }
            if (i >= MENU_COUNT * cp.getPage()) {
                ItemStack itemStack = item.createMenuItem(cp);
                inv.setItem(i - MENU_COUNT * cp.getPage(), itemStack);
            }
            i++;
            if (i >= MENU_COUNT * (cp.getPage() + 1)) {
                break;
            }
        }
        
        for (InventoryMenuControl item : controlItems) {
            if (cp.getRank().hasPermission(item.inventoryMenu.getRank()) &&
                    item.inventoryMenu.isVisible(cp) &&
                    inv.getItem(item.slot) == null) {
                ItemStack itemStack = item.inventoryMenu.createMenuItem(cp);
                inv.setItem(item.slot, itemStack);
            }
        }
        
        cp.getPlayer().openInventory(inv);
    }
    public ItemStack createMenuItem(CorePlayer cp) {
        ItemStack item = displayItemFun.apply(cp);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(Chat.MENU_NAME + nameFun.apply(cp));
        //meta.addAttributeModifier(Attribute.BACKGROUND_COLOR, AttributeModifier.deserialize(map));
        if (descriptionFun != null) {
            meta.setLore(Chat.wrapDescription("\n" + descriptionFun.apply(cp)));
        } else {
            meta.setLore(null);
        }
        meta.getPersistentDataContainer().set(new NamespacedKey(Core.getInstance(), "menuitem"), PersistentDataType.STRING, this.hotbarName);
        meta.addItemFlags(ItemFlag.values());
        item.setItemMeta(meta);
        return item;
    }
    public boolean hasMenu() {
        return size > 0;
    }
    public boolean closeOnAction() {
        return closeOnAction;
    }
    
    public boolean isBackButton() {
        return backButton;
    }
    
    public InventoryMenu addBackButton(InventoryMenu prevMenu) {
        controlItems.add(new InventoryMenuControl(0 + (5 * 9), InventoryMenu.createItem()
                        .setName(ChatColor.RED + "" + ChatColor.BOLD + "Return")
                        .setDisplayItem(Material.DIAMOND_AXE, 9)
                        .setBackButton(true)
                        .setAction(cp -> cp.setInventoryMenu(prevMenu))));
        initControls();
        return this;
    }
    
    public void removeMenuItem(int slot) {
        items.remove(slot);
    }
    
    public void clear() {
        items.clear();
    }
    
    public InventoryMenu addMenuItem(InventoryMenu menuItem, int slot) {
        items.put(slot, menuItem);
        menuItem.addBackButton(this);
        return menuItem;
    }
    public InventoryMenu addMenuItem(InventoryMenu menuItem, int x, int y) {
        return addMenuItem(menuItem, x + (y * 9));
    }
    public InventoryMenu addMenuItem(InventoryMenu menuItem) {
        unsortedItems.add(menuItem);
        menuItem.addBackButton(this);
        return menuItem;
    }
    public InventoryMenu addStaticItem(InventoryMenu menuItem, int x, int y) {
        controlItems.add(new InventoryMenuControl(x + (y * 9), menuItem));
        menuItem.addBackButton(this);
        return menuItem;
    }
    
    public int getPageCount(CorePlayer cp) {
        int pageCount = 1;
        
        for (Map.Entry<Integer, InventoryMenu> item : items.entrySet()) {
            if (cp.getRank().hasPermission(item.getValue().getRank()) &&
                    item.getKey() >= MENU_COUNT * cp.getPage() &&
                    item.getKey() < MENU_COUNT * (cp.getPage() + 1) &&
                    item.getValue().isVisible(cp)) {
                pageCount = Math.max(pageCount, item.getKey() / MENU_COUNT + 1);
            }
        }
        
        int i = 0;
        for (InventoryMenu item : unsortedItems) {
            if (!item.isVisible(cp)) continue;
            while (items.containsKey(i)
                    && items.get(i).isVisible(cp)) {
                i++;
            }
            pageCount = Math.max(pageCount, i / MENU_COUNT + 1);
            i++;
        }
        
        return pageCount;
    }
    
    public InventoryMenu getMenuItem(CorePlayer cp, int slot) {
        for (InventoryMenuControl ci : controlItems) {
            if (ci.slot == slot &&
                    ci.inventoryMenu.isVisible(cp)) {
                return ci.inventoryMenu;
            }
        }
        
        InventoryMenu menuItem;
        if (slot < MENU_COUNT) {
            if ((menuItem = items.get(slot + (cp.getPage() * MENU_COUNT))) != null
                    && menuItem.isVisible(cp)) {
                return menuItem;
            }
            int i = 0;
            for (InventoryMenu item : unsortedItems) {
                if (!item.isVisible(cp)) continue;
                while (items.containsKey(i)
                        && items.get(i).isVisible(cp)) {
                    i++;
                }
                if (i - MENU_COUNT * cp.getPage() == slot) {
                    return item;
                }
                if (i - MENU_COUNT * cp.getPage() > slot) {
                    return null;
                }
                i++;
            }
        }
        
        return null;
    }
    public InventoryMenu getMenuItem(String name) {
        for (InventoryMenu i : items.values()) {
            if (i.getName().equalsIgnoreCase(name)) {
                return i;
            }
        }
        for (InventoryMenu i : unsortedItems) {
            if (i.getName().equalsIgnoreCase(name)) {
                return i;
            }
        }
        for (InventoryMenuControl i : controlItems) {
            if (i.inventoryMenu.getName().equalsIgnoreCase(name)) {
                return i.inventoryMenu;
            }
        }
        return null;
    }
    
    public void call(CorePlayer cp) {
        if (this.action != null) {
            action.accept(cp);
        }
    }
    
}
