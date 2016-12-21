# busprediction

The repository contains code for both front end android APP and backend python flask server. 

To start the server:

1. Create a directory named data under current directory. Then put the MTA historical data into it.

2. Start MySQL server on local machine, default user root password none. run the mysql.py script to parse raw txt file into MySQL database.

3. After the data is successfully loaded, make sure pyspark is correctly installed. 

4. Run app.py script, which should start the flask server at port 5000.

5. Use any feasible solution to expose the machine into global internet.


Android side:

1. In the MainActivity.java file, make sure the url matches the server url.

2. Compile and install the apk on an android phone with SDK higher than 23.

3. Make sure Location service, Internet permission and write external storage permission is allowed for the APP

4. Enter the souce bus stop ID and destionation stop ID, press the button and it should draw a route of the bus route and return the estimated travel time.

5. The client server connection may takes some time depending on the internet condition.