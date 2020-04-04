/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.spleefleague.core.chat.ticket;

import com.spleefleague.core.chat.Chat;
import com.spleefleague.core.chat.ChatChannel;
import com.spleefleague.core.chat.ChatChannel.Channel;
import com.spleefleague.core.player.CorePlayer;
import org.bukkit.Bukkit;

/**
 * @author NickM13
 */
public class Ticket {

    private CorePlayer sender;
    private String message;
    
    // Timeout if ticket is not replied to
    private long timeout;
    // Prevent multiple responses
    private long responseTimeout;
    private boolean open;
    
    public Ticket(CorePlayer player, String issue) {
        sender = player;
        message = issue;
        
        resetTimeout();
        responseTimeout = 0;
        open = true;
        
        sendMessageToSender(null, issue);
    }
    
    public CorePlayer getSender() {
        return sender;
    }
    
    public void resetTimeout() {
        // Timeout timer set to current time plus 5 minutes
        timeout = System.currentTimeMillis() + 1000 * 60 * 5;
    }
    
    public void checkTimeout() {
        if (open && System.currentTimeMillis() > timeout) {
            open = false;
            Chat.sendMessageToPlayer(sender, Chat.TICKET_PREFIX + "[Ticket]" + Chat.TICKET_ISSUE + "Your ticket has timed out.");
        }
    }
    
    private String formatSender(String issue) {
        String msg = "";
        
        msg += Chat.TICKET_PREFIX + "[Ticket: " + getSender().getDisplayName() + Chat.TICKET_PREFIX + "] ";
        msg += Chat.TICKET_ISSUE + issue;
        
        return msg;
    }
    
    // Ticket sender sees this one
    private String formatStaff1(CorePlayer player, String issue) {
        String msg = "";
        
        msg += Chat.TICKET_PREFIX + "[Ticket";
        if (player != null)
            msg += Chat.TICKET_PREFIX + ": " + player.getDisplayName();
        msg += Chat.TICKET_PREFIX + "] ";
        msg += Chat.TICKET_ISSUE + issue;
        
        return msg;
    }
    
    // Staff sees this one
    private String formatStaff2(CorePlayer player, String issue) {
        String msg = "";
        
        msg += Chat.TICKET_PREFIX + "[Ticket: " + getSender().getDisplayName();
        if (player != null)
            msg += Chat.TICKET_PREFIX + ":" + player.getDisplayName();
        msg += Chat.TICKET_PREFIX + "] ";
        msg += Chat.TICKET_ISSUE + issue;
        
        return msg;
    }
    
    public void sendMessageToSender(CorePlayer staff, String msg) {
        if (responseTimeout < System.currentTimeMillis() || staff == null) {
            Chat.sendMessageToPlayer(sender, formatStaff1(staff, msg));
            Chat.sendMessage(ChatChannel.getChannel(Channel.TICKET), formatStaff2(staff, msg));
            // 10 second response timeout
            if (staff != null) responseTimeout = System.currentTimeMillis() + 10000;
        } else {
            Chat.sendMessageToPlayer(staff, "Try again in " + (int)((responseTimeout - System.currentTimeMillis()) / 1000) + " seconds");
        }
    }
    
    public void sendMessageToStaff(String msg) {
        Chat.sendMessage(ChatChannel.getChannel(Channel.TICKET), formatSender(msg));
    }
    
    public boolean isOpen() {
        return open;
    }
    
    public void close(CorePlayer staff) {
        if (!open) return;
        open = false;
        Chat.sendMessageToPlayer(sender, Chat.TICKET_PREFIX + "[Ticket]" + Chat.TICKET_ISSUE + " Your ticket has been closed.");
        Chat.sendMessage(ChatChannel.getChannel(Channel.TICKET), Chat.TICKET_PREFIX + "[Ticket: " + sender.getDisplayName() + Chat.TICKET_PREFIX + "]" + Chat.TICKET_ISSUE + " Ticket closed.");
    }
    
}
