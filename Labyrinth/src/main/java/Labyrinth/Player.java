package Labyrinth;

import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.awt.image.BufferedImage;

public class Player extends Obj {
    int max_speed = 2;
    boolean moving;
    public static final int WIDTH = -1;
    public static final int HEIGHT = -1;
    public static final double SCALAR = .3;
    public static final File SPRITE = new File("Player_Sprite");
    int mov_count;
    public Player(int x, int y, BufferedImage[] spr,App os) throws IOException {
        super(x,y,spr,os);
        glob = true;
        shadow_casting=false;
        solid = false;
        mov_count = 0;
    }
    @Override
    public void step(){
        double x = getX();
        double y = getY();
        if(speed!=0){
            mov_count+=1;
            frame = ((int)(mov_count/7))%5;
        }else{
            frame = 2;
            mov_count=0;
        }
        if(Point.distance(x+width / 2, y+height/2, mouseX, mouseY)<max_speed){
            speed = 0;
        }else if(moving){
            speed =max_speed;
            turnTo(mouseDirection());
        }else if(Point.distance(x+width / 2, y+height / 2, mouseX, mouseY)>max_speed){
            //check if will be turning into another object, if not turn
            turnTo(mouseDirection());
            
        }
        super.step();
    }
    @Override
    public void keyPress(int keycode){
        if(keycode == KeyEvent.VK_W){
            speed = max_speed;
            moving = true;
        }
    }
    @Override
    public String toString(){
        return "{\"x\":\""+getX()+"\",\"y\":\""+getY()+"\",\"direction\":\""+Math.toDegrees(getDirection())+"\",\"speed\":\""+speed+"\",\"view_width\":\""+game.panel.view.width+"\",\"view_height\":\""+game.panel.view.height+"\"}";
    }
    @Override
    public void draw(Graphics g) {
        double x = getX();
        double y = getY();
        //center the view on me
        game.panel.view.x = Math.max(Math.min((int)x-game.panel.view.width/2,game.panel.room_dim.width-game.panel.view.width),0);
        game.panel.view.y = Math.max(Math.min((int)y-game.panel.view.height/2,game.panel.room_dim.height-game.panel.view.height),0);
        super.draw(g);
    }
    @Override
    public void keyRelease(int keycode){
        if(keycode==KeyEvent.VK_W){
            speed = 0;
            moving = false;
        }else if(keycode==KeyEvent.VK_ESCAPE){
            System.exit(0);
        }
    }
}
