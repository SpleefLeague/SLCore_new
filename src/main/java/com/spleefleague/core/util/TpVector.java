/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.spleefleague.core.util;

import org.bukkit.Location;
import org.bukkit.util.Vector;

/**
 * @author NickM13
 */
public class TpVector {
    protected enum TpOrigin {
        NONE, RELATIVE, DIRECTIONAL
    }
    protected class TpCoord {
        public TpOrigin origin = TpOrigin.NONE;
        public Double value;
        
        public TpCoord() {
            
        }
        public TpCoord(TpOrigin origin, Double value) {
            this.origin = origin;
            this.value = value;
        }
    }
    
    public TpCoord x, y, z;
    
    public TpVector(Double x, Double y, Double z) {
        this.x = new TpCoord(TpOrigin.NONE, x);
        this.y = new TpCoord(TpOrigin.NONE, y);
        this.z = new TpCoord(TpOrigin.NONE, z);
    }
    public TpVector(String x, String y, String z) {
        setX(x);
        setY(y);
        setZ(z);
    }
    
    public void setX(String str) {
        x = new TpCoord();
        x.origin = (str.charAt(0) == '~' ? TpOrigin.RELATIVE : (str.charAt(0) == '^' ? TpOrigin.DIRECTIONAL : TpOrigin.NONE));
        if (x.origin.equals(TpOrigin.NONE)) {
            x.value = Double.parseDouble(str);
        } else if (str.length() > 1) {
            x.value = Double.parseDouble(str.substring(1));
        } else {
            x.value = 0D;
        }
    }
    
    public void setY(String str) {
        y = new TpCoord();
        y.origin = (str.charAt(0) == '~' ? TpOrigin.RELATIVE : (str.charAt(0) == '^' ? TpOrigin.DIRECTIONAL : TpOrigin.NONE));
        if (y.origin.equals(TpOrigin.NONE)) {
            y.value = Double.parseDouble(str);
        } else if (str.length() > 1) {
            y.value = Double.parseDouble(str.substring(1));
        } else {
            y.value = 0D;
        }
    }
    
    public void setZ(String str) {
        z = new TpCoord();
        z.origin = (str.charAt(0) == '~' ? TpOrigin.RELATIVE : (str.charAt(0) == '^' ? TpOrigin.DIRECTIONAL : TpOrigin.NONE));
        if (z.origin.equals(TpOrigin.NONE)) {
            z.value = Double.parseDouble(str);
        } else if (str.length() > 1) {
            z.value = Double.parseDouble(str.substring(1));
        } else {
            z.value = 0D;
        }
    }
    
    public void apply(Location loc) {
        switch (x.origin) {
            case NONE:
                loc.setX(x.value);
                break;
            case RELATIVE:
                loc.add(new Vector(x.value, 0D, 0D));
                break;
            case DIRECTIONAL:
                loc.add(new Vector(x.value, 0D, 0D));
                break;
        }
        switch (y.origin) {
            case NONE:
                loc.setY(y.value);
                break;
            case RELATIVE:
                loc.add(new Vector(0D, y.value, 0D));
                break;
            case DIRECTIONAL:
                loc.add(new Vector(0D, y.value, 0D));
                break;
        }
        switch (z.origin) {
            case NONE:
                loc.setZ(z.value);
                break;
            case RELATIVE:
                loc.add(new Vector(0D, 0D, z.value));
                break;
            case DIRECTIONAL:
                loc.add(new Vector(0D, 0D, z.value));
                break;
        }
    }
}
