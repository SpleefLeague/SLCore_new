/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.spleefleague.core.chat.ticket;

import static com.spleefleague.core.Core.getInstance;
import com.spleefleague.core.chat.Chat;
import com.spleefleague.core.chat.ChatChannel;
import com.spleefleague.core.player.CorePlayer;
import java.util.HashMap;
import java.util.Map;
import org.bukkit.Bukkit;

/**
 * @author NickM13
 */
public class TicketManager {

    Map<CorePlayer, Ticket> tickets = new HashMap<>();
    
    private class TicketRunnable implements Runnable {

        @Override
        public void run() {
            for (Map.Entry<CorePlayer, Ticket> t : tickets.entrySet()) {
                t.getValue().checkTimeout();
                if (!t.getValue().isOpen()) {
                    tickets.remove(t.getKey());
                }
            }
        }

    }
    
    public TicketManager() {
        // Every 5 seconds, check all tickets for timeouts
        Bukkit.getScheduler().scheduleSyncRepeatingTask(getInstance(), new TicketRunnable(), 20L, 100L);
    }
    
    public void createTicket(CorePlayer player, String issue) {
        Ticket t = new Ticket(player, issue);
        tickets.put(player, t);
    }
    
    public Ticket getTicket(CorePlayer player) {
        return tickets.get(player);
    }
    
}
