from flask import Flask #Web application framework
from pymongo import MongoClient #Python library for MongoDB
from flask_login import LoginManager #For managing flask, handles logging in, out, and remembering sessions(cookies wooooo)
import os

client = MongoClient() #Creates the MongoDB instance
db = client['labyrinth'] 
app = Flask(__name__, static_url_path='/static')#Instane of a Flask class, uses __name__ since we're using a single module
login=LoginManager(app) #init LoginManager configured with the flask app(must use flask app)
app.config['SECRET_KEY'] = os.urandom(20).hex() #secret for the app, used for authentication of sessions
app.config['TEMPLATES_AUTO_RELOAD'] = True
from app import routes