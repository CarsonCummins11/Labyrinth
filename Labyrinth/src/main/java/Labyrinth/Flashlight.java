package Labyrinth;


public class Flashlight extends LightSource {
    Player owner;
    public Flashlight(int xx, int yy,int str,int dir, int arc_ang, App a,Player own){
        super(xx, yy, str, dir, arc_ang,a);
        owner = own;
    }
    @Override
    public void draw(LightMap m){
        x=(int)owner.getX()+owner.width/2;
        y=(int)owner.getY()+owner.height/2;
        direction = (int)Math.toDegrees(owner.getDirection())+270;
        super.draw(m);
    }
    
}
