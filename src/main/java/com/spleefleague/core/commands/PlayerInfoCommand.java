/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.spleefleague.core.commands;

import com.mongodb.client.MongoCursor;
import com.spleefleague.core.Core;
import com.spleefleague.core.annotation.CommandAnnotation;
import com.spleefleague.core.chat.Chat;
import com.spleefleague.core.command.CommandTemplate;
import com.spleefleague.core.infraction.Infraction;
import com.spleefleague.core.player.CorePlayer;
import com.spleefleague.core.player.Rank;
import com.spleefleague.core.util.TimeUtils;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.bson.Document;
import org.bukkit.OfflinePlayer;

/**
 * @author NickM13
 */
public class PlayerInfoCommand extends CommandTemplate {
    
    public PlayerInfoCommand() {
        super(PlayerInfoCommand.class, "playerinfo", Rank.DEFAULT);
        addAlias("pi");
        setUsage("/playerinfo [player]");
        setDescription("Get player's server statistics");
    }
    
    @CommandAnnotation
    public void playerinfo(CorePlayer sender) {
        playerinfo(sender, sender.getPlayer());
    }
    
    @CommandAnnotation
    public void playerinfo(CorePlayer sender, OfflinePlayer op) {
        CorePlayer cp = Core.getInstance().getPlayers().getOffline(op.getUniqueId());
        sender.sendMessage(Chat.BRACE + Chat.fillTitle("[ " + cp.getDisplayName() + "'s data" + Chat.BRACE + " ]"));
        sender.sendMessage(Chat.BRACE + "Name: " +
                Chat.DEFAULT + op.getName());
        sender.sendMessage(Chat.BRACE + "UUID: " +
                Chat.DEFAULT + op.getUniqueId().toString());
        sender.sendMessage(Chat.BRACE + "Rank: " +
                Chat.DEFAULT + cp.getRank().getColor() + cp.getRank().getDisplayNameUnformatted());
        sender.sendMessage(Chat.BRACE + "State: " +
                Chat.DEFAULT + getState(cp));
        sender.sendMessage(Chat.BRACE + "IP: " +
                Chat.DEFAULT + getIp(cp));
        sender.sendMessage(Chat.BRACE + "Shared accounts: " +
                Chat.DEFAULT + getSharedAccounts(cp));
        sender.sendMessage(Chat.BRACE + "Total online time: " +
                Chat.DEFAULT + getOnlineTime(cp));
    }
    
    private String getState(CorePlayer cp) {
        Infraction inf = Infraction.getActive(UUID.fromString(cp.getUuid()));
        String state = "";
        
        if (!cp.isOnline()) {
            if (inf == null) {
                state = "Offline";
            } else {
                switch (inf.getType()) {
                    case BAN:
                        state = "Permanent Ban";
                        break;
                    case TEMPBAN:
                        state = "Temporary Ban";
                        break;
                    default:
                        state = "Offline";
                        break;
                }
            }
        } else {
            state = "Online";
        }
        
        return state;
    }
    
    private String getIp(CorePlayer cp) {
        Document doc = Core.getInstance().getPluginDB().getCollection("PlayerConnections").find(new Document("uuid", cp.getUuid()).append("type", "JOIN")).sort(new Document("date", -1)).first();
        
        return doc.get("ip", String.class);
    }
    
    private String getSharedAccounts(CorePlayer cp) {
        MongoCursor<Document> cursor = Core.getInstance().getPluginDB().getCollection("PlayerConnections").find().iterator();
        
        String sharedAccounts = "";
        Set<String> uuids = new HashSet<>();
        Set<String> ips = new HashSet<>();
        
        while (cursor.hasNext()) {
            Document doc = cursor.next();
            if (doc.get("type", String.class).equalsIgnoreCase("JOIN")) {
                if (cp.getUuid().equalsIgnoreCase(doc.get("uuid", String.class))) {
                    ips.add(doc.get("ip", String.class));
                }
            }
        }
        
        for (String ip : ips) {
            cursor = Core.getInstance().getPluginDB().getCollection("PlayerConnections").find(new Document("ip", ip)).iterator();
            while (cursor.hasNext()) {
                Document doc = cursor.next();
                uuids.add(doc.get("uuid", String.class));
            }
        }
        
        for (String uuid : uuids) {
            Document doc = Core.getInstance().getPluginDB().getCollection("Players").find(new Document("uuid", uuid)).first();
            if (doc != null && doc.containsKey("username")) {
                if (sharedAccounts.length() > 0) sharedAccounts += ", ";
                    sharedAccounts += doc.get("username", String.class);
            }
        }
        
        return sharedAccounts;
    }
    
    private String getOnlineTime(CorePlayer cp) {
        String onlineTime = "";
        long onlineTimeTotal = 0;
        long lastJoin = -1;
        
        MongoCursor<Document> cursor = Core.getInstance().getPluginDB().getCollection("PlayerConnections").find(new Document("uuid", cp.getUuid())).sort(new Document("date", 1)).iterator();
        
        while (cursor.hasNext()) {
            Document doc = cursor.next();
            long time = doc.get("date", Date.class).getTime();
            switch (doc.get("type", String.class)) {
                case "JOIN":
                    lastJoin = time;
                    break;
                case "LEAVE":
                    if (lastJoin != -1) {
                        onlineTimeTotal += time - lastJoin;
                    }
                    break;
                default: break;
            }
        }
        
        onlineTime = TimeUtils.timeToString(onlineTimeTotal);
        
        return onlineTime;
    }

}
