from flask import Flask, request
app = Flask(__name__)
import sys
import busprediction

@app.route('/')
def main():
	return "ok"

@app.route('/gettime',methods=['POST'])
def gettime():
	srcID = request.form['srcID']
	destID = request.form['destID']
	result = busprediction.makePrediction(srcID,destID)
	print result
	return str(result)


if __name__ == '__main__':
	try:
		app.run()
	except KeyboardInterrupt:
		print "\n"
		sys.exit(1)