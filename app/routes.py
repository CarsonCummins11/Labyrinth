from app import db,app,login,User,game
from flask import render_template,request,jsonify
from flask_login import current_user, login_user, login_required,logout_user
import json
from time import time
m = game.Map()
@app.route("/")
def main():
    return render_template("home.html")
@app.route("/gamekey",methods=["POST"])
def gamekey():
    username = request.get_json(force=True)["username"]
    password = request.get_json(force=True)["password"]
    k = game.generate_gamekey(username,password)
    return jsonify(k)
@app.route("/refresh",methods=["POST"])
def refresh():
    t0 = time()
    st = game.get_relative_state(request.get_json(force=True),m)
    print(time()-t0)
    return jsonify(st)
    
