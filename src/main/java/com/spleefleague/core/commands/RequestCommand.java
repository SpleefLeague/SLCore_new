/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.spleefleague.core.commands;

import com.spleefleague.core.annotation.CommandAnnotation;
import com.spleefleague.core.annotation.LiteralArg;
import com.spleefleague.core.chat.ChatRequest;
import com.spleefleague.core.command.CommandTemplate;
import com.spleefleague.core.player.CorePlayer;
import com.spleefleague.core.player.Rank;

/**
 * @author NickM13
 */
public class RequestCommand extends CommandTemplate {
    
    public RequestCommand() {
        super(RequestCommand.class, "request", Rank.DEFAULT);
        setUsage("Not for personal use");
    }
    
    @CommandAnnotation
    public void requestAccept(CorePlayer sender, @LiteralArg(value="accept") String test, Integer index) {
        ChatRequest.acceptRequest(sender, index);
    }

}
