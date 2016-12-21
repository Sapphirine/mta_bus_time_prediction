import sys
import MySQLdb

USER_NAME = "root"
PASSWORD = ""
DB_NAME = "bigdata"
TABLE_NAME = "bus_records"
FILE_PATH = "data/"
db = None
cursor = None


# Connect to Mysql local database with specified username, password and db name 
# Set the global variable of db and cursor to use later 
def connectDB():
	global db 
	db = MySQLdb.connect("localhost",USER_NAME,PASSWORD,DB_NAME)
	global cursor 
	cursor = db.cursor()

# Create the table if not exist with the TABLE_NAME specified
def createTable():
	cursor.execute("CREATE TABLE IF NOT EXISTS %s (Id INT PRIMARY KEY AUTO_INCREMENT, \
		date_received DATE, hour_received TEXT, min_received TEXT, vehicle_id INT, route_id TEXT, trip_id TEXT, next_stop_id TEXT)" % TABLE_NAME)
	db.commit()

# Insert into the table specified above with given time_received, vehicle_id, route_id, trip_id, next_stop_id
# Input: time_received, vehicle_id, route_id, trip_id, next_stop_id corresponds to the fields in MTA historical record data 
def insertEntry(date_received, hour_received, min_received, vehicle_id, route_id, trip_id, next_stop_id):
	cursor.execute("INSERT INTO  %s(date_received,hour_received,min_received,vehicle_id,route_id,trip_id,next_stop_id) \
		VALUES('%s','%s','%s','%s','%s','%s','%s')" % (TABLE_NAME,date_received,hour_received,min_received,vehicle_id,route_id,trip_id,next_stop_id))
	db.commit()

# Read the given file and insert the selected fields from file to DB
# Input: file name of the source file
def readTextFile(fname):
	with open(FILE_PATH+fname) as lines:
		firstLine = True
		for line in lines:
			if(firstLine):
				firstLine = False
				continue
			entries = line.strip().split("\t")
			date = entries[2].split()[0]
			hour = entries[2].split()[1].split(":")[0]
			minute = entries[2].split()[1].split(":")[1]
			insertEntry(date,hour,minute,entries[3],entries[7],entries[8],entries[10])

# Read all 7 MTA bus time historical files and store them into DB one by one
def inputAllData():
	for i in range (1,4):
		filename = "MTA-Bus-Time_.2014-08-0" + str(i) + ".txt"
		print "reading " + filename
		readTextFile(filename)

def main():
	connectDB()
	createTable()
	inputAllData()


if __name__ == "__main__":
	try:
		main()
	except KeyboardInterrupt:
		print "\n"
		sys.exit(1)