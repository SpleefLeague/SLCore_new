/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.spleefleague.core.game;

import com.google.common.collect.Lists;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.spleefleague.core.Core;
import com.spleefleague.core.annotation.DBField;
import com.spleefleague.core.player.CorePlayer;
import com.spleefleague.core.util.CoreUtils;
import com.spleefleague.core.util.Day;
import com.spleefleague.core.util.database.DBEntity;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bson.Document;

/**
 * @author NickM13
 */
public class Leaderboard extends DBEntity {
    
    private static Map<String, Leaderboard> leaderboards = new HashMap<>();
    private static MongoCollection lbCollection = null;
    
    public static void init() {
        lbCollection = Core.getInstance().getPluginDB().getCollection("Leaderboards");
        MongoCursor<Document> it = lbCollection.find().iterator();
    }
    public static void init(String name, LeaderboardStyle style) {
        Document doc;
        Leaderboard leaderboard = new Leaderboard(style);
        if ((doc = (Document) lbCollection.find(new Document("name", String.class)).first()) != null) {
            leaderboard.load(doc);
        }
        leaderboard.checkResetDay();
    }
    
    public static Leaderboard getLeaderboard(String name) {
        if (leaderboards.containsKey(name))
            return leaderboards.get(name);
        return null;
    }
    public static Set<String> getLeaderboardNames() {
        return leaderboards.keySet();
    }
    public static Set<String> getLeaderboardStyles() {
        return CoreUtils.enumToSet(LeaderboardStyle.class);
    }
    
    public static void close() {
        if (lbCollection == null) return;
        
        for (HashMap.Entry<String, Leaderboard> lb : leaderboards.entrySet()) {
            Document doc = lb.getValue().save();
            if (lbCollection.find(new Document("name", lb.getValue().getName())).first() != null) {
                lbCollection.replaceOne(new Document("name", lb.getValue().getName()), doc);
            } else {
                lbCollection.insertOne(doc);
            }
        }
    }
    public static void refreshPlayer(String name, UUID player) {
        leaderboards.get(name).updatePlayer(player);
    }
    public static UUID getLeadingPlayer(String name) {
        return leaderboards.get(name).first();
    }
    public static String getLeadingPlayerName(String name) {
        UUID player = leaderboards.get(name).first();
        return player != null ? Core.getInstance().getPlayers().getOffline(player).getDisplayName() : "No Lead";
    }
    public static int getLeadingPlayerScore(String name) {
        UUID player = leaderboards.get(name).first();
        return player != null ? Core.getInstance().getPlayers().getOffline(leaderboards.get(name).first()).getScore(name) : 0;
    }
    public static int getPlace(String name, UUID player) {
        return leaderboards.get(name).getPlaceOf(player);
    }
    public static void setPlayerScore(String name, UUID player, int score) {
        Core.getInstance().getPlayers().get(player).setScore(name, score);
        leaderboards.get(name).updatePlayer(player);
    }
    /** 
     * Only sets player score if its higher than their previous best
     * Used for SJ Endless
     * @param name
     * @param player
     * @param score
     */
    public static void checkPlayerScore(String name, UUID player, int score) {
        Core.getInstance().getPlayers().get(player).checkScore(name, score);
        leaderboards.get(name).updatePlayer(player);
    }
    
    public enum LeaderboardStyle {
        ALLTIME,
        YEARLY,
        BIANNUALLY,
        QUARTERLY,
        MONTHLY,
        WEEKLY,
        DAILY;
    }
    
    class RankedPlayer {
        
        public CorePlayer cp;
        public int score;
        
    }
    
    class RankedPlayerComparator implements Comparator<UUID> {
        
        String name;
        
        public RankedPlayerComparator(String name) {
            this.name = name;
        }
        
        @Override
        public int compare(UUID p1, UUID p2) {
            if (p1.equals(p2)) return 0;
            int score1 = Core.getInstance().getPlayers().get(p1).getScore(name);
            int score2 = Core.getInstance().getPlayers().get(p2).getScore(name);
            if (score1 < score2) {
                return -1;
            }
            return 1;
        }
        
    }
    
    @DBField
    private String name;
    @DBField
    // Day to reset leaderboard
    private Integer resetDay;
    private LeaderboardStyle style;
    @DBField
    private List<UUID> players;
    
    public Leaderboard(LeaderboardStyle style) {
        this.style = style;
        this.players = new ArrayList<>();
        
    }
    
    public void setResetDay() {
        switch (style) {
            case DAILY:
                resetDay = Day.getCurrentDay() + 1;
                break;
            case WEEKLY:
                resetDay = Day.getCurrentDay() + 1;
                break;
        }
    }
    
    public boolean checkResetDay() {
        if (resetDay <= Day.getCurrentDay()) {
            
        }
        return true;
    }
    
    public List<UUID> getPlayers() {
        return Lists.newArrayList(players);
    }
    
    public String getName() {
        return name;
    }
    
    public UUID first() {
        if (players.isEmpty()) return null;
        return players.get(0);
    }
    
    public UUID getAt(int id) {
        if (id >= players.size()) return null;
        return players.toArray(new UUID[players.size()])[id];
    }
    
    public int getPlaceOf(UUID player) {
        return players.indexOf(player) + 1;
    }
    
    public void updatePlayer(UUID player) {
        if (players.contains(player))
            players.remove(player);
        
        CorePlayer cp1, cp2;
        cp1 = Core.getInstance().getPlayers().get(player);
        boolean inserted = false;
        for (int i = 0; i < players.size(); i++) {
            cp2 = Core.getInstance().getPlayers().getOffline(players.get(i));
            if (cp1.getScore(name) > cp2.getScore(name)) {
                players.add(i, player);
                inserted = true;
                break;
            }
        }
        if (!inserted) {
            players.add(player);
        }
    }
    
}
