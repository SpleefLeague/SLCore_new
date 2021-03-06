/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.spleefleague.core.request;

import com.spleefleague.core.player.CorePlayer;
import java.util.function.Consumer;

/**
 * @author NickM13
 */
public class Request {
    
    private Consumer<CorePlayer> consumer;
    
    public Request(Consumer<CorePlayer> consumer) {
        this.consumer = consumer;
    }
    
    public void accept(CorePlayer cp) {
        consumer.accept(cp);
    }
    
}
