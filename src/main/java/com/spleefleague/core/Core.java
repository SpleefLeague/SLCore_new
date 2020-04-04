package com.spleefleague.core;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.PlayerInfoData;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.google.common.collect.Lists;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.spleefleague.core.chat.Chat;
import com.spleefleague.core.chat.ChatChannel;
import com.spleefleague.core.chat.ChatGroup;
import com.spleefleague.core.chat.ticket.Ticket;
import com.spleefleague.core.chat.ticket.TicketManager;
import com.spleefleague.core.command.CommandManager;
import com.spleefleague.core.command.CommandTemplate;
import com.spleefleague.core.game.Leaderboard;
import com.spleefleague.core.infraction.Infraction;
import com.spleefleague.core.listener.*;
import com.spleefleague.core.menu.AnvilMenu;
import com.spleefleague.core.menu.InventoryMenu;
import com.spleefleague.core.party.Party;
import com.spleefleague.core.player.CorePlayer;
import com.spleefleague.core.player.PlayerManager;
import com.spleefleague.core.player.Rank;
import com.spleefleague.core.plugin.CorePlugin;
import com.spleefleague.core.queue.PlayerQueue;
import com.spleefleague.core.queue.QueueManager;
import com.spleefleague.core.queue.QueueRunnable;
import com.spleefleague.core.util.Warp;
import com.spleefleague.core.util.database.DBPlayer;
import com.spleefleague.core.vendor.KeyItem;
import com.spleefleague.core.vendor.Vendor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.minecraft.server.v1_15_R1.EntityPlayer;
import org.bson.Document;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.v1_15_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.reflections.Reflections;

/**
 * @author NickM13
 */
public class Core extends CorePlugin<CorePlayer> {

    public static World DEFAULT_WORLD;

    private static Core instance;

    private MongoClient mongoClient;

    private QueueManager queueManager;
    private QueueRunnable qrunnable;
    
    private Set<CorePlugin> plugins;

    // Command manager, contains list of all commands
    // and registers them to the server
    private CommandManager commandManager;

    // Manager for tickets (issues) players are having
    private TicketManager ticketManager;
    
    // For packet managing
    private static ProtocolManager protocolManager;

    // For globally stopping threads
    public boolean running;

    public Core() {
        
    }

    @Override
    public void init() {
        instance = this;
        DEFAULT_WORLD = Bukkit.getWorlds().get(0);
        protocolManager = ProtocolLibrary.getProtocolManager();
        plugins = new HashSet<>();
        plugins.add(this);
        
        initMongo();
        Rank.init();
        Chat.init();
        Warp.init();
        Infraction.init();
        KeyItem.init();
        Vendor.init();
        InventoryMenu.init();
        
        // Initialize listeners
        ConnectionListener.init();
        GameListener.init();
        ChatListener.init();
        MenuListener.init();

        // Initialize manager
        playerManager = new PlayerManager<>(this, CorePlayer.class, getPluginDB().getCollection("Players"));
        commandManager = new CommandManager();
        ticketManager = new TicketManager();

        // Initialize various things
        initQueues();
        initCommands();
        initMenu();
        initTabList();
        
        playerManager.initOnline();
        
        Leaderboard.init();
        
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            for (CorePlayer cp : getPlayers().getAll()) {
                cp.updateArmorEffects();
            }
        }, 15L, 15L);
        
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            for (CorePlayer cp : getPlayers().getAll()) {
                cp.checkAfk();
                cp.updateRank();
            }
        }, 200L, 200L);

        running = true;
    }

    @Override
    public void close() {
        if (running) {
            Warp.close();
            Infraction.close();
            KeyItem.close();
            Vendor.close();
            Leaderboard.close();
            qrunnable.close();
            playerManager.close();
            try {
                mongoClient.close();
            } catch (NoClassDefFoundError e) {
                System.out.println("Jar files updated, unable to close MongoDB");
            }
            running = false;
            ProtocolLibrary.getPlugin().onDisable();
        }
    }

    public static Core getInstance() {
        return instance;
    }

    private void initMongo() {
        // Disable mongodb logging
        System.setProperty("DEBUG.MONGO", "false");
        System.setProperty("DB.TRACE", "false");
        Logger.getLogger("org.mongodb").setLevel(Level.OFF);
        try {
            // Test server connection driver, from MongoDB Atlas (free)
            // TODO: Change this when adding to live server !!
            MongoClientURI uri = new MongoClientURI(
                    "mongodb://nickm13:MeadNick0313@spleefleague-shard-00-00-foua3.mongodb.net:27017,spleefleague-shard-00-01-foua3.mongodb.net:27017,spleefleague-shard-00-02-foua3.mongodb.net:27017/test?ssl=true&replicaSet=SpleefLeague-shard-0&authSource=admin&retryWrites=true&w=majority");
            mongoClient = new MongoClient(uri);
        } catch (Exception ex) {}
    }

    private void initCommands() {
        Reflections reflections = new Reflections("com.spleefleague.core.commands");
        
        Set<Class<? extends CommandTemplate>> subTypes = reflections.getSubTypesOf(CommandTemplate.class);
        
        subTypes.forEach(st -> {
            try {
                addCommand(st.newInstance());
            } catch (InstantiationException | IllegalAccessException ex) {
                Logger.getLogger(Core.class.getName()).log(Level.SEVERE, null, ex);
            }
        });
        
        commandManager.flushRegisters();
    }
    
    public void returnToWorld(CorePlayer cp1) {
        if (cp1.isVanished())
            cp1.getPlayer().hidePlayer(Core.getInstance(), cp1.getPlayer());
        else
            cp1.getPlayer().showPlayer(Core.getInstance(), cp1.getPlayer());
        // See and become visible to all players outside of games
        // getOnline doesn't return vanished players
        for (CorePlayer cp2 : getPlayers().getAll()) {
            if (!cp1.equals(cp2)) {
                if ((CorePlugin.getBattleGlobal(cp1.getPlayer()) == CorePlugin.getBattleGlobal(cp2.getPlayer()))) {
                    cp1.getPlayer().showPlayer(this, cp2.getPlayer());
                    if (!cp1.isVanished()) cp2.getPlayer().showPlayer(this, cp1.getPlayer());
                    else                   cp2.getPlayer().hidePlayer(this, cp1.getPlayer());
                } else {
                    cp1.getPlayer().hidePlayer(this, cp2.getPlayer());
                    cp2.getPlayer().hidePlayer(this, cp1.getPlayer());
                }
            }
        }
    }
    
    public void initMenu() {
        // Profile Menus
        InventoryMenu profileMenu = InventoryMenu.createMenu()
                .setTitle("Profile Menu")
                .setName(ChatColor.BLUE + "" + ChatColor.BOLD + "Profile")
                .setDisplayItem((cp) -> {
                    ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
                    SkullMeta meta = (SkullMeta) skull.getItemMeta();
                    meta.setOwningPlayer(cp.getPlayer());
                    skull.setItemMeta(meta);
                    return skull;
                })
                .setDescription(cp -> ("View statistics on your player character"));

        profileMenu.addMenuItem(InventoryMenu.createItem()
                .setName("Character Information")
                .setDisplayItem(new ItemStack(Material.CHEST))
                .setAction(cp -> {

                }));

        InventoryMenu.getHotbarMenu(InventoryMenu.InvMenuType.SLMENU).addMenuItem(profileMenu, 7);

        // Moderative Menus
        InventoryMenu moderativeMenu = InventoryMenu.createMenu()
                .setTitle("Moderative Tools")
                .setName("Moderative Tools")
                .setRank(Rank.MODERATOR)
                .setDisplayItem(new ItemStack(Material.REDSTONE))
                .setDescription("Useful tools to deal with the general population");

        moderativeMenu.addMenuItem(AnvilMenu.create()
                .setAction((cp, s) -> {
                    Core.getInstance().warn(cp.getName(), Bukkit.getOfflinePlayer(s), "AnvilGUI");
                })
                .setTitle("Warn Player")
                .setName("Warn Player")
                .setDisplayItem(new ItemStack(Material.GOLDEN_APPLE)));

        InventoryMenu.getHotbarMenu(InventoryMenu.InvMenuType.SLMENU).addMenuItem(moderativeMenu, 8);

        // Cosmetic Menus
        InventoryMenu cosmeticMenu = InventoryMenu.createMenu()
                .setTitle("Leaderboard")
                .setName("Leaderboard")
                .setDisplayItem(new ItemStack(Material.DARK_OAK_SIGN))
                .setDescription("The top list of the best players on SpleefLeague");

        InventoryMenu.getHotbarMenu(InventoryMenu.InvMenuType.SLMENU).addMenuItem(cosmeticMenu, 6);
        
        // Options Menus
        InventoryMenu optionsMenu = InventoryMenu.createMenu()
                .setTitle("Options")
                .setName("Options")
                .setDisplayItem(new ItemStack(Material.WRITABLE_BOOK))
                .setDescription("Customize your SpleefLeague experience");
        
        // Chat Options Menus
        InventoryMenu chatOptionsMenu = InventoryMenu.createMenu()
                .setTitle("Chat Channels")
                .setName("Chat Channels")
                .setDisplayItem(new ItemStack(Material.WRITABLE_BOOK))
                .setDescription("Toggle Chat Channels");
        
        for (ChatChannel.Channel channel : ChatChannel.Channel.values()) {
            chatOptionsMenu.addMenuItem(InventoryMenu.createItem()
                    .setName(ChatChannel.getChannel(channel).getName())
                    .setDescription(cp -> { return "This chat is "
                            + (cp.isChannelDisabled(channel.toString()) ? (ChatColor.RED + "Disabled") : (ChatColor.GREEN + "Enabled")); })
                    .setDisplayItem(cp -> { return new ItemStack( cp.isChannelDisabled(channel.toString()) ? Material.BOOK : Material.WRITABLE_BOOK); })
                    .setAction(cp -> { cp.toggleDisabledChannel(channel.toString()); })
                    .setCloseOnAction(false)
                    .setVisibility(cp -> ChatChannel.getChannel(channel).isAvailable(cp)));
        }
        
        optionsMenu.addMenuItem(chatOptionsMenu);

        InventoryMenu.getHotbarMenu(InventoryMenu.InvMenuType.SLMENU).addMenuItem(optionsMenu, 5);
        
        // Exit Button
        InventoryMenu.getHotbarMenu(InventoryMenu.InvMenuType.SLMENU).addMenuItem(InventoryMenu.createItem()
                .setName("Exit Menu")
                .setDescription("Click here to close out of this menu")
                .setDisplayItem(Material.REDSTONE_BLOCK), 8);
    }
    
    public void initTabList() {
        Core.getProtocolManager().addPacketListener(new PacketAdapter(Core.getInstance(), 
                PacketType.Play.Server.PLAYER_INFO,
                PacketType.Play.Server.NAMED_ENTITY_SPAWN,
                PacketType.Play.Server.ENTITY_DESTROY) {
            @Override
            public void onPacketSending(PacketEvent pe) {
                if (pe.getPacketType() == PacketType.Play.Server.PLAYER_INFO) {
                    PacketContainer packet = pe.getPacket();
                    CorePlayer cp;
                    switch (packet.getPlayerInfoAction().read(0)) {
                        case ADD_PLAYER:
                            cp = Core.getInstance().getPlayers().get(packet.getPlayerInfoDataLists().read(0).get(0).getProfile().getUUID());
                            if (!cp.isVanished()) {
                                packet.getPlayerInfoDataLists().write(0, Lists.newArrayList(new PlayerInfoData(
                                        packet.getPlayerInfoDataLists().read(0).get(0).getProfile(),
                                        packet.getPlayerInfoDataLists().read(0).get(0).getLatency(),
                                        packet.getPlayerInfoDataLists().read(0).get(0).getGameMode(),
                                        WrappedChatComponent.fromText(cp.getDisplayName()))));
                            } else {
                                pe.setCancelled(true);
                            }
                            break;
                        case REMOVE_PLAYER:
                            cp = Core.getInstance().getPlayers().get(packet.getPlayerInfoDataLists().read(0).get(0).getProfile().getUUID());
                            if (cp != null && cp.isOnline() && !cp.isVanished()) {
                                pe.setCancelled(true);
                            }
                            break;
                        case UPDATE_DISPLAY_NAME:
                            break;
                        case UPDATE_GAME_MODE:
                            break;
                        case UPDATE_LATENCY:
                            break;
                        default: break;
                    }
                } else if (pe.getPacketType() == PacketType.Play.Server.NAMED_ENTITY_SPAWN) {
                    PacketContainer packet = pe.getPacket();
                } else if (pe.getPacketType() == PacketType.Play.Server.ENTITY_DESTROY) {
                    /*
                    PacketContainer packet = pe.getPacket();
                    switch (packet.getPlayerInfoAction().read(0)) {
                        case ADD_PLAYER:
                            break;
                        case REMOVE_PLAYER:
                            break;
                        case UPDATE_DISPLAY_NAME:
                            break;
                        case UPDATE_GAME_MODE:
                            break;
                        case UPDATE_LATENCY:
                            break;
                        default: break;
                    }
                    */
                    //pe.setCancelled(true);
                }
            }
        });
    }

    public MongoClient getMongoClient() {
        return mongoClient;
    }

    @Override
    public MongoDatabase getPluginDB() {
        return mongoClient.getDatabase("SpleefLeague");
    }
    
    public static ProtocolManager getProtocolManager() {
        return protocolManager;
    }
    public static void sendPacket(List<Player> players, PacketContainer packet) {
        for (Player p : players) {
            sendPacket(p, packet);
        }
    }
    public static void sendPacket(Player p, PacketContainer packet) {
        Bukkit.getScheduler().runTaskLater(Core.getInstance(), () -> {
            try {
                protocolManager.sendServerPacket(p, packet);
            } catch (InvocationTargetException ex) {
                Logger.getLogger(Core.class.getName()).log(Level.SEVERE, null, ex);
            }
        }, 1L);
    }
    public static void sendPacketAll(PacketContainer packet) {
        for (CorePlayer cp : Core.getInstance().getPlayers().getAll()) {
            sendPacket(cp.getPlayer(), packet);
        }
    }
    public static void sendPacketAllExcept(CorePlayer cp, PacketContainer packet) {
        for (CorePlayer cp1 : Core.getInstance().getPlayers().getAll()) {
            if (!cp.equals(cp1)) {
                sendPacket(cp1.getPlayer(), packet);
            }
        }
    }

    private void initQueues() {
        queueManager = new QueueManager();
        queueManager.initialize();

        qrunnable = new QueueRunnable();
        Bukkit.getScheduler().scheduleSyncRepeatingTask(getInstance(), qrunnable, 20L, 20L);
    }

    public void addQueue(PlayerQueue queue) {
        queueManager.addQueue(queue);
    }

    public void removeQueue(PlayerQueue queue) {
        queueManager.removeQueue(queue);
    }

    public void unqueuePlayerGlobally(CorePlayer player) {
        for (PlayerQueue pq : queueManager.getQueues()) {
            pq.unqueuePlayer(player);
        }
    }
    
    public boolean unqueuePartyGlobally(Party party) {
        boolean unqueued = false;
        for (PlayerQueue pq : queueManager.getQueues()) {
            if (pq.isTeamQueue()) {
                unqueued = pq.unqueuePlayer(party.getOwner());
            }
        }
        return unqueued;
    }

    public QueueManager getQueueManager() {
        return queueManager;
    }
    
    public String getUsername(UUID uuid) {
        CorePlayer cp;
        if ((cp = playerManager.get(uuid)) != null) {
            return cp.getDisplayName();
        } else {
            MongoCollection<Document> collection = getPluginDB().getCollection("Players");
            Document doc = collection.find(new Document("uuid", uuid.toString())).first();
            return doc.get("username", String.class);
        }
    }
    public UUID getUniqueId(String name) {
        CorePlayer cp;
        if ((cp = playerManager.get(name)) != null) {
            return cp.getPlayer().getUniqueId();
        }
        return UUID.randomUUID();
    }

    public PlayerManager getPlayerManager() {
        return playerManager;
    }
    
    public List<CorePlayer> getPlayersInRadius(Location loc, Double minDist, Double maxDist) {
        List<CorePlayer> cpList = new ArrayList<>();
        
        for (CorePlayer cp1 : playerManager.getAll()) {
            if (loc.getWorld().equals(cp1.getLocation().getWorld()) &&
                    loc.distance(cp1.getLocation()) >= minDist &&
                    loc.distance(cp1.getLocation()) <= maxDist) {
                for (int i = 0; i < cpList.size(); i++) {
                    CorePlayer cp2 = cpList.get(i);
                    if (loc.distance(cp1.getLocation()) < loc.distance(cp2.getLocation())) {
                        cpList.add(i, cp1);
                        break;
                    }
                    if (i == cpList.size() - 1) {
                        cpList.add(cp1);
                    }
                }
            }
        }
        
        return cpList;
    }

    public void addCommand(CommandTemplate command) {
        commandManager.addCommand(command);
    }

    public void flushCommands() {
        commandManager.flushRegisters();
    }

    public void openTicket(CorePlayer player, String issue) {
        ticketManager.createTicket(player, issue);
    }

    public Ticket getTicket(CorePlayer player) {
        return ticketManager.getTicket(player);
    }

    public static String getChatPrefix() {
        return Chat.BRACE + "[" + Chat.PLUGIN_PREFIX + "SpleefLeague" + Chat.BRACE + "] " + Chat.DEFAULT;
    }

    public void tempban(String sender, OfflinePlayer target, long millis, String reason) {
        Infraction infraction = new Infraction();
        infraction.setUuid(target.getUniqueId())
                .setPunisher(sender)
                .setType(Infraction.Type.TEMPBAN)
                .setDuration(millis)
                .setReason(reason);
        Infraction.create(infraction);

        if (target.isOnline()) {
            ((Player) target).kickPlayer("TempBan for " + infraction.getRemainingTimeString() + ": " + reason + "!");
        }
        Core.getInstance().sendMessage(ChatChannel.getChannel(ChatChannel.Channel.STAFF),
                "TempBanned player " + target.getName() + " for " + infraction.getRemainingTimeString() + (reason.length() > 0 ? (": " + reason) : ""));
    }
    
    public void ban(String sender, OfflinePlayer target, String reason) {
        Infraction infraction = new Infraction();
        infraction.setUuid(target.getUniqueId())
                .setPunisher(sender)
                .setType(Infraction.Type.BAN)
                .setDuration(0)
                .setReason(reason);
        Infraction.create(infraction);

        if (target.isOnline()) {
            ((Player) target).getLocation().getWorld().strikeLightning(((Player) target).getLocation());
            ((Player) target).kickPlayer("Banned: " + reason + "!");
        }
        Core.getInstance().sendMessage(ChatChannel.getChannel(ChatChannel.Channel.STAFF), "Banned player " + target.getName() + (reason.length() > 0 ? (": " + reason) : ""));
    }
    
    public void unban(String sender, OfflinePlayer target, String reason) {
        Infraction infraction = new Infraction();
        infraction.setUuid(target.getUniqueId())
                .setPunisher(sender)
                .setType(Infraction.Type.UNBAN)
                .setDuration(0)
                .setReason(reason);
        Infraction.create(infraction);

        Core.getInstance().sendMessage(ChatChannel.getChannel(ChatChannel.Channel.STAFF), "Unbanned player " + target.getName() + (reason.length() > 0 ? (": " + reason) : ""));
    }

    public void kick(String sender, OfflinePlayer target, String reason) {
        Infraction infraction = new Infraction();
        infraction.setUuid(target.getUniqueId())
                .setPunisher(sender)
                .setType(Infraction.Type.WARNING)
                .setDuration(0)
                .setReason(reason);
        Infraction.create(infraction);
        
        if (target.isOnline()) {
            ((Player) target).getLocation().getWorld().strikeLightning(((Player) target).getLocation());
            ((Player) target).kickPlayer("Kicked: " + reason + "!");
        }
        Core.getInstance().sendMessage(ChatChannel.getChannel(ChatChannel.Channel.STAFF), "Kicked player " + target.getName() + (reason.length() > 0 ? (": " + reason) : ""));
    }

    public void warn(String sender, OfflinePlayer target, String reason) {
        Infraction infraction = new Infraction();
        infraction.setUuid(target.getUniqueId())
                .setPunisher(sender)
                .setType(Infraction.Type.WARNING)
                .setDuration(0)
                .setReason(reason);
        Infraction.create(infraction);
        
        if (target.isOnline()) {
            Core.sendMessageToPlayer(Core.getInstance().getPlayers().get(target.getPlayer()), "Warning from " + sender + ": " + reason);
        }
        Core.getInstance().sendMessage(ChatChannel.getChannel(ChatChannel.Channel.STAFF), "Warned player " + target.getName() + (reason.length() > 0 ? (": " + reason) : ""));
    }

    // Send message to global channel
    public void sendMessage(String msg) {
        Chat.sendMessage(ChatChannel.getDefaultChannel(), getChatPrefix() + msg);
    }

    // Send message to a specific player
    public void sendMessage(DBPlayer dbp, String msg) {
        Chat.sendMessageToPlayer(dbp, getChatPrefix() + msg);
    }
    public void sendMessage(CommandSender cs, String msg) {
        cs.sendMessage(getChatPrefix() + msg);
    }

    // Send an invalid command format message to player
    public void sendMessageInvalid(DBPlayer dbp, String msg) {
        Chat.sendMessageToPlayerInvalid(dbp, getChatPrefix() + msg);
    }

    // Send player to a ChatChannel
    public void sendMessage(ChatChannel cc, String msg) {
        Chat.sendMessage(cc, getChatPrefix() + msg);
    }

    // Send player to a ChatGroup
    public void sendMessage(ChatGroup cg, String msg) {
        cg.sendMessage(getChatPrefix() + msg);
    }
    
    // Tell player to player message
    public void sendTell(CorePlayer sender, CorePlayer target, String msg) {
        sender.sendMessage(Chat.DEFAULT + "[me -> " + target.getDisplayName() + "] " + Chat.WHISPER + msg);
        target.sendMessage(Chat.DEFAULT + "[" + sender.getDisplayName() + " -> me] " + Chat.WHISPER + msg);
        target.setReply(sender.getPlayer());
    }

    public void broadcast(String msg) {
        String title, subtitle;
        String msgs[] = msg.split("/");
        title = msgs[0];
        subtitle = msgs.length > 1 ? msgs[1] : "";
        Chat.sendTitle(ChatChannel.getDefaultChannel(), Chat.BROADCAST + title, Chat.BROADCAST + subtitle, 5, msg.length() * 2 + 10, 15);
    }
    
    // Send message to a player
    public static void sendMessageToPlayer(DBPlayer dbp, String msg) {
        Chat.sendMessageToPlayer(dbp, getChatPrefix() + msg);
    }
}
