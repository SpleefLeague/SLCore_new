/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.spleefleague.core.chat;

import com.spleefleague.core.util.database.DBPlayer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

/**
 * @author NickM13
 */
public class ChatGroup {
    
    private final Set<DBPlayer> players = new HashSet<>();
    private final Scoreboard scoreboard;
    private final Objective objective;
    private final List<String> teamsOrdered = new ArrayList<>();
    
    public ChatGroup() {
        scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        scoreboard.registerNewTeam("Players");
        scoreboard.getTeam("Players").setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.NEVER);
        objective = scoreboard.registerNewObjective("ServerName", "dummy", "=---=");
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
    }
    
    public void setScoreboardName(String name) {
        objective.setDisplayName(name);
    }
    
    // Conversion of player names or integer ids to a
    // message that isn't displayed in chat (ChatColors)
    private final Map<String, Integer> playerIdMap = new HashMap<>();
    private String idToStr(int id) {
        String str = "";
        while (id >= 0) {
            switch (id % 16) {
                case 0: str += ChatColor.AQUA;          break;
                case 1: str += ChatColor.BLACK;         break;
                case 2: str += ChatColor.BLUE;          break;
                case 3: str += ChatColor.DARK_AQUA;     break;
                case 4: str += ChatColor.DARK_BLUE;     break;
                case 5: str += ChatColor.DARK_GRAY;     break;
                case 6: str += ChatColor.DARK_GREEN;    break;
                case 7: str += ChatColor.DARK_PURPLE;   break;
                case 8: str += ChatColor.DARK_RED;      break;
                case 9: str += ChatColor.GOLD;          break;
                case 10: str += ChatColor.GRAY;         break;
                case 11: str += ChatColor.GREEN;        break;
                case 12: str += ChatColor.LIGHT_PURPLE; break;
                case 13: str += ChatColor.RED;          break;
                case 14: str += ChatColor.WHITE;        break;
                case 15: str += ChatColor.YELLOW;       break;
            }
            id -= 16;
        }
        return str;
    }
    public void addTeam(String name, String displayName) {
        playerIdMap.put(name, playerIdMap.size() + 8);
        addTeam(playerIdMap.get(name), displayName);
    }
    public void addTeam(int id, String displayName) {
        String str = idToStr(id);
        scoreboard.registerNewTeam(str).addEntry(str);
        scoreboard.getTeam(str).setPrefix(displayName);
        scoreboard.getTeam(str).setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.NEVER);
        teamsOrdered.add(str);
        for (int i = 0; i < teamsOrdered.size(); i++) {
            objective.getScore(teamsOrdered.get(i)).setScore(teamsOrdered.size() - i - 1);
        }
    }
    
    public void setTeamScore(String name, int score) {
        objective.getScore(idToStr(playerIdMap.get(name))).setScore(score);
    }
    public void setTeamScore(int id, int score) {
        objective.getScore(idToStr(id)).setScore(score);
    }
    
    public void setTeamName(String name, String displayName) {
        scoreboard.getTeam(idToStr(playerIdMap.get(name))).setPrefix(displayName);
    }
    
    public void setExperience(float progress, int level) {
        for (DBPlayer dbp : players) {
            dbp.getPlayer().sendExperienceChange(progress, level);
        }
    }
    
    public void setScore(String s1, int score) {
        objective.getScore(s1).setScore(score);
    }
    
    public void addPlayer(DBPlayer dbp) {
        dbp.getPlayer().setScoreboard(scoreboard);
        players.add(dbp);
        scoreboard.getTeam("Players").addEntry(dbp.getName());
    }
    
    public void removePlayer(DBPlayer dbp) {
        dbp.getPlayer().setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
        dbp.getPlayer().sendExperienceChange(0, 0);
        players.remove(dbp);
    }
    
    public void sendMessage(String msg) {
        for (DBPlayer dbp : players) {
            Chat.sendMessageToPlayer(dbp, msg);
        }
    }
    
    public void sendTitle(String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        for (DBPlayer dbp : players) {
            dbp.getPlayer().sendTitle(title, subtitle, fadeIn, stay, fadeOut);
        }
    }
    
}
