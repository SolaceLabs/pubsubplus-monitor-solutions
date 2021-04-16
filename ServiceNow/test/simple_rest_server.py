#!/usr/bin/python
# simple_rest_server.py
#    Simple REST Server
# A simple server to test ServiceNow integration
# Running:
#    python simple-rest-flask.py -- (try python3.6 if this doesn't work)
#    starts at 127.0.0.1:3000
# Usage:
#    Posting an event:
#       post JSON record to: 
#       127.0.0.1:3000/post
#       see test/send_set_event.sh
#    To list one resource:
#       curl -v http://localhost:3000/list/<event-name>
#       eg: curl -v http://localhost:3000/list/test
#    To list all resources:
#       curl -v http://localhost:3000/list/all
# nram, solace psg
# Jun 25, 2019

from flask import Flask
from flask_restful import Api, Resource, reqparse
import json
import pprint
import argparse


me="simple_rest_server"
myver="0.1"

app = Flask(__name__)
api = Api(app)

events = [
    {
        "resource": "test",
        "node": "test",
        "type": "test",
        "source": "test",
        "severity": "2"
    }
]


class ListEvent(Resource):
    def get(self, r):
        print ("ListEvent get: {}".format(r))

        pp = pprint.PrettyPrinter(indent=4)
        print ("Events:\n")
        print (pp.pprint(events))
        if r == "all":
            return events, 200

        for e in events:
            if (r == e["resource"]):
                return e, 200

        return "Event not found", 404


class AddEvent(Resource):
    def post(self):
        print ("AddEvent post: ")

        pp = pprint.PrettyPrinter(indent=4)

        parser = reqparse.RequestParser()
        parser.add_argument("records")
        args = parser.parse_args()
        r = args["records"]
        # print ("record: {} (type: {})".format(records, type(records)))
        r1 = args["records"].replace('\'', '"').replace('u"', '"')
        # print ("r1: {} (type: {})".format(r1, type(r1)))
        r2 = json.loads(r1)
        # print ("r2: {} (type: {})".format(r2), type(r2)))
        #if r.verbose > 0 :
        print (pp.pprint(r2))

        # r = r2["resource"]
        # for e in events:
        #    if(r == e["resource"]):
        #        return "Event with resource {} already exists ({})".format(r, args["records"]), 400

        events.append(r2)
        return r, 201

# -----------------------------------------------------------------
# main
# -----------------------------------------------------------------


p = argparse.ArgumentParser(prog=me,
                            description='simple_rest_server',
                            formatter_class=argparse.RawDescriptionHelpFormatter)

po = p.add_argument_group("Optional")
po.add_argument('--ip', action="store", default="127.0.0.1", help='IP to start service')
po.add_argument('--port', action="store", default="3000", help='port to start service')
po.add_argument('-v', '--verbose', action="count", help='Verbose mode')

r = p.parse_args()

print ("Starting REST service at: {}:{} ...".format(r.ip, r.port))

api.add_resource(AddEvent, "/post")
api.add_resource(ListEvent, "/list/<string:r>")
app.run(debug=False, host=r.ip, port=int(r.port))
