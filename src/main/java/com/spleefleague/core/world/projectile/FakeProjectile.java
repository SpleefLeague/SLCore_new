/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.spleefleague.core.world.projectile;

import com.spleefleague.core.annotation.DBField;
import com.spleefleague.core.util.database.DBEntity;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;

/**
 * @author NickM13
 */
public class FakeProjectile extends DBEntity {
    
    @DBField
    public EntityType entityType;
    @DBField
    public Integer range;
    @DBField
    public Integer fireRate;
    @DBField
    public Integer power;
    @DBField
    public Integer bounces = 1;
    @DBField
    public Integer lifeTicks = 100;
    @DBField
    public Double spread = 0.;
    @DBField
    public Double drag = 0.;
    @DBField
    public Boolean gravity = true;
    @DBField
    public Material material = Material.BARRIER;
    @DBField
    public Integer damage = 0;
    
    public FakeProjectile() {
        
    }
    /*
    public FakeProjectile(EntityType entityType, int range, int fireRate, int power, boolean gravity, int bounces, double bounciness) {
        this.entityType = entityType;
        this.range = range;
        this.fireRate = fireRate;
        this.power = power;
        this.bounces = bounces;
        this.gravity = gravity;
    }
    
    public FakeProjectile(int lifeTicks, EntityType entityType, int range, int fireRate, int power, boolean gravity, int bounces, double bounciness, double spread) {
        this(entityType, range, fireRate, power, gravity, bounces, bounciness);
        this.lifeTicks = lifeTicks;
        this.spread = spread;
    }
    
    public FakeProjectile(EntityType entityType, int range, int fireRate, int power, boolean gravity, int bounces, double bounciness, double spread) {
        this(entityType, range, fireRate, power, gravity, bounces, bounciness);
        this.spread = spread;
    }
    */
}
