/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.spleefleague.core.util;

import com.spleefleague.core.io.converter.LocationConverter;
import com.spleefleague.core.util.database.DBVariable;
import java.util.List;
import org.bson.Document;
import org.bukkit.Location;

/**
 * @author NickM13
 */
public class Dimension implements DBVariable<Document> {
    private Point low, high;
    
    public Dimension() {
        low = new Point();
        high = new Point();
    }
    public Dimension(Point p1, Point p2) {
        low = new Point();
        high = new Point();
        equalize(p1, p2);
    }
    
    // Set lower and higher values based on two points
    private void equalize(Point p1, Point p2) {
        if (p1.x < p2.x) {
            low.x = p1.x;
            high.x = p2.x;
        } else {
            low.x = p2.x;
            high.x = p1.x;
        }
        
        if (p1.y < p2.y) {
            low.y = p1.y;
            high.y = p2.y;
        } else {
            low.y = p2.y;
            high.y = p1.y;
        }
        
        if (p1.z < p2.z) {
            low.z = p1.z;
            high.z = p2.z;
        } else {
            low.z = p2.z;
            high.z = p1.z;
        }
    }
    
    public Point getLow() {
        return low;
    }
    
    public Point getHigh() {
        return high;
    }
    
    // Check if point is contained between lower and upper bounds
    public boolean isContained(Point p) {
        return (p.x >= low.x && p.x <= high.x &&
                p.y >= low.y && p.y <= high.y &&
                p.z >= low.z && p.z <= high.z);
    }

    @Override
    public void load(Document doc) {
        // Read from two array lists
        Location loc1 = LocationConverter.load(doc.get("low", List.class));
        Location loc2 = LocationConverter.load(doc.get("high", List.class));
        
        Point p1 = new Point(loc1.getX(), loc1.getY(), loc1.getZ());
        Point p2 = new Point(loc2.getX(), loc2.getY(), loc2.getZ());
        
        equalize(p1, p2);
    }

    @Override
    public Document save() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    @Override
    public String toString() {
        return "{" + low + ", " + high + "}";
    }
}
