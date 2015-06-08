#!/usr/bin/python

from BaseHTTPServer import BaseHTTPRequestHandler
import urlparse

import numpy as np

from sklearn import datasets
from sklearn.cross_validation import StratifiedKFold
from sklearn.externals.six.moves import xrange
from sklearn.mixture import GMM

import pandas
from pandas import DataFrame, Series

from StringIO import StringIO

import json

n_clusters = 3
classifier = GMM(n_components=n_clusters, covariance_type='diag',  min_covar=0.001)

class GetHandler(BaseHTTPRequestHandler):
    
    def do_GET(self):
        #Should not be doing GETS anyway
        self.send_response(404)
        self.end_headers()
        self.wfile.write(message)

    def do_POST(self):
        """Respond to a POST request."""

        # Extract and print the contents of the POST
        length = int(self.headers['Content-Length'])
        print 'length: %d' % (length)
        #post_data = urlparse.parse_qs(self.rfile.read(length).decode())

        #print post_data
        #for key, value in post_data.iteritems():
        #    print "%s=%s" % (key, value)


        df = pandas.read_csv(StringIO(self.rfile.read(length)), sep=" ")
        classifier.fit(df) 

        components = []
        for i in range(n_clusters):
            component = {}
            component["x"] = int(classifier.means_[i][0].tolist())
            component["y"] = int(classifier.means_[i][1].tolist())
            component["weight"] = float("%.3f" % classifier.weights_[i].tolist())
            component["covarX"] = int(classifier.covars_[i][0].tolist())
            component["covarY"] = int(classifier.covars_[i][1].tolist())
            components.append(component)

        self.send_response(200)
        self.send_header("Content-type", "application/json")
        self.end_headers()
        self.wfile.write(json.dumps(components, sort_keys=True))


if __name__ == '__main__':
    from BaseHTTPServer import HTTPServer
    server = HTTPServer(('', 8000), GetHandler)
    print 'Starting server, use <Ctrl-C> to stop'
    server.serve_forever()
