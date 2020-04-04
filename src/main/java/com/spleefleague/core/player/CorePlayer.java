/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.spleefleague.core.player;

import com.spleefleague.core.util.database.Checkpoint;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.PlayerInfoData;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.comphenix.protocol.wrappers.WrappedGameProfile;
import com.google.common.collect.Lists;
import com.spleefleague.core.Core;
import com.spleefleague.core.annotation.DBField;
import com.spleefleague.core.annotation.DBLoad;
import com.spleefleague.core.annotation.DBSave;
import com.spleefleague.core.chat.Chat;
import com.spleefleague.core.chat.ChatChannel;
import com.spleefleague.core.command.CommandTemplate;
import com.spleefleague.core.infraction.Infraction;
import com.spleefleague.core.io.converter.LocationConverter;
import com.spleefleague.core.menu.InventoryMenu;
import com.spleefleague.core.party.Party;
import com.spleefleague.core.player.cosmetics.CosmeticArmor;
import com.spleefleague.core.plugin.CorePlugin;
import com.spleefleague.core.request.Request;
import com.spleefleague.core.util.TpVector;
import com.spleefleague.core.util.database.DBPlayer;
import com.spleefleague.core.vendor.KeyItem;
import com.spleefleague.core.vendor.VendorItem;
import java.lang.reflect.InvocationTargetException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bson.Document;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scoreboard.Team;

/**
 * @author NickM13
 */
public class CorePlayer extends DBPlayer {
    
    private static Map<UUID, PermissionAttachment> perms = new HashMap<>();
    
    /**
     * Database variables
     */
    
    @DBField(serializer=LocationConverter.class)
    private Location lastLocation;
    @DBField
    private Checkpoint checkpoint;
    
    private Rank rank;
    private final List<TempRank> tempRanks;
    
    @DBField
    protected Map<String, Integer> scores = new HashMap<>();
    
    @DBField
    private Boolean vanished;
    
    @DBField
    private Integer coins;
    @DBField
    private Set<String> keys = new HashSet<>();
    
    private Map<Integer, Request> requests = new HashMap<>();

    // Options
    private final Set<String> disabledChannels;
    
    @DBField
    private String gameMode = GameMode.SURVIVAL.name();
    
    /**
     * Non-database variables
     */
    
    private Party party;
    
    private long urlTime;
    
    private int inventoryPage;
    private InventoryMenu inventoryMenu;
    private ChatChannel chatChannel;
    
    // 5 min, sets player to afk
    private static final long AFK_TIMEOUT = 1000L * 60 * 5;
    private long lastAction;
    private boolean afk;
    
    private static final VendorItem DEFAULT_HELD_ITEM = new VendorItem("none", Material.BAKED_POTATO, 0, "None", "");
    private VendorItem heldItem = DEFAULT_HELD_ITEM;
    // Used for cosmetic reasons
    private Map<String, String> selectedItems = new HashMap<>();
    
    private final Set<CosmeticArmor> activeCosmetics = new HashSet<>();
    
    private Player replyPlayer = null;
    
    public CorePlayer() {
        super();
        chatChannel = ChatChannel.getDefaultChannel();
        lastLocation = null;
        checkpoint = null;
        tempRanks = new ArrayList<>();
        disabledChannels = new HashSet<>();
        vanished = false;
        party = null;
        coins = 0;
        lastAction = System.currentTimeMillis();
        afk = false;
    }
    
    @Override
    public void init() {
        if (!perms.containsKey(getPlayer().getUniqueId())) {
            PermissionAttachment attachment = getPlayer().addAttachment(Core.getInstance());
            perms.put(getPlayer().getUniqueId(), attachment);
        }
        setRank(rank);
        online = true;
        updateArmor();
    }
    
    @DBSave(fieldname="heldItem")
    protected Document saveHeldItem() {
        if (heldItem == DEFAULT_HELD_ITEM) {
            return null;
        }
        return new Document("type", heldItem.getType()).append("identifier", heldItem.getIdentifier());
    }
    
    @DBLoad(fieldname="heldItem")
    protected void loadHeldItem(Document doc) {
        if (doc != null) {
            Bukkit.getScheduler().runTaskLater(Core.getInstance(), () -> {
                VendorItem vendorItem = VendorItem.getVendorItem(doc.get("type", String.class), doc.get("identifier", String.class));
                if (vendorItem != null)
                    this.heldItem = vendorItem;
                refreshHotbar();
            }, 20L);
        }
    }
    
    @Override
    public void setGameMode(GameMode gameMode) {
        this.gameMode = gameMode.name();
        getPlayer().setGameMode(gameMode);
    }
    public GameMode getGameMode() {
        return GameMode.valueOf(gameMode);
    }
    
    public void checkAfk() {
        if (lastAction + AFK_TIMEOUT < System.currentTimeMillis()) {
            setAfk(true);
        }
    }
    public void setLastAction() {
        lastAction = System.currentTimeMillis();
        setAfk(false);
    }
    public void setAfk(boolean state) {
        if (afk != state) {
            if (state == true) {
                getPlayer().getInventory().setItemInOffHand(InventoryMenu.createItem("AFK", Material.DIAMOND_HOE, 253));
            } else {
                getPlayer().getInventory().setItemInOffHand(null);
            }
            afk = state;
            Core.sendMessageToPlayer(this, "You are " + (afk ? "now afk" : "no longer afk"));
        }
    }
    public boolean isAfk() {
        return afk;
    }
    
    public void setSelectedItem(String type, String id) {
        if (selectedItems.containsKey(type) &&
                heldItem == VendorItem.getVendorItem(type, selectedItems.get(type))) {
            heldItem = VendorItem.getVendorItem(type, id);
        }
        selectedItems.put(type, id);
        refreshHotbar();
    }
    
    public void addKey(KeyItem key) {
        keys.add(key.getIdentifier());
        Core.getInstance().sendMessage(this, "You have received " + key.getDisplayName());
    }
    public void removeKey(KeyItem key) {
        keys.remove(key.getIdentifier());
        Core.getInstance().sendMessage(this, "You have lost " + key.getDisplayName());
    }
    
    public void setHeldItem(VendorItem item) {
        heldItem = item;
        refreshHotbar();
    }
    public VendorItem getHeldItem() {
        return heldItem;
    }
    
    private static InventoryMenu createActiveHeldMenuItem() {
        return InventoryMenu.createItem()
                .setName(cp -> {
                    return cp.getHeldItem().getDisplayName();
                }).setDescription(cp -> {
                    return cp.getHeldItem().getDescription();
                }).setDisplayItem(cp -> {
                    return cp.getHeldItem().getItem();
                }).setCloseOnAction(false);
    }
    public void openHeldItem() {
        InventoryMenu menu = InventoryMenu.createMenu()
                .setTitle("Held Item");
        menu.addMenuItem(InventoryMenu.createItem()
                .setName(DEFAULT_HELD_ITEM.getDisplayName())
                .setDescription(DEFAULT_HELD_ITEM.getDescription())
                .setDisplayItem(DEFAULT_HELD_ITEM.getItem())
                .setAction(cp -> {
                    cp.setHeldItem(DEFAULT_HELD_ITEM);
                })
                .setCloseOnAction(false));
        for (String type : VendorItem.getItemTypes()) {
            if (type.equalsIgnoreCase("key")) continue;
            if (selectedItems.containsKey(type)) {
                VendorItem item = VendorItem.getVendorItem(type, selectedItems.get(type));
                if (item != null) {
                    menu.addMenuItem(InventoryMenu.createItem()
                            .setName(item.getDisplayName())
                            .setDescription(item.getDescription())
                            .setDisplayItem(item.getItem())
                            .setAction(cp -> {
                                cp.setHeldItem(item);
                            })
                            .setCloseOnAction(false));
                }
            }
        }
        for (String key : keys) {
            KeyItem item = KeyItem.getKeyItem(key);
            if (item != null) {
                menu.addMenuItem(InventoryMenu.createItem()
                        .setName(item.getDisplayName())
                        .setDescription(item.getDescription())
                        .setDisplayItem(item.getItem())
                        .setAction(cp -> {
                            cp.setHeldItem(item);
                        })
                        .setCloseOnAction(false));
            }
        }
        menu.addStaticItem(createActiveHeldMenuItem(), 4, 5);
        setInventoryMenu(menu);
    }
    
    @Override
    public void close() {
        Core.getInstance().unqueuePlayerGlobally(this);
        
        Team team = getPlayer().getScoreboard().getEntryTeam(getPlayer().getName());
        if (team != null) team.removeEntry(getPlayer().getName());
        online = false;
        Document leaveDoc = new Document("date", Date.from(Instant.now()));
        leaveDoc.append("type", "LEAVE");
        leaveDoc.append("uuid", getPlayer().getUniqueId().toString());

        try {
            Core.getInstance().getPluginDB().getCollection("PlayerConnections").insertOne(leaveDoc);
        } catch (NoClassDefFoundError e) {
            
        }
    }
    
    public void setVanished(boolean vanished) {
        this.vanished = vanished;
    }
    
    @Override
    public boolean isVanished() {
        return vanished;
    }
    
    public void joinParty(Party party) {
        this.party = party;
    }
    
    public void leaveParty() {
        if (party != null) {
            party = null;
        }
    }
    
    public Party getParty() {
        return party;
    }
    
    @Override
    protected void newPlayer() {
        rank = Rank.getDefaultRank();
    }
    
    public boolean canSendUrl() {
        if (urlTime > System.currentTimeMillis()) {
            urlTime = 0;
            return true;
        }
        return false;
    }
    public void allowUrl() {
        urlTime = System.currentTimeMillis() + 30 * 1000;
    }
    
    public int getCoins() {
        return coins;
    }
    public void setCoins(int coins) {
        this.coins = coins;
    }
    public void addCoins(int coins) {
        this.coins += coins;
    }
    
    public void updateArmor() {
        PlayerInventory inventory = getPlayer().getInventory();
        ItemStack helm, chest, legs, boots;
        helm = inventory.getHelmet();
        chest = inventory.getChestplate();
        legs = inventory.getLeggings();
        boots = inventory.getBoots();
        
        activeCosmetics.clear();
        
        CosmeticArmor cosmetic;
        if (helm != null && (cosmetic = CosmeticArmor.getArmor(helm)) != null) {
            activeCosmetics.add(cosmetic);
        }
        if (chest != null && (cosmetic = CosmeticArmor.getArmor(chest)) != null) {
            activeCosmetics.add(cosmetic);
        }
        if (legs != null && (cosmetic = CosmeticArmor.getArmor(legs)) != null) {
            activeCosmetics.add(cosmetic);
        }
        if (boots != null && (cosmetic = CosmeticArmor.getArmor(boots)) != null) {
            activeCosmetics.add(cosmetic);
        }
    }
    public void updateArmorEffects() {
        if (!CorePlugin.isInBattleGlobal(getPlayer())) {
            for (CosmeticArmor armor : activeCosmetics) {
                getPlayer().addPotionEffect(new PotionEffect(armor.getEffectType(), 39, armor.getAmplitude(), true, false), true);
            }
        }
    }
    
    @Override
    public String getDisplayName() {
        return getRank().getColor() + this.getName() + Chat.DEFAULT;
    }
    
    public TextComponent getChatName() {
        TextComponent text = new TextComponent(getName());
        
        text.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("Click to send a message").create()));
        text.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/tell " + getName()));
        text.setColor(getRank().getColor().asBungee());
        
        return text;
    }
    
    @DBSave(fieldname="tempRanks")
    protected List<Document> saveTempRanks() {
        List<Document> docs = new ArrayList<>();
        
        for (TempRank tr : tempRanks) {
            docs.add(new Document("rank", tr.rank.getName()).append("expireTime", tr.expireTime));
        }
        
        return docs;
    }
    @DBLoad(fieldname="tempRanks")
    protected void loadTempRanks(List<Document> ranks) {
        for (Document d : ranks) {
            tempRanks.add(new TempRank(d.get("rank", String.class), d.get("expireTime", Long.class)));
        }
    }
    @DBSave(fieldname="disabledChats")
    protected List<String> saveDisabledChatChannels() {
        return Lists.newArrayList(disabledChannels);
    }
    @DBLoad(fieldname="disabledChats")
    protected void loadDisabledChatChannels(List<String> channels) {
        for (String c : channels) {
            disabledChannels.add(c.toLowerCase());
        }
    }
    
    public void toggleDisabledChannel(String channel) {
        channel = channel.toLowerCase();
        if (disabledChannels.contains(channel)) {
            disabledChannels.remove(channel);
        } else {
            disabledChannels.add(channel);
        }
    }
    public boolean isChannelDisabled(String channel) {
        return disabledChannels.contains(channel.toLowerCase());
    }

    public void invsee(CorePlayer target) {
        getPlayer().openInventory(target.getPlayer().getInventory());
    }
    public void invcopy(CorePlayer target) {
        loadPregameState();
        pregameState.save(PregameState.PSFlag.INVENTORY);
        getPlayer().getInventory().setContents(target.getInventory());
    }

    public boolean canBuild() {
        return (rank.hasPermission(Rank.BUILDER) && !getPlayer().getGameMode().equals(GameMode.SURVIVAL) && !CorePlugin.isInBattleGlobal(getPlayer()));
    }

    @DBLoad(fieldname="rank")
    protected void loadRank(String str) {
        rank = Rank.getRank(str);
    }
    @DBSave(fieldname="rank")
    protected String saveRank() {
        return rank.getName();
    }

    public void updateRank() {
        Iterator<TempRank> it = tempRanks.iterator();
        while (it.hasNext()) {
            TempRank tr = it.next();
            if (tr.expireTime < System.currentTimeMillis()) {
                it.remove();
            }
        }
        
        Team team;
        if ((team = Bukkit.getScoreboardManager().getMainScoreboard().getEntryTeam(username)) != null) {
            team.removeEntry(username);
        }
        Bukkit.getScoreboardManager().getMainScoreboard().getTeam(getRank().getNameShort()).addEntry(username);
        getPlayer().setOp(getRank().getHasOp());
        
        PacketContainer packet = new PacketContainer(PacketType.Play.Server.PLAYER_INFO);
        packet.getPlayerInfoAction().write(0, EnumWrappers.PlayerInfoAction.UPDATE_DISPLAY_NAME);
        packet.getPlayerInfoDataLists().write(0, Lists.newArrayList(new PlayerInfoData(
                WrappedGameProfile.fromPlayer(getPlayer()),
                1,
                EnumWrappers.NativeGameMode.fromBukkit(getPlayer().getGameMode()),
                WrappedChatComponent.fromText(getDisplayName()))));
        Core.sendPacketAll(packet);
        
        PermissionAttachment pperms = perms.get(getPlayer().getUniqueId());
        pperms.getPermissions().clear();
        
        for (String p : CommandTemplate.getAllPermissions()) {
            boolean has = false;
            if (rank.hasPermission(p)) {
                has = true;
            } else {
                for (TempRank tr : tempRanks) {
                    if (tr.rank.hasExclusivePermission(p)) {
                        has = true;
                        break;
                    }
                }
            }
            pperms.setPermission(p, has);
        }
    }
    public void setRank(Rank rank) {
        this.rank = rank;
        
        updateRank();
    }
    public boolean addTempRank(String rank, Integer hours) {
        TempRank tempRank = new TempRank(rank, hours * 60L * 60L * 1000L + System.currentTimeMillis());
        if (tempRank.rank != null) {
            Core.getInstance().sendMessage(this, "Added temp rank " + rank + " for " + hours + " hours");
            tempRanks.add(tempRank);
            updateRank();
            return true;
        }
        return false;
    }
    public void clearTempRank() {
        Iterator<TempRank> it = tempRanks.iterator();
        while (it.hasNext()) {
            TempRank tr = it.next();
            it.remove();
        }
        updateRank();
    }
    public Rank getPermanentRank() {
        return rank;
    }
    public Rank getRank() {
        if (tempRanks.size() > 0) {
            TempRank highestRank = null;
            Iterator<TempRank> it = tempRanks.iterator();
            while (it.hasNext()) {
                TempRank tr = it.next();
                if (highestRank == null ||
                        tr.rank.getLadder() > highestRank.rank.getLadder()) {
                    highestRank = tr;
                }
            }
            if (highestRank != null) {
                return highestRank.rank;
            }
        }
        return rank;
    }
    
    public boolean hasScore(String name) {
        return scores.containsKey(name);
    }
    public Integer getScore(String name) {
        if (!scores.containsKey(name)) return 0;
        return scores.get(name);
    }
    public void setScore(String name, int score) {
        scores.put(name, score);
    }
    public void checkScore(String name, int score) {
        if (scores.containsKey(name) &&
                scores.get(name) < score) {
            scores.put(name, score);
        }
    }

    public Location getLocation() {
        return getPlayer().getLocation();
    }

    public void teleport(Location loc) {
        lastLocation = getPlayer().getLocation();
        getPlayer().teleport(loc, PlayerTeleportEvent.TeleportCause.COMMAND);
    }
    public void teleport(double x, double y, double z) {
        Location loc = new Location(getLocation().getWorld(), x, y, z, getLocation().getYaw(), getLocation().getPitch());
        teleport(loc);
    }
    public void teleport(TpVector tpVector) {
        Location loc = getLocation().clone();
        
        tpVector.apply(loc);
        
        teleport(loc);
    }
    
    public void refreshHotbar() {
        getPlayer().getInventory().clear();
        getPlayer().getInventory().setItem(0, InventoryMenu.getHotbarMenu(InventoryMenu.InvMenuType.SLMENU).createMenuItem(this));
        getPlayer().getInventory().setItem(8, InventoryMenu.getHotbarMenu(InventoryMenu.InvMenuType.HELD).createMenuItem(this));
    }
    public void gotoSpawn() {
        if (!getPlayer().getGameMode().equals(GameMode.CREATIVE)) {
            refreshHotbar();
        }
        teleport(Core.DEFAULT_WORLD.getSpawnLocation().clone().add(0, 0, 0));
    }
    
    public void setCheckpoint(String warp, int duration) {
        checkpoint = new Checkpoint(warp, duration);
    }
    
    public void checkpoint() {
        if (checkpoint != null && checkpoint.isActive()) {
            teleport(checkpoint.getLocation());
        }
    }

    public Location getLastLocation() {
        return lastLocation;
    }
    public Checkpoint getCheckpoint() {
        return checkpoint;
    }
    
    public void setReply(Player player) {
        replyPlayer = player;
    }
    public Player getReply() {
        return replyPlayer;
    }

    public void setChatChannel(ChatChannel cc) {
        chatChannel = cc;
        Chat.sendMessageToPlayer(this, "Chat Channel set to " + cc.getName());
    }

    public ChatChannel getChatChannel() {
        return chatChannel;
    }

    public void sendMessage(String string) {
        getPlayer().sendMessage(string);
    }
    
    public void sendMessage(BaseComponent message) {
        getPlayer().spigot().sendMessage(message);
    }

    public void sendTitle(String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        getPlayer().sendTitle(title, subtitle, fadeIn, stay, fadeOut);
    }

    public void setInventoryMenu(InventoryMenu inventoryMenu) {
        if (inventoryMenu != null) {
            if (inventoryMenu.isBackButton()) {
                return;
            }
            if (this.inventoryMenu == null || !this.inventoryMenu.getName().equals(inventoryMenu.getName())) {
                inventoryPage = 0;
            }
            if (inventoryMenu.hasMenu()) {
                ItemStack item = getPlayer().getItemOnCursor();
                getPlayer().setItemOnCursor(null);
                inventoryMenu.openInventory(this);
                getPlayer().setItemOnCursor(item);
            } else {
                getPlayer().closeInventory();
            }
        } else {
            getPlayer().closeInventory();
        }
        this.inventoryMenu = inventoryMenu;
    }
    public void closeInventoryMenu() {
        inventoryMenu = null;
    }

    public InventoryMenu getInventoryMenu() {
        return inventoryMenu;
    }
    
    public int getPage() {
        return inventoryPage;
    }
    public void nextPage() {
        inventoryPage++;
    }
    public void prevPage() {
        inventoryPage--;
    }
    public void savePage() {
        inventoryMenu.saveEdit();
    }
    
    public int getPing() {
        try {
            Object entityPlayer = getPlayer().getClass().getMethod("getHandle").invoke(getPlayer());
            return (int) entityPlayer.getClass().getField("ping").get(entityPlayer);
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException | NoSuchFieldException e) {
            return -1;
        }
    }
    
    public String getPingFormatted() {
        int ping = getPing();
        String str = "";
        
        if (ping <= 30)          str += ChatColor.GREEN;
        else if (ping <= 100)    str += ChatColor.DARK_GREEN;
        else if (ping <= 250)    str += ChatColor.GOLD;
        else                    str += ChatColor.RED;
        
        str += ping + "ms";
        return str;
    }

    public Infraction checkBan() {
        Infraction latestban = null;

        for (Infraction i : Infraction.getAll(getUniqueId())) {
            if (i.getType().equals(Infraction.Type.BAN)) {
                if (latestban == null
                        || latestban.getExpireTime() < i.getExpireTime()) {
                    latestban = i;
                }
            } else if (i.getType().equals(Infraction.Type.TEMPBAN)) {
                if (latestban == null
                        || latestban.getExpireTime() < i.getExpireTime()) {
                    latestban = i;
                }
            }
        }

        if (latestban == null) {
            return null;
        }

        for (Infraction i : Infraction.getAll(getUniqueId())) {
            if (i.getType().equals(Infraction.Type.UNBAN)) {
                if (i.getExpireTime() > latestban.getExpireTime()) {
                    return null;
                }
            }
        }

        return latestban;
    }
    
}
