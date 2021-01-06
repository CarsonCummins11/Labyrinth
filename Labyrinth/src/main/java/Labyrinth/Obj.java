package Labyrinth;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;


import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.RenderingHints;

public class Obj{
    private double x;
    private double y;
    private double direction;
    double speed;
    BufferedImage[] sprite;
    int frame;
    int mouseX;
    int mouseY;
    int width;
    int height;
    boolean glob;
    App game;
    Point2D[] hitbox=null;
    boolean hitbox_calculated;
    boolean shadow_casting=true;
    boolean solid=true;
    public Obj(int X, int Y,BufferedImage[] spr, App os) throws IOException {
        game = os;
        //initialize player vars
        x=X;
        y=Y;
        speed = 0;
        direction=0;
        frame = 0;
        sprite=spr;
        width=spr[0].getWidth();
        height=spr[0].getHeight();
        glob = false;
    }
    public void update(HashMap<String,String> comps){
        hitbox_calculated = false;
        x = Double.parseDouble(comps.get("x"));
        y = Double.parseDouble(comps.get("y"));
        direction = Double.parseDouble(comps.get("direction"));
        speed= Double.parseDouble(comps.get("speed"));
    }
    public HashMap<String,String> toHashMap(){
        return null;
    }
    public void calculate_hitbox(){
        if(hitbox==null){
            hitbox = new Point2D[4];
        }
        AffineTransform a1 = new AffineTransform();
        a1.rotate(direction,x+width/2,y+height/2);
        hitbox[0] = a1.transform(new Point((int)x,(int)y), null);
        hitbox[1]= a1.transform(new Point((int)x+width,(int)y), null);
        hitbox[2]= a1.transform(new Point((int)x+width,(int)y+height), null);
        hitbox[3] = a1.transform(new Point((int)x,(int)y+height), null);
        hitbox_calculated=true;
    }
    private int tri_area(Point2D A, Point2D B, Point2D C){
        return (int)Math.abs( (B.getX() * A.getY() - A.getX() * B.getY()) + (C.getX() * B.getY() - B.getX() * C.getY()) + (A.getX() * C.getY() - C.getX() * A.getY()) ) / 2;
    }

    public boolean containsPoint(int xx, int yy){
        if(!hitbox_calculated)calculate_hitbox();
        Point2D P = new Point(xx,yy);
        Point2D A = hitbox[0];
        Point2D B = hitbox[1];
        Point2D C = hitbox[2];
        Point2D D = hitbox[3];
        int area = tri_area(A, P, D)+tri_area(D, P, C)+tri_area(C, P, B)+tri_area(P, B, A);
        if(area>(width*height)){
            return false;
        }
        return true;

    }
    //returns array objects whose hitboxes contain the given point
    public Obj[] atPoint(int x, int y){
        ArrayList<Obj> retlist = new ArrayList<>();
        for(String s : game.objects.keySet()){
            Obj o = game.objects.get(s);
            if(o.containsPoint(x,y)){
                retlist.add(o);
            }
        }
        Obj[] ret = new Obj[retlist.size()];
        for(int i = 0; i< ret.length; i++){
            ret[i] = retlist.get(i);
        }
        return ret;

    }
    public boolean checkCollision(Obj other){
        if(!hitbox_calculated)calculate_hitbox();
        if(!other.hitbox_calculated)other.calculate_hitbox();
        for(Point2D p : other.hitbox){
            if(containsPoint((int)p.getX(), (int)p.getY()))return true;
        }
        return false;
    }
    public double mouseDirection(){
        double c_x = (x+width / 2-game.panel.view.x)-mouseX;
        double c_y = (y+height / 2-game.panel.view.y)-mouseY;
        double ret = Math.atan2(c_y,c_x)+1.5*Math.PI;
        return ret;
    }
    public void draw(Graphics g) {
        if(sprite!=null){
            Graphics2D g2 = (Graphics2D) g;
            AffineTransform a = new AffineTransform();
            a.translate(x, y);
            a.rotate(direction,width / 2,height / 2);
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2.drawImage(sprite[frame], a, null);
        }
    }
    public void keyPress(int key){

    }
    public void keyRelease(int key){

    }
    public void step(){
        if(!solid){
            x+=speed*Math.cos(direction+Math.PI*1.5);
            y+=speed*Math.sin(direction+Math.PI*1.5);
        }else{
            Obj[] p = atPoint((int)(x+speed*Math.cos(direction+Math.PI*1.5)), (int)(y+speed*Math.sin(direction+Math.PI*1.5)));
            boolean go = true;
            for(Obj o : p){
                if(o.solid){
                    go=false;
                    break;
                }
            }
            if(go){
                x+=speed*Math.cos(direction+Math.PI*1.5);
                y+=speed*Math.sin(direction+Math.PI*1.5);
            }
        }
    }
    public void turnTo(double dir){
        double dir_prev = direction;
        direction = dir;
        calculate_hitbox();
        for (Point2D p : hitbox){
            Obj[] atP = atPoint((int)p.getX(), (int)p.getY());
            for(Obj o :atP){
                if(o.solid){
                    direction=dir_prev;
                    calculate_hitbox();
                    break;
                }
            }   
        }
    }
    public void setDirection(double dir){
        direction=dir;
    }
    public double getDirection(){
        return direction;
    }
    public double getX(){
        return x;
    }
    public double getY(){
        return y;
    }
    public void mouseMoved(int mx, int my){
        mouseX = mx;
        mouseY = my;
    }
    public void mousePress(){

    }
    public void mouseRelease(){
        
    }
    public void mouseClick(){

    }
    public void Collision(Obj other){

    }
}
