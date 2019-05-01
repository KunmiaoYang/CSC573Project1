Team:
Name 1: Kunmiao Yang, Student ID 1: 200200409
Name 2: Mengyan Dong, Student ID 2: 200284681

Compile:
This code is developed by Java + IntelliJ. To compile it, you need to install JDK 1.8 x64 and IntelliJ on your machine. The projects contains 2 artifacts: server.jar and client.jar. After compilation, you will find them under the out/artifacts directory. 
Before compile you need to configure the user name and password for the database you installed in your server machine. It is located under the resources/db/account.properties.example file. After configure this file, rename it to account.properties file so the program can read it.

Deployment for server:
The server will need MySQL in order to save and retrieve data. Therefore, MySQL is required in the server machine. JRE/JDK 1.8 x64 is also required to run the server program. 
After install and initialize the database, you will need to add two tables to it. Use the schema.sql file to create the tables.
Then you will be able to deploy the server with the compiled server.jar file with this command:
java -jar server.jar
The port used is fixed to 7734, so make sure the port is not occupied before starting the server.

Running client:
Before running a client, you need to have a directory for it to store the RFC files. This directory should contain only RFC files, including txt or pdf RFC files. Each file should be named as: 
	"<RFC#> <RFC title>.txt"
where <RFC#> is 4 digits number of this RFC, and <RFC title> is the title of RFC. Note that there is a space between them. For example: "0001 Host Software.txt", "0009 Host Software.pdf". This directory can be empty, but make sure at least one of your client has RFC for the purpose of test.
When the directory is ready, use this command line to run a client:
java -jar client.jar -s <host> -l <local store>
where <host> is the server's IP address, you can also use host name if there is DNS for it. <local store> is the directory we talked about above.
The client has 2 threads: one connects with server, and the other provides P2P service for other clients. Since the client will automatically allocate ports for both threads and send the port info to server, the whole system should be tested inside the same Private network, or the port might change outside, and the P2P might not be able to access the correct port.