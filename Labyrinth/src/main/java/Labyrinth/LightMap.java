package Labyrinth;

import java.awt.Graphics;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.awt.Color;
import java.awt.Rectangle;
public class LightMap {
    
    public static final int MAX_DARK = 100;
    public static final int AMBIENT = 0;
    public static final int SOFTENING_STEPS = 15;
    public static final int SHRINK_FACTOR = 3;
    public static final double DIFFUSAL_RATE = 5;
    public static final int DIFFUSAL_RADIUS = 20;
    int[][] alphas;
    public LightMap(int w, int h){
        alphas = new int[w][h];
    }
    public LightMap(int[][] a){
        alphas=a;
    }
    public LightMap getExtendedSection(Rectangle r, int by){
        //extend rectangle
        int rx = Math.max(0,r.x-by);
        int ry = Math.max(0,r.y-by);
        int rw = rx+r.width+by<alphas.length?r.width+by:alphas.length-rx;
        int rh = ry+r.height+by<alphas[0].length?r.height+by:alphas[0].length-ry;
        Rectangle sect = new Rectangle(rx,ry,rw,rh);
        //get section
        return getSection(sect);
    }
    public LightMap getSection(Rectangle r){
        int[][] ret = new int[r.width][r.height];
        for(int i = r.x; i<r.width+r.x; i++){
            for(int j = r.y; j<r.height+r.y; j++){
                ret[i-r.x][j-r.y] = alphas[i][j];
            }
        }
        return new LightMap(ret);
    }
    public Color blackOfAlpha(double alpha){
        return new Color(0,0,0,(float)(alpha));
    }
    @Override
    public String toString(){
        String ret = "{width:"+alphas.length+",height:"+alphas[0].length+",map:";
        int[][] shr = shrink(SHRINK_FACTOR);
        for(int i = 0; i<shr.length; i++){
            for(int j = 0; j<shr[0].length; j++){
                ret+=alphas[i][j];
            }
        }
        return ret;
    }
    public BufferedImage getImage(){
            BufferedImage ret = new BufferedImage(alphas.length,alphas[0].length,BufferedImage.TYPE_INT_ARGB);
            for(int i = 0; i<alphas.length; i++){
                for(int j = 0; j<alphas[0].length; j++){
                    ret.setRGB(i, j, blackOfAlpha((double)alphas[i][j]/(double)MAX_DARK).getRGB());
                }
            }
            return ret;
    }
    public void draw(Graphics g, int xx, int yy){
        long t0 = System.currentTimeMillis();
        //soften();
        System.out.println("soften time: "+Long.toString(System.currentTimeMillis()-t0));
        BufferedImage bi = getImage();
        long t1 = System.currentTimeMillis();
        g.drawImage(bi, xx, yy, null);
        System.out.println("Draw time: "+Long.toString(System.currentTimeMillis()-t1));
    }
    private int getAlpha(int i, int j,int[][] a){
        if(i<0||i>=a.length||j<0||j>=a[0].length)return -1;
        return a[i][j];
    }
    public int[][] shrink(int factor){
        int[][] alphas_shrunk = new int[alphas.length/factor][alphas[0].length/factor];
        //shrink alpha super fast by selecting every SHRINK_FACTOR'th pixel
        for(int i = 0; i<alphas.length; i+=factor){
            for(int j = 0; j<alphas[0].length; j+=factor){
                alphas_shrunk[i/factor][j/factor] = alphas[i][j];
            }
        }
        return alphas_shrunk;
    }
    public void grow(int[][] alphas_shrunk, int factor){
        for(int i = 0; i<alphas_shrunk.length; i++){
            for(int j = 0; j<alphas_shrunk[0].length; j++){
                for(int k = 0; k<factor; k++){
                    for(int h = 0; h<factor; h++){
                        alphas[i*factor+k][j*factor+h] = alphas_shrunk[i][j];
                    }
                }
            }
        }
    }
    //basically blurs the whole thing by shrinking/averaging/growing
    public void soften(){
            int[][] alphas_shrunk = shrink(SHRINK_FACTOR);
            int[][] alpha_soft = new int[alphas_shrunk.length][alphas_shrunk[0].length];
            for(int i = 0; i<alpha_soft.length; i++){
                for(int j = 0; j<alpha_soft[0].length; j++){
                    int count = 0;
                    int sum = 0;
                    for(int p = i-SOFTENING_STEPS; p<i+SOFTENING_STEPS-1; p++){
                        int h = getAlpha(p,j,alphas_shrunk);
                        if(h!=-1){
                            count++;
                            sum+=h;
                        }
                    }
                    alpha_soft[i][j] = sum/count;
                }
            }
            alphas_shrunk = alpha_soft;
            alpha_soft = new int[alphas_shrunk.length][alphas_shrunk[0].length];
            for(int i = 0; i<alpha_soft.length; i++){
                for(int j = 0; j<alpha_soft[0].length; j++){
                    int count = 0;
                    int sum = 0;
                    for(int p = j-SOFTENING_STEPS; p<j+SOFTENING_STEPS-1; p++){
                        int h = getAlpha(i,p,alphas_shrunk);
                        if(h!=-1){
                            count+=1;
                            sum+=h;
                        }
                    }
                    alpha_soft[i][j] = sum/count;
                }
            }
            //size up again and put in original array
            alphas_shrunk = alpha_soft;
            grow(alphas_shrunk,SHRINK_FACTOR);
    }
    public void darken(){
        for(int i = 0; i<alphas.length; i++){
            for(int j = 0; j<alphas[0].length; j++){
                alphas[i][j] = MAX_DARK-AMBIENT;
            }
        }
    }
    public double min(double a, double b){
        if(a>b){
            return a;
        }else{
            return b;
        }
    }
    public int alphaAsDistance(int x, int y, int srcx, int srcy, int strength){
        double dist = Point.distance(x, y, srcx, srcy);
        return Math.max((int)(MAX_DARK - (MAX_DARK*dist/strength)),0);
    }
    public void polygon_light(int[] x, int[] y, int strength, int srcx, int srcy){
        int minY = Integer.MAX_VALUE;
        for(int i : y){
            if(i<minY)minY=i;
        }
        //find maxY
        int maxY = Integer.MIN_VALUE;
        for(int i : y){
            if(i>maxY)maxY=i;
        }
        //find min x
        int minX = Integer.MAX_VALUE;
        for(int i : x){
            if(i<minX)minX=i;
        }
        //find max x
        int maxX = Integer.MIN_VALUE;
        for(int i: x){
            if(i>maxX)maxX=i;
        }
        maxX = Math.min(maxX, alphas.length);
        maxY = Math.min(maxY,alphas[0].length);
        BufferedImage polyDraw = new BufferedImage(alphas.length,alphas[0].length,BufferedImage.TYPE_INT_ARGB);
        Graphics w = polyDraw.getGraphics();
        w.setColor(Color.black);
        w.fillPolygon(x,y,x.length);
        int BLACK_RGB = Color.black.getRGB();
        for(int i = Math.max(0, minX); i<maxX; i++){
            for(int j = Math.max(0,minY); j<maxY; j++){
                try{
                    if(BLACK_RGB==polyDraw.getRGB(i,j)){
                        lighten(i, j, alphaAsDistance(i, j, srcx, srcy, strength));
                    }   
                }catch(Exception e){
                    System.out.println(i+","+j);
                    break;
                }
            }
        }
    }
    private void lighten_point(int x, int y, int val){
        if(x>=0&&x<alphas.length&&y>=0&&y<alphas[0].length){
            alphas[x][y]=Math.max(alphas[x][y]-val, 0);
        }
    }
    public void lighten(int x, int y, int val,int depth){
        if (depth>DIFFUSAL_RADIUS){
            return;
        }
        for(int i = -1; i<=1; i++){
            for (int j = -1; j<=1; j++){
                int manhattan_dist = Math.abs(i)+Math.abs(j);
                if (manhattan_dist==0){
                    lighten_point(x, y, val);
                }else{
                    lighten(x+i, y+j, (int)(val/DIFFUSAL_RATE),depth+1);
                }
            }
        }

    }
    public void lighten(int x, int y, int val){
        for(int i = -1; i<=1; i++){
            for (int j = -1; j<=1; j++){
                int manhattan_dist = Math.abs(i)+Math.abs(j);
                if (manhattan_dist==0){
                    lighten_point(x, y, val);
                }else{
                    lighten(x+i, y+j, (int)(val/DIFFUSAL_RATE),1);
                }
            }
        }
    }
    public void circle_light(int cx, int cy, int strength){
        int rad = strength/2;
        for(int x = cx-rad; x<cx+rad; x++){
            for(int y = cy-rad; y<cy+rad; y++){
                lighten(x,y,alphaAsDistance(x, y, cx, cy, rad));
            }
        }
    }
}
