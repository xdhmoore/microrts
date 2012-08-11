/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package rts;

import rts.units.*;

/**
 *
 * @author santi
 */
public class UnitAction {
    public static final int TYPE_NONE = 0;
    public static final int TYPE_MOVE = 1;
    public static final int TYPE_ATTACK = 2;
    public static final int TYPE_HARVEST = 3;
    public static final int TYPE_RETURN = 4;
    public static final int TYPE_PRODUCE = 5;

    String actionName[] ={"none","move","attack","harvest","return","produce"};
    
    public static final int DIRECTION_NONE = -1;
    public static final int DIRECTION_UP = 0;
    public static final int DIRECTION_RIGHT = 1;
    public static final int DIRECTION_DOWN = 2;
    public static final int DIRECTION_LEFT = 3;
 
    int type = TYPE_NONE;
    int direction = DIRECTION_NONE;
    int unit_type = Unit.NONE;
    ResourceUsage r_cache = null;
    
    public UnitAction(int a_type, int a_direction, int a_unit_type) {
        type = a_type;
        direction = a_direction;
        unit_type = a_unit_type;
    }
    
   
    public int getType() {
        return type;
    }
    
   
    public int getUnitType() {
        return unit_type;
    }
    
    
    public ResourceUsage resourceUsage(Unit u, PhysicalGameState pgs) {
        if (r_cache!=null) return r_cache;
        r_cache = new ResourceUsage();
        
        switch(type) {
            case TYPE_MOVE:
                {
                    int pos = u.getX() + u.getY()*pgs.getWidth();
                    switch(direction) {
                        case DIRECTION_UP: pos -= pgs.getWidth(); break;
                        case DIRECTION_RIGHT: pos ++; break;
                        case DIRECTION_DOWN: pos += pgs.getWidth(); break;
                        case DIRECTION_LEFT: pos --; break;
                    }
                    r_cache.positionsUsed.add(pos);
                }
                break;
            case TYPE_ATTACK:
                break;
            case TYPE_HARVEST:
                break;
            case TYPE_RETURN:
                break;
            case TYPE_PRODUCE:
                {
                    switch(unit_type) {
                        case Unit.BASE: r_cache.resourcesUsed[u.getPlayer()]+=Base.BASE_COST; break;
                        case Unit.BARRACKS: r_cache.resourcesUsed[u.getPlayer()]+=Barracks.BARRACKS_COST; break;
                        case Unit.WORKER: r_cache.resourcesUsed[u.getPlayer()]+=Worker.WORKER_COST; break;
                        case Unit.LIGHT: r_cache.resourcesUsed[u.getPlayer()]+=Light.LIGHT_COST; break;
                        case Unit.HEAVY: r_cache.resourcesUsed[u.getPlayer()]+=Heavy.HEAVY_COST; break;
                    }
                    int pos = u.getX() + u.getY()*pgs.getWidth();
                    switch(direction) {
                        case DIRECTION_UP: pos -= pgs.getWidth(); break;
                        case DIRECTION_RIGHT: pos ++; break;
                        case DIRECTION_DOWN: pos += pgs.getWidth(); break;
                        case DIRECTION_LEFT: pos --; break;
                    }
                    r_cache.positionsUsed.add(pos);
                }
                break;
        }
        
        return r_cache;
    }
    
    
    public int ETA(Unit u) {
        switch(type) {
            case TYPE_NONE:
                return u.getMoveTime();
            case TYPE_MOVE:
                return u.getMoveTime();
            case TYPE_ATTACK:
                return u.getAttackTime();
            case TYPE_HARVEST:
                return 20;
            case TYPE_RETURN:
                return u.getMoveTime();
            case TYPE_PRODUCE:
                switch(unit_type) {
                    case Unit.BASE: return Base.BASE_PRODUCTION_TIME;
                    case Unit.BARRACKS: return Barracks.BARRACKS_PRODUCTION_TIME;
                    case Unit.WORKER: return Worker.WORKER_PRODUCTION_TIME;
                    case Unit.LIGHT: return Light.LIGHT_PRODUCTION_TIME;
                    case Unit.HEAVY: return Heavy.HEAVY_PRODUCTION_TIME;
                }
                break;
        }
        
        return 0;
    }
    
    
    public void execute(Unit u, GameState s) {
        PhysicalGameState pgs = s.getPhysicalGameState();
        switch(type) {
            case TYPE_NONE:
                break;
            case TYPE_MOVE:
                switch(direction) {
                    case DIRECTION_UP:      u.setY(u.getY()-1); break;
                    case DIRECTION_RIGHT:   u.setX(u.getX()+1); break;
                    case DIRECTION_DOWN:    u.setY(u.getY()+1); break;
                    case DIRECTION_LEFT:    u.setX(u.getX()-1); break;
                }
                break;
            case TYPE_ATTACK:
                {
                    Unit u2 = null;
                    switch(direction) {
                        case DIRECTION_UP:      u2 = pgs.getUnitAt(u.getX(), u.getY()-1); break;
                        case DIRECTION_RIGHT:   u2 = pgs.getUnitAt(u.getX()+1, u.getY()); break;
                        case DIRECTION_DOWN:    u2 = pgs.getUnitAt(u.getX(), u.getY()+1); break;
                        case DIRECTION_LEFT:    u2 = pgs.getUnitAt(u.getX()-1, u.getY()); break;
                    }
                    if (u2!=null) {
                        u2.setHitPoints(u2.getHitPoints() - u.getDamage());
                        if (u2.getHitPoints()<=0) {
                            s.removeUnit(u2);
                        }
                    }
                }
                break;
            case TYPE_HARVEST:
                {
                    Unit u2 = null;
                    switch(direction) {
                        case DIRECTION_UP:      u2 = pgs.getUnitAt(u.getX(), u.getY()-1); break;
                        case DIRECTION_RIGHT:   u2 = pgs.getUnitAt(u.getX()+1, u.getY()); break;
                        case DIRECTION_DOWN:    u2 = pgs.getUnitAt(u.getX(), u.getY()+1); break;
                        case DIRECTION_LEFT:    u2 = pgs.getUnitAt(u.getX()-1, u.getY()); break;
                    }
                    if (u2!=null) {                    
                        u2.setResources(u2.getResources() - 1);
                        if (u2.getResources()<=0) {
                            s.removeUnit(u2);
                        }
                        u.setResources(1);
                    }
                }
                break;
            case TYPE_RETURN:
                {
                    Player p = pgs.getPlayer(u.getPlayer());
                    p.setResources(p.getResources() + 1);
                    u.setResources(0);
                }
                break;
            case TYPE_PRODUCE:
                {
                    Unit newUnit = null;
                    int targetx = u.getX();
                    int targety = u.getY();
                    switch(direction) {
                        case DIRECTION_UP:      targety--; break;
                        case DIRECTION_RIGHT:   targetx++; break;
                        case DIRECTION_DOWN:    targety++; break;
                        case DIRECTION_LEFT:    targetx--; break;
                    }
                    switch(unit_type) {
                        case Unit.BASE:
                            newUnit = new Base(u.getPlayer(), targetx, targety);
                            break;
                        case Unit.BARRACKS:
                            newUnit = new Barracks(u.getPlayer(), targetx, targety);
                            break;
                        case Unit.WORKER:
                            newUnit = new Worker(u.getPlayer(), targetx, targety, 0);
                            break;
                        case Unit.LIGHT:
                            newUnit = new Light(u.getPlayer(), targetx, targety);
                            break;
                        case Unit.HEAVY:
                            newUnit = new Heavy(u.getPlayer(), targetx, targety);
                            break;
                        default: System.err.println("UnitAction.execute unknown unit type " + unit_type);
                    }
                    pgs.addUnit(newUnit);
                    Player p = pgs.getPlayer(u.getPlayer());
                    p.setResources(p.getResources() - newUnit.getCost());                    
                }
                break;
        }        
    }
    

    public String toString() {
        String tmp = actionName[type] + "(";
        
        if (direction == DIRECTION_UP) tmp += "up";
        if (direction == DIRECTION_RIGHT) tmp += "right";
        if (direction == DIRECTION_DOWN) tmp += "down";
        if (direction == DIRECTION_LEFT) tmp += "left";
        
        if (direction!=DIRECTION_NONE && unit_type!=Unit.NONE) tmp += ",";
        
        if (unit_type!=Unit.NONE) tmp += Unit.typeNames[unit_type];
        
        return tmp + ")";
    }
    
    public String getActionName() {
        return actionName[type];
    }

    public int getDirection() {
        return direction;
    }

}