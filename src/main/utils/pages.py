import flask
from flask import Flask, render_template, request, redirect, url_for, session, flash
import os
import subprocess
import json

with open('User.json', 'r') as f:
    user = json.load(f)

tokens = ['c6d0c20b-15b4-46f9-b7ef-b1ea54097b95']
repo_base_address = '/home/ooad/repo-store'
server_base_address = '/home/ooad/pages'

app = Flask(__name__)
app.secret = os.urandom(24)


# get route parameter
@app.route('/pages/<username>/<reponame>/', methods=['GET'])
@app.route('/pages/<username>/<reponame>/<path:path>', methods=['GET'])
def get_user_repo(username, reponame, path=None):
    print(username, reponame, path)
    if path is None or path == '':
        path = 'index.html'
    if path.startswith('.git'):
        response = flask.Response(status=403, response='Forbidden')
        return response
    return flask.send_from_directory(f'''{server_base_address}/{username}/{reponame}/''', path)
    # serve with the folder in /username/reponame


@app.route('/pages/<path:path>')
def index(path):
    return flask.send_from_directory('/home/dgy/github/testHtml', path)


@app.route('/pages/api/configure', methods=['POST'])
def configure():
    data = request.get_json()
    if data['token'] not in tokens:
        response = flask.Response(status=403, response='token error')
        return response
    if data['option'] == 'enable':
        if data['userName'] not in user:
            user[data['userName']] = {}
        user[data['userName']][data['repoName']] = data['repoId']
        user[data['userName']]['userId'] = data['userId']
        with open('User.json', 'w') as f:
            json.dump(user, f)
        result = subprocess.getoutput(
            f'''git clone {repo_base_address}/{data['userId']}/{data['repoId']} {server_base_address}/{data['userName']}/{data['repoName']}''')
        print(result)
        response = flask.Response(status=200, response='success')
        return response
    elif data['option'] == 'disable':
        if data['userName'] in user and data['repoName'] in user[data['userName']]:
            del user[data['userName']][data['repoName']]
            with open('User.json', 'w') as f:
                json.dump(user, f)
            result = subprocess.getoutput(
                f'''rm -rf {server_base_address}/{data['userName']}/{data['repoName']}''')
            print(result)
            response = flask.Response(status=200, response='success')
            return response
        else:
            response = flask.Response(status=404, response='not found')
            return response

    # check if the user exists


if __name__ == '__main__':
    app.run(port=8082)
