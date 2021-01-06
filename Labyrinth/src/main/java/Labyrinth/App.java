package Labyrinth;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.event.MouseInputListener;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.awt.GridLayout;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.io.File;
import java.awt.image.BufferedImage;
import java.awt.Image;
public class App implements MouseInputListener, KeyListener {
    public static final String GAMEKEY_URL = "http://127.0.0.1:5000/gamekey";
    public static final String UPDATE_URL = "http://127.0.0.1:5000/refresh";
    JFrame frame;
    public Surface panel;
    HashMap<String,Obj> objects;
    ArrayList<LightSource> lights;
    public String gameKey;
    private HttpClient client;
    public static final Dimension DIM = new Dimension(500, 500);
    HashMap<String,BufferedImage[]> sprites;
    public App() {
        client = null;
        try {
            Scanner s = new Scanner(new File("login.csv"));
            String[] login = s.nextLine().split(",");
            gameKey = requestGameKey(login[0], login[1]);
        } catch (IOException | InterruptedException e1) {
            System.out.println("Failed to establish connection with server");
        }
        try {
            addObjects();
        } catch (IOException e) {
            System.out.println("couldn't load a sprite");
        }
        initializeFrame();
        startGameLoops();
    }

    private static String mapToString(HashMap<String, String> map) {
        ObjectMapper m = new ObjectMapper();
        try {
            return m.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    private static HashMap<String, String> mapFromString(String s) {
        if(s==null||s.equals(""))return null;
        String[] r = s.substring(1, s.length() - 1).trim().replaceAll("\\s","").split(",");
        HashMap<String, String> ret = new HashMap<>();
        for (String p : r) {
            String[] l = p.split("=");
            ret.put(l[0], l[1]);
        }
        return ret;
    }
    private BufferedImage[] loadSprite(File f, double scalar) throws IOException {
        BufferedImage[] ret = null;
        if(f!=null){
            File[] imageFiles = f.listFiles();
            ret = new BufferedImage[imageFiles.length];
            for(int i = 0; i<imageFiles.length; i++){
                ret[i] = ImageIO.read(imageFiles[i]);
                ret[i] = scale(ret[i],(int)(ret[i].getWidth()*scalar),(int)(ret[i].getHeight()*scalar));
            }
        }
        return ret;
    }   
    private BufferedImage[] loadSprite(File f, int w, int h) throws IOException {
        BufferedImage[] ret = null;
        if(f!=null){
            File[] imageFiles = f.listFiles();
            ret = new BufferedImage[imageFiles.length];
            for(int i = 0; i<imageFiles.length; i++){
                if(w!=-1){
                    ret[i] = scale(ImageIO.read(imageFiles[i]),w,h);
                }else{
                    ret[i] = ImageIO.read(imageFiles[i]);
                }
            }
        }
        return ret;
    }   
    /**
 * Converts a given Image into a BufferedImage
 *
 * @param img The Image to be converted
 * @return The converted BufferedImage
 */
public static BufferedImage toBufferedImage(Image img)
{
    if (img instanceof BufferedImage)
    {
        return (BufferedImage) img;
    }

    // Create a buffered image with transparency
    BufferedImage bimage = new BufferedImage(img.getWidth(null), img.getHeight(null), BufferedImage.TYPE_INT_ARGB);

    // Draw the image on to the buffered image
    Graphics2D bGr = bimage.createGraphics();
    bGr.drawImage(img, 0, 0, null);
    bGr.dispose();

    // Return the buffered image
    return bimage;
}
    private BufferedImage scale(BufferedImage before, int w2,int h2) {
        return toBufferedImage(before.getScaledInstance(w2, h2, BufferedImage.SCALE_SMOOTH));
    }
    private void addObjects() throws IOException {
        sprites = new HashMap<>();
        //load sprites
        BufferedImage[] wall_spr = loadSprite(Wall.SPRITE, Wall.WIDTH,Wall.HEIGHT);
        BufferedImage[] player_spr = loadSprite(Player.SPRITE,Player.SCALAR);
        sprites.put("Wall",wall_spr);
        sprites.put("player",player_spr);
        objects = new HashMap<String,Obj>();
        lights = new ArrayList<LightSource>();
        Player mc = new Player(100, 100, player_spr, this);
        objects.put("0",mc);
        lights.add(new Flashlight(0, 0, 300, 0, 60, this, mc));
    }

    private void startGameLoops() {
        new Timer("drawing").scheduleAtFixedRate(new TimerTask() {
            public void run() {
                panel.repaint();
            }
        }, (long) 0, (long) (1000 / 60));
        // continously step the game
        new Timer("stepping").scheduleAtFixedRate(new TimerTask() {
            public void run() {
                step();
            }
        }, (long) 0, (long) (1000 / 60));
        // attempt to synchronize the game 10 times/second
        new Timer("synchro").scheduleAtFixedRate(new TimerTask() {
            public void run() {
                try {
                    synchro(gameKey);
                } catch (IOException | InterruptedException e) {
                    
                }
            }
        }, (long)0, (long)(1000 / 10));
    }
    public void synchro(String online_key) throws IOException,InterruptedException{
        Player mc = (Player)objects.get("0");
        if(panel.m==null)return;
        HashMap<String,String> dat = new HashMap<>();
        dat.put("player",mc.toString());
        dat.put("key",online_key);
        Flashlight f = (Flashlight)lights.get(0);
        HashMap<String,String> ret_dat = POST(UPDATE_URL,dat);
        ArrayList<LightSource> ltemp = new ArrayList<>();
        ltemp.add(f);
        for(String k : ret_dat.keySet()){
            HashMap<String,String> repr = mapFromString(ret_dat.get(k));
            if(repr.equals(null))continue;
            if(repr.get("dat_type").equals("object")){
                if(objects.containsKey(repr.get("id"))){
                    objects.get(repr.get("id")).update(repr);
                }else{
                    objects.put(repr.get("id"),objFromMap(repr));
                }
            }else{
                ltemp.add(lightFromMap(repr));
            }
        }
        lights=ltemp;
    }
    public LightSource lightFromMap(HashMap<String,String> r){
        if(r.get("type").equals("LightSource")){
            return new LightSource(
                Integer.parseInt(r.get("x")),
                Integer.parseInt(r.get("y")),
                Integer.parseInt(r.get("strength")),
                Integer.parseInt(r.get("direction")),
                Integer.parseInt(r.get("arc")),
                 this);
        }
        return null;
    }
    public Obj objFromMap(HashMap<String,String> o) throws IOException{
        String type = o.get("type");
        if(type.equals("Wall")){
            return Wall.loadFromMap(o,this,sprites.get("Wall"));
        }else if(type.equals("player")){
            return OtherPlayer.loadFromMap(o,this,sprites.get("player"));
        }
        return null;

    }
    public HashMap<String,String> POST(String url, HashMap<String,String> data) throws IOException,InterruptedException{
        if(client==null)client = HttpClient.newHttpClient();
        String dat = mapToString(data);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .POST(HttpRequest.BodyPublishers.ofString(dat))
                .build();
        HttpResponse<String> response = client.send(request,HttpResponse.BodyHandlers.ofString());
        String json = response.body();
        Map<String,Object> map = new HashMap<>();
        ObjectMapper mapper = new ObjectMapper();
        map = mapper.readValue(json,new TypeReference<Map<String, Object>>(){});
        HashMap<String,String> ret = new HashMap<>();
        if (map==null)return new HashMap<>();
        for(String s : map.keySet()){
            ret.put(s,map.get(s).toString());
        }
        return ret;
    }
    public HashMap<String,String> GET(String url){
       throw new java.lang.UnsupportedOperationException("Not supported yet.");
    }
    public String requestGameKey(String user, String pass) throws IOException, InterruptedException {
        HashMap<String, String> post_data = new HashMap<>();
        post_data.put("username",user);
        post_data.put("password",pass);
        return POST(GAMEKEY_URL,post_data).get("key");
    }
    private void initializeFrame(){
        frame = new JFrame();
        panel = new Surface(this,new Dimension(5000,5000));
        frame.setLayout(new GridLayout());
        frame.add(panel);
        frame.addKeyListener(this);
        frame.addMouseListener(this);
        frame.addMouseMotionListener(this);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        if (gd.isFullScreenSupported()) {
            frame.setUndecorated(true);
            gd.setFullScreenWindow(frame);
        } else {
            System.err.println("Full screen not supported");
            frame.setSize(DIM); // just something to let you see the window
        }

        frame.setVisible(true);
    }
    public static void main(String[] args) {
        new App();
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        if(e.getButton()==MouseEvent.BUTTON1){
        for(String o : objects.keySet()){
            objects.get(o).mouseClick();
        }
    }

    }

    @Override
    public void mousePressed(MouseEvent e) {
        if(e.getButton()==MouseEvent.BUTTON1){
            for(String o : objects.keySet()){
                objects.get(o).mousePress();
            }
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if(e.getButton()==MouseEvent.BUTTON1){
        for(String o : objects.keySet()){
            objects.get(o).mouseRelease();
        }}

    }
    public void step(){
        //detect collisions
        for(String i : objects.keySet()){
            objects.get(i).hitbox_calculated=false;
            for(String j : objects.keySet()){
                if(i.equals(j))continue;
                Obj o1 = objects.get(i);
                Obj o2 = objects.get(j);
                if(o1.checkCollision(o2)||o2.checkCollision(o1)){
                    o1.Collision(o2);
                    o2.Collision(o1);
                }
            }
        }
        for(String o : objects.keySet()){
            objects.get(o).step();
        }
    }
    @Override
    public void mouseEntered(MouseEvent e) {

    }

    @Override
    public void mouseExited(MouseEvent e) {
    }

    @Override
    public void mouseDragged(MouseEvent e) {

    }

    @Override
    public void mouseMoved(MouseEvent e) {
        for(String o : objects.keySet()){
            objects.get(o).mouseMoved(e.getX(),e.getY());
        }

    }

    @Override
    public void keyTyped(KeyEvent e) {

    }

    @Override
    public void keyPressed(KeyEvent e) {
        for(String o : objects.keySet()){
            objects.get(o).keyPress(e.getKeyCode());
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        for(String o : objects.keySet()){
            objects.get(o).keyRelease(e.getKeyCode());
        }

    }
}
