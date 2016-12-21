import sys,os
os.environ["SPARK_CLASSPATH"] = '/usr/local/Cellar/hadoop/2.7.2/spark-2.0.1-bin-hadoop2.7/bin/mysql-connector-java-5.1.40-bin.jar'
import datetime
from pyspark import SparkConf, SparkContext
from pyspark.sql import SparkSession
from pyspark.sql import Row
from pyspark.sql import SQLContext
from pyspark.sql.types import *
from pyspark.sql.functions import col, avg


import mysql 

DB_URL = "jdbc:mysql://localhost:3306/bigdata?user=root"
TABLE_NAME = "bus_records"

def connectToDB(spark):
	df = spark \
		.read \
		.format("jdbc") \
		.option("url", DB_URL) \
		.option("dbtable", TABLE_NAME) \
		.option("driver", "com.mysql.jdbc.Driver") \
		.load()
	return df

def searchByStop(stopId,df):
	rows = df.filter("next_stop_id == %s" %stopId)
	return rows

# Get the source stop id and destination stop id. Then find corresponding times for each trip. And build model from that
# Input: the source stop id, the destination stop id. Current time in format HH:MM:SS. df, the dataframe from mysql
def parseInput(src_id, dest_id, current_hour, df):
	src_rows = searchByStop(src_id,df)
	dest_rows = searchByStop(dest_id,df)
	src_rows = src_rows.select(src_rows.trip_id,src_rows.date_received,src_rows.vehicle_id,src_rows.hour_received.alias('start_hour'), \
		src_rows.min_received.alias('start_min'))
	dest_rows = dest_rows.select(dest_rows.trip_id,dest_rows.date_received,dest_rows.hour_received.alias('end_hour'), \
		dest_rows.min_received.alias('end_min'))
	cond = [src_rows.trip_id == dest_rows.trip_id, src_rows.date_received == dest_rows.date_received]
	totalDurationData = src_rows.join(dest_rows, cond).select(src_rows.vehicle_id,src_rows.start_hour,src_rows.start_min, \
		dest_rows.end_hour,dest_rows.end_min)
	durationOfHour = totalDurationData.filter("start_hour == %s" %current_hour)
	avg_time = 0
	i = 0
	for row in durationOfHour.collect():
		round_time = calculateDuration(row['start_hour'],row['start_min'],row['end_hour'],row['end_min'])
		if not round_time:
			continue
		avg_time += calculateDuration(row['start_hour'],row['start_min'],row['end_hour'],row['end_min'])
		i += 1
	if(avg_time == 0):
		expected_time = 0
	else:
		expected_time = avg_time / i

	# avg_time = durationOfHour.agg(
	# 	(avg(col("end_hour")) - avg(col("start_hour"))) * 60 + (avg(col("end_min"))) - (avg(col("start_min"))))\
	# 	.withColumnRenamed('((((avg(end_hour) - avg(start_hour)) * 60) + avg(end_min)) - avg(start_min))','avg_time')
	# result = avg_time.head()[0]
	# print result
	return expected_time
	# for i in range (0,24):
		# start_hour = trainningData.filter("start_hour == %s" %str(i)).collect()

def calculateDuration(start_hour,start_min,end_hour,end_min):
	if(start_hour > end_hour):
		end_hour += 24
	return (int(end_hour)-int(start_hour))*60 + int(end_min) - int(start_min)

def makePrediction(srcID, destID):
# def makePrediction():
	print srcID, destID
	conf = SparkConf().setAppName("bus prediction")
	sc = SparkContext(conf=conf)
	sqlCtx = SQLContext(sc)
	spark = SparkSession \
        .builder \
        .appName("bus prediction") \
        .getOrCreate()
	df = connectToDB(spark)
	# searchByStop("'MTA_308023'",df)
	now = datetime.datetime.now()
	# result = parseInput("'MTA_903147'","'MTA_401948'",now.hour,df)
	# srcID = "903147"
	# destID = "401948"
	result = parseInput("'MTA_%s'"%srcID,"'MTA_%s'"%destID,now.hour,df)
	spark.stop()
	return result


if __name__ == "__main__":
	try:
		makePrediction()
	except KeyboardInterrupt:
		print "\n"
		sys.exit(1)