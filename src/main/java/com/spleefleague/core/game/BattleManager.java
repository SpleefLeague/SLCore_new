/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.spleefleague.core.game;

import com.spleefleague.core.Core;
import com.spleefleague.core.party.Party;
import com.spleefleague.core.player.CorePlayer;
import com.spleefleague.core.plugin.CorePlugin;
import com.spleefleague.core.queue.PlayerQueue;
import com.spleefleague.core.queue.QueueContainer;
import com.spleefleague.core.util.database.DBPlayer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.bukkit.Bukkit;

/**
 * @author NickM13
 * @param <B>
 */
public class BattleManager<B extends Battle> implements QueueContainer {
    
    Class battleClass;

    String name;
    String displayName;
    ArenaMode mode;

    PlayerQueue queue;
    List<B> battles = new ArrayList<>();
    
    public void init() {
        Bukkit.getScheduler().scheduleSyncRepeatingTask(Core.getInstance(), () -> {
            Iterator<B> bit = battles.iterator();
            Battle b;
            while (bit.hasNext()) {
                b = bit.next();
                if (b != null) {
                    if (!b.isOngoing()) {
                        bit.remove();
                    } else {
                        b.updateScoreboard();
                        b.doCountdown();
                    }
                }
            }
        }, 20L, 20L);
        Bukkit.getScheduler().scheduleSyncRepeatingTask(Core.getInstance(), () -> {
            Iterator<B> bit = battles.iterator();
            Battle b;
            while (bit.hasNext()) {
                b = bit.next();
                if (b != null) {
                    b.updateExperience();
                }
            }
        }, 2L, 2L);
    }
    
    public void close() {
        for (B b : battles) {
            b.endBattle();
        }
    }

    public BattleManager(ArenaMode mode) {
        this.name = mode.getName();
        this.displayName = mode.getDisplayName();
        this.mode = mode;
        this.battleClass = mode.getBattleClass();

        queue = new PlayerQueue();
        queue.initialize(displayName, this, this.mode.getTeamStyle().equals(ArenaMode.TeamStyle.TEAM));
        battles = new ArrayList<>();
    }
    
    public boolean isTeamQueue() {
        return this.mode.getTeamStyle().equals(ArenaMode.TeamStyle.TEAM);
    }
    
    public int queuePlayer(DBPlayer dbp) {
        if (this.isTeamQueue()) {
            Party party = Core.getInstance().getPlayers().get(dbp).getParty();
            if (party == null) {
                Core.sendMessageToPlayer(dbp, "You must been in a party to join this queue!");
                return 1;
            }
            if (this.mode.getRequiredTeamSizes().contains(party.getPlayers().size())) {
                Core.sendMessageToPlayer(dbp, "No queue exists for your party size!");
                return 2;
            }
        }
        queue.queuePlayer(dbp);
        return 0;
    }
    public int queuePlayer(DBPlayer dbp, Arena arena) {
        if (arena == null) {
            return queuePlayer(dbp);
        }
        else {
            if (arena.isPaused()) {
                Core.sendMessageToPlayer(dbp, arena.getDisplayName() + " is currently disabled.");
                return 3;
            } else {
                if (this.isTeamQueue()) {
                    Party party = Core.getInstance().getPlayers().get(dbp).getParty();
                    System.out.println("Is Team Queue");
                    if (party == null) {
                        Core.sendMessageToPlayer(dbp, "You must been in a party to join this queue!");
                        return 1;
                    }
                    if (arena.getTeamSize() != party.getPlayers().size()) {
                        Core.sendMessageToPlayer(dbp, "That arena requires a team size of " + arena.getTeamSize() + "!");
                        return 2;
                    }
                }
                queue.queuePlayer(dbp, arena);
                return 0;
            }
        }
    }
    
    private ArrayList<DBPlayer> gatherPlayers(int num, int teamSize) {
        ArrayList<DBPlayer> splayers = new ArrayList<>();
        ArrayList<DBPlayer> dbplayers = queue.getMatchedPlayers(num, teamSize);
        if (dbplayers == null) return null;
        for (DBPlayer dbp : dbplayers) {
            splayers.add(Core.getInstance().getPlayers().get(dbp.getPlayer()));
            if (splayers.size() == num) return splayers;
        }
        return null;
    }

    @Override
    public void checkQueue() {
        if (this.isTeamQueue()) {
            for (int size : this.mode.getRequiredTeamSizes()) {
                if (queue.getQueueSize() >= this.mode.getRequiredTeams()) {
                    ArrayList<DBPlayer> players = gatherPlayers(this.mode.getRequiredTeams(), size);
                    if (players != null) {
                        startMatch(players, queue.getLastArenaName());
                    }
                }
            }
        } else {
            if (queue.getQueueSize() >= this.mode.getRequiredTeams()) {
                ArrayList<DBPlayer> players = gatherPlayers(this.mode.getRequiredTeams(), 0);
                if (players != null) {
                    startMatch(players, queue.getLastArenaName());
                }
            }
        }
    }
    
    public B getPlayerBattle(DBPlayer player) {
        return (B) player.getBattle();
    }
    
    public void startMatch(List<DBPlayer> players, String name) {
        Arena arena = Arena.getByName(name, mode);
        B sb = null;
        List<DBPlayer> playersFull = new ArrayList<>();
        if (this.isTeamQueue()) {
            int size = -1;
            for (DBPlayer dbp : players) {
                CorePlayer cp = Core.getInstance().getPlayers().get(dbp);
                Party party = cp.getParty();
                if (party == null) return;
                if (size == -1) {
                    size = party.getPlayers().size();
                } else if (size != party.getPlayers().size()) {
                    return;
                }
                for (CorePlayer cp2 : party.getPlayers()) {
                    playersFull.add(cp2);
                    if (CorePlugin.isInBattleGlobal(cp2.getPlayer())) {
                        party.getChatGroup().sendMessage(cp2.getDisplayName() + " is already in a battle!");
                        System.out.println("Player " + cp2.getDisplayName() + " is already in a battle!");
                        Core.getInstance().unqueuePlayerGlobally(cp);
                        Core.getInstance().unqueuePlayerGlobally(cp2);
                        return;
                    }
                }
            }
        } else {
            playersFull = players;
            for (DBPlayer sp : playersFull) {
                CorePlayer cp = Core.getInstance().getPlayers().get(sp);
                Party party = cp.getParty();
                if (party != null) {
                    party.leave(cp);
                }
                if (CorePlugin.isInBattleGlobal(sp.getPlayer())) {
                    System.out.println("Player " + cp.getDisplayName() + " is already in a battle!");
                    Core.getInstance().unqueuePlayerGlobally(cp);
                    return;
                }
            }
        }
        try {
            if (arena.isAvailable()) {
                for (DBPlayer dbp : playersFull) {
                    CorePlayer cp = Core.getInstance().getPlayers().get(dbp);
                    Core.getInstance().unqueuePlayerGlobally(cp);
                }
                sb = (B) battleClass
                        .getDeclaredConstructor(List.class, mode.arenaClass)
                        .newInstance(players, arena);
                sb.startBattle();
                battles.add(sb);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public void endMatch(B battle) {
        battles.remove(battle);
    }
    
}
