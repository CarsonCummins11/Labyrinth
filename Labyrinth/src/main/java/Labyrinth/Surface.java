package Labyrinth;

import javax.swing.JPanel;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;

@SuppressWarnings("serial")
public class Surface extends JPanel {
    App game;
    Rectangle view;
    Dimension room_dim;
    LightMap m;
    public Surface(App g, Dimension roomDim){
        super();
        game = g;
        room_dim = roomDim;
        view = new Rectangle(0,0,getWidth(),getHeight());
        m=null;
    }
    @Override
    public void paintComponent(Graphics g){
        super.paintComponent(g);
        BufferedImage full_room = new BufferedImage((int)room_dim.getWidth(),(int)room_dim.getHeight(),BufferedImage.TYPE_INT_ARGB);
        Graphics g_full = full_room.getGraphics();
        //darken room
        LightMap m_temp = new LightMap((int)room_dim.getWidth(), (int)room_dim.getHeight());
        m_temp.darken();
        for(String o : game.objects.keySet()){
            game.objects.get(o).draw(g_full);
        }
        for(LightSource l : game.lights){
                l.draw(m_temp);
        }
        //update view size
        view.setSize(getWidth(), getHeight());
        int lag_comp = 15;
        //draw the light map
        m_temp.getExtendedSection(view,lag_comp).draw(g_full,Math.max(0,view.x-lag_comp/2),Math.max(0,view.y-lag_comp/2));
        m = m_temp;
        g.drawImage(full_room.getSubimage(view.x,view.y,view.width,view.height),0,0,null);

    }
}
