/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.spleefleague.core.commands;

import com.spleefleague.core.command.CommandTemplate;
import com.spleefleague.core.Core;
import com.spleefleague.core.annotation.CommandAnnotation;
import com.spleefleague.core.annotation.LiteralArg;
import com.spleefleague.core.chat.ticket.Ticket;
import com.spleefleague.core.player.CorePlayer;
import com.spleefleague.core.player.Rank;

/**
 * @author NickM13
 */
public class TicketCommand extends CommandTemplate {
    
    public TicketCommand() {
        super(TicketCommand.class, "ticket", Rank.DEFAULT);
        setUsage("/ticket <msg|player|close> [msg|player]");
    }
    
    @CommandAnnotation
    public void ticket(CorePlayer sender, String msg) {
        Ticket ticket;
        if ((ticket = Core.getInstance().getTicket(sender)) == null || !ticket.isOpen()) {
            Core.getInstance().openTicket(sender, msg);
        } else {
            error(sender, "Please wait for your current ticket to be closed");
        }
    }
    
    @CommandAnnotation(minRank="MODERATOR")
    public void ticketClose(CorePlayer sender, @LiteralArg(value="close") String close, CorePlayer cp) {
        Ticket ticket;
        if ((ticket = Core.getInstance().getTicket(cp)) != null) {
            ticket.close(sender);
        }
    }
    
    @CommandAnnotation(minRank="MODERATOR")
    public void ticket(CorePlayer sender, CorePlayer cp2, String msg) {
        Ticket ticket;
        if ((ticket = Core.getInstance().getTicket(cp2)) != null) {
            ticket.sendMessageToSender(sender, msg);
        }
    }
    
}
