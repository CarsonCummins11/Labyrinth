package Labyrinth;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.awt.image.BufferedImage;

public class Wall extends Obj {
    public static final int WIDTH=50,HEIGHT = 50;
    public static final File SPRITE = new File("Wall_Sprite");
    public Wall(int x, int y,double dir, BufferedImage[] spr, App os) throws IOException {
        super(x,y,spr,os);
        setDirection(dir);
    }
    public static Wall loadFromMap(HashMap<String,String> o, App a,BufferedImage[] spr) throws IOException{
        return new Wall(
            Integer.parseInt(o.get("x")),
            Integer.parseInt(o.get("y")),
            Double.parseDouble(o.get("direction")),
            spr,
            a
            );

    }
    
}
