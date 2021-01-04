from app import db
import secrets
from werkzeug.security import check_password_hash
import math
import numpy as np
import json
from time import time
MAX_SPEED = 2
PLAYER_WIDTH = 25
PLAYER_HEIGHT = 50
MAP_PATH = 'Map.txt'
#padding flashlight so stuff gets loaded before player views it
FLASHLIGHT_ARC = 70
FLASHLIGHT_STR = 330
RAY_STEP=40
ARC_STEP=5
#grabbed this from stack overflow
def do_polygons_intersect(a, b):
    """
 * Helper function to determine whether there is an intersection between the two polygons described
 * by the lists of vertices. Uses the Separating Axis Theorem
 *
 * @param a an ndarray of connected points [[x_1, y_1], [x_2, y_2],...] that form a closed polygon
 * @param b an ndarray of connected points [[x_1, y_1], [x_2, y_2],...] that form a closed polygon
 * @return true if there is any intersection between the 2 polygons, false otherwise
    """

    polygons = [a, b]
    minA, maxA, projected, i, i1, j, minB, maxB = None, None, None, None, None, None, None, None

    for i in range(len(polygons)):

        # for each polygon, look at each edge of the polygon, and determine if it separates
        # the two shapes
        polygon = polygons[i]
        for i1 in range(len(polygon)):

            # grab 2 vertices to create an edge
            i2 = (i1 + 1) % len(polygon)
            p1 = polygon[i1]
            p2 = polygon[i2]

            # find the line perpendicular to this edge
            normal = { 'x': p2[1] - p1[1], 'y': p1[0] - p2[0] }

            minA, maxA = None, None
            # for each vertex in the first shape, project it onto the line perpendicular to the edge
            # and keep track of the min and max of these values
            for j in range(len(a)):
                projected = normal['x'] * a[j][0] + normal['y'] * a[j][1]
                if (minA is None) or (projected < minA): 
                    minA = projected

                if (maxA is None) or (projected > maxA):
                    maxA = projected

            # for each vertex in the second shape, project it onto the line perpendicular to the edge
            # and keep track of the min and max of these values
            minB, maxB = None, None
            for j in range(len(b)): 
                projected = normal['x'] * b[j][0] + normal['y'] * b[j][1]
                if (minB is None) or (projected < minB):
                    minB = projected

                if (maxB is None) or (projected > maxB):
                    maxB = projected

            # if there is no overlap between the projects, the edge we are looking at separates the two
            # polygons, and we know there is no overlap
            if (maxA < minB) or (maxB < minA):
                return False
    return True
def generate_gamekey(u,p):
    prof = db['players'].find_one({'username':u})
    if prof is None:
        return None
    if(check_password_hash(prof['password'],p)):
        tok = secrets.token_urlsafe(64)
        db['players'].update({'username':u},{'$set':{'gamekey':tok}})
        return {'key':tok}
def get_dist(p1,p2):
    return math.sqrt( ((p1[0]-p2[0])**2)+((p1[1]-p2[1])**2) )
def get_relative_state(d,mp):
    prof = db['players'].find_one({'gamekey':d['key']})
    if prof is None:
        print('player not found')
        return None
    x = int(prof['x'])
    y = int(prof['y'])
    direction = prof['direction']
    mc = json.loads(d['player'])
    v_width = int(mc['view_width'])
    v_height = int(mc['view_height'])
    '''
    if not (get_dist([x,y],[float(mc['x']),float(mc['y'])]) <=MAX_SPEED*5 and 0<=float(mc['speed'])<=MAX_SPEED):
        print('player moved too fast')
        print(str(x)+','+str(y)+'    '+str(mc['x'])+','+str(mc['y']))
        return None
    if len(map.get_objects_in(build_hitbox(x,y,direction,PLAYER_WIDTH,PLAYER_HEIGHT)))>1:
        print('player exists in illegal area')
        return None
    '''
    db['players'].update({'gamekey':d['key']},{'$set':{'x':float(mc['x']),'y':float(mc['y']),'speed':float(mc['speed']),'direction':float(mc['direction'])}})
    ret_l=[]
    t0 = time()
    light = Light(x,y,direction,FLASHLIGHT_STR,FLASHLIGHT_ARC,mp)
    t1 = time()
    for o in mp.objects:
        if mp.islit(o,additional=[light]):
            ret_l.append(o.to_json())
    for o in db['players'].find():
        if(o['username'] == prof['username']):
            continue
        pp=Obj(int(o['x']),int(o['y']),int(o['direction']),int(o['speed']),PLAYER_WIDTH,PLAYER_HEIGHT,'player',o['username'])
        if mp.islit(pp,additional=[light]):
            ret_l.append(pp.to_json())
    print('penis '+str(t1-t0))
    ret = {}
    for i in range(len(ret_l)):
        ret[str(i)] = ret_l[i]
    return ret
def build_hitbox(x,y,direction,width,height):
    origin = (x+width/2,y+width/2)
    ret = [0,0,0,0]
    ret[0] = rotate(origin,(x,y),direction)
    ret[1] = rotate(origin,(x+width,y),direction)
    ret[2] = rotate(origin,(x+width,y+height),direction)
    ret[3] = rotate(origin,(x,y+height),direction)
    return ret
def rotate(origin, point, ang):
    angle = math.radians(ang)
    ox, oy = origin
    px, py = point

    qx = ox + math.cos(angle) * (px - ox) - math.sin(angle) * (py - oy)
    qy = oy + math.sin(angle) * (px - ox) + math.cos(angle) * (py - oy)
    return qx, qy
class Light:
    def __init__(self,xx,yy,direct,stre,ar,on):
        self.x = xx
        self.y = yy
        self.direction = direct
        self.strength = stre
        self.arc = ar
        self.poly = []
        if (self.arc<360):
            self.poly = self.create_poly(on)
        else:
            self.poly = self.create_circle(on)
    @staticmethod
    def from_string_array(lin):
        return Light(int(lin[1]),int(lin[2]),int(lin[3]),int(lin[4]),int(lin[5]))
    def update_poly(self,on):
        if(self.arc<360):
            self.poly = self.create_poly(on)
        else:
            self.poly = self.draw_circle(on)
    def create_circle(self,on):
        rad = self.strength/2
        ret = []
        for i in range(0,360,ARC_STEP):
            xx = rad*math.cos(math.radians(i))
            yy = rad*math.sin(math.radians(i))
            ret.append((xx,yy))
        return ret
    def create_poly(self,on):
        ret = []
        for i in range(int(self.direction-self.arc/2),int(self.direction+self.arc/2),ARC_STEP):
            ret.append(self.raycast(i,on))
        ret.append((int(self.x),int(self.y)))
        return ret
    def raycast(self,direct,on):
        for i in range(0,int(self.strength),RAY_STEP):
            xx = i*math.cos(math.radians(direct))+self.x
            yy = i*math.sin(math.radians(direct))+self.y
            if len(on.get_objects_at(xx,yy))>0:
                return (int(xx),int(yy))
        return (int((self.strength)*math.cos(direct)+self.x),int((self.strength)*math.sin(direct)+self.y))
class Map:
    def __init__(self):
        self.objects = []
        self.lights = []
        #load objects and lights from map file
        fl = ''
        with open(MAP_PATH,'r') as ff:
            fl = ff.readline().strip()
        file = open(MAP_PATH,'r')
        for line in file:
            inf = line.split()
            if(inf[0]=='L'):
                self.lights.append(Light.from_string_array(inf))
            elif(inf[0]=='O'):
                self.objects.append(Obj.from_string_array(inf))
            else:
                continue
    def islit(self,o,additional = []):
        hit = o.get_hitbox()
        for l in self.lights:
            if(do_polygons_intersect(hit,l.poly)):
                return True
        for l in additional:
            if(do_polygons_intersect(hit,l.poly)):
                return True
        return False

    def get_objects_at(self,x,y):
        ret = []
        for o in self.objects:
            if o.covers_point(x,y):
                ret.append(o)
        for o in db['players'].find():
            pp=Obj(int(o['x']),int(o['y']),int(o['direction']),int(o['speed']),PLAYER_WIDTH,PLAYER_HEIGHT,'player',o['username'])
            if pp.covers_point(x,y):
                ret.append(pp)
        return ret
    def get_objects_in(self,hit):
        ret = []
        for o in self.objects:
            if o.covered_by(hit):
                ret.append(o)
        for o in db['players'].find():
            pp=Obj(int(o['x']),int(o['y']),int(o['direction']),int(o['speed']),PLAYER_WIDTH,PLAYER_HEIGHT,'player',o['username'])
            if pp.covered_by(hit):
                ret.append(pp)
        return ret
        
class Obj:
    def __init__(self,x,y,direction,speed,width,height,typ,ident):
        self.x = x
        self.y = y
        self.speed = speed
        self.direction = direction
        self.width = width
        self.height = height
        self.t = typ
        self.id = ident
        self.hitbox = None
    def get_hitbox(self):
        if(self.hitbox is not None):
            return self.hitbox
        self.hitbox = build_hitbox(self.x,self.y,self.direction,self.width,self.height)
        return self.hitbox
    @staticmethod
    def from_string_array(lin):
        return Obj(int(lin[1]),int(lin[2]),int(lin[3]),int(lin[4]),int(lin[5]),int(lin[6]),lin[7],lin[8])
    @staticmethod
    def tri_area(A,B,C):
        return abs( (B[0] * A[1] - A[0] * B[1]) + (C[0] * B[1] - B[0] * C[1]) + (A[0] * C[1] - C[0] * A[1]) ) / 2
    def covers_point(self,xx,yy):
        hb = build_hitbox(self.x,self.y,self.direction,self.width,self.height)
        P =(xx,yy)
        A = hb[0]
        B = hb[1]
        C = hb[2]
        D = hb[3]
        area = Obj.tri_area(A, P, D)+Obj.tri_area(D, P, C)+Obj.tri_area(C, P, B)+Obj.tri_area(P, B, A)
        return False if area>self.width*self.height else True
    def covered_by(self,hit):
        return do_polygons_intersect(self.get_hitbox(),hit)
    def to_json(self):
        return {'dat_type':'object','x':self.x,'y':self.y,'direction':self.direction,'width':self.width,'height':self.height,'type':self.t,'id':self.id, 'speed':self.speed}
