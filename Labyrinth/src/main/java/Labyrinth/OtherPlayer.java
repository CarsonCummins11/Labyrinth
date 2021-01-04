package Labyrinth;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.HashMap;
public class OtherPlayer extends Obj {
    public OtherPlayer(int xx, int yy,int sp, double dir, BufferedImage[] spr,App a) throws IOException{
        super(xx,yy,spr,a);
        turnTo(dir);
        speed = sp;
    }    
    public static OtherPlayer loadFromMap(HashMap<String,String> o, App a, BufferedImage[] spr) throws IOException{
        return new OtherPlayer(
            Integer.parseInt(o.get("x")),
            Integer.parseInt(o.get("y")),
            Integer.parseInt(o.get("speed")),
            Integer.parseInt(o.get("direction")),
            spr,
            a
        );
    }
}
