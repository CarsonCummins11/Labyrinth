package Labyrinth;

import java.awt.Rectangle;
import java.awt.Point;

public class LightSource {

    public static final int RAY_STEP = 2;
    public static final int ARC_STEP = 2;

    int x;
    int y;
    int strength;
    int direction;
    int arc;
    App game;
    public LightSource(int xx, int yy, int str, int dir, int arc_ang, App a){
        x=xx;
        y=yy;
        game = a;
        strength=str;
        direction=dir;
        arc=arc_ang;
    }
    //builds a point light source
    public LightSource(int xx, int yy, int str,App a){
        x=xx;
        y=yy;
        strength=str;
        direction = 0;
        arc = 360;
        game = a;

    }
    public void draw(LightMap m){
        if(arc<360){
            int[][] poly = constructLightPolygon();
            m.polygon_light(poly[0], poly[1], strength,x,y);
        }else{
            m.circle_light(x, y, strength);
        }
    }
    public boolean shadowCasterAt(int x, int y){
        for(String s : game.objects.keySet()){
            Obj o = game.objects.get(s);
            if(o.shadow_casting){
                if(o.containsPoint(x, y)){
                    return true;
                }
            }
        }
        return false;
    }
    public boolean lightsWithin(Rectangle r){
        Point ul = r.getLocation();
        Point p1 = new Point((int)ul.getX()+r.width,(int)ul.getY());
        Point p2 = new Point((int)ul.getX(),(int)ul.getY()+r.height);
        int Xn = Math.max(p1.x, Math.min(x, p2.x)); 
        int Yn = Math.max(p1.y, Math.min(y, p2.y)); 
        int Dx = Xn - x; 
        int Dy = Yn - y; 
        return (Dx * Dx + Dy * Dy) <= strength * strength;
    }
    public int[][] constructLightPolygon(){
        int[][] ret = new int[2][arc/ARC_STEP+1];
        for(int i = direction-arc/2; i<direction+arc/2; i+=ARC_STEP){
            int[] ray_res = castRay(i);
            ret[0][(i-(direction-arc/2))/ARC_STEP] = ray_res[0];
            ret[1][(i-(direction-arc/2))/ARC_STEP] = ray_res[1];
        }
        //if it's a directed light add the x and y
        if(arc<360){
            ret[0][ret[0].length-1] = x;
            ret[1][ret[0].length-1] = y;
        }
        return ret;
    }
    public int[] castRay(int dir){
        for(int i = 0; i<strength; i+=RAY_STEP){
            int xx = (int)(i*Math.cos(Math.toRadians(dir)))+x;
            int yy = (int)(i*Math.sin(Math.toRadians(dir)))+y;
            if(shadowCasterAt(xx, yy)||i+RAY_STEP>=strength)
                return new int[]{xx,yy};
        }
        return null;
    }
}
