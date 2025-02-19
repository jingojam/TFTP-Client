Araño, Christian Timothy Z.
12203211
NSCOM01 - S12
MP 1
Project Features
Class Structure
The project is divided into multiple classes to organize the structure of the program efficiently.
These classes include:
● TFTPClient
Handles the UI, user interactions, inputs, and core TFTP file operations.
● WRRQPacket
Handles the creation of Write Request (WRQ) and Read Request (RRQ) packets.
● DATAPacket
Handles the creation of DATA packets for file transfer.
● ACKPacket
Handles the creation of Acknowledgment (ACK) packets.
● ERRORPacket
Handles the creation of ERROR packets.
● OACKPacket
Handles the creation of Option Acknowledgment (OACK) packets.
This modularized structure improves the clarity and maintainability of the code, making
implementation more intuitive.
Base TFTP File Operations and Compliance
The project adheres to the Trivial File Transfer Protocol (TFTP) specifications as outlined in the
following RFCs:
● RFC 1350 – Basic TFTP Protocol
● RFC 2347 – TFTP Option Extension
● RFC 2348 – TFTP Blocksize Option
● RFC 2349 – TFTP Timeout and Transfer Size Options
The core operations include:
● Reading files from the server
- Constructing and sending Read Request (RRQ) packets to initiate a file retrieval
from the server.
● Receiving files from the server
- The client processes incoming DATA packets and sends appropriate ACK
packets to confirm blocks.
● Writing files to the server
- Constructing and sending Write Request (WRQ) packets to initiate a write
request to the server, and construction and sending of DATA packets to the
server for file writing.
Error Handling Capabilities
The project implements error handling for various scenarios, including:
● Error packets received for transaction termination
● Transaction errors
● Missing or inaccessible files
Compiling and Running
!!! IMPORTANT !!!
- I am using Java 21, make sure that you have Java 21.
- Here is my specific version:
openjdk 21.0.4 2024-07-16 LTS
OpenJDK Runtime Environment Microsoft-9889606 (build 21.0.4+7-LTS)
OpenJDK 64-Bit Server VM Microsoft-9889606 (build 21.0.4+7-LTS, mixed mode,
sharing)
- Verify if your version is compatible by using java –version command in cmd.
- In any case, should you wish, you may opt to install and setup Java 21.0.4 LTS
from https://learn.microsoft.com/en-us/java/openjdk/download#openjdk-21
- There are 2 .jar files. One is compiled using Java 21, and I made one specifically
compiled using Java 11 (in case you have an older version as mine). You may use
either of the files for running the program.
- Generally, the .jar file compiled in Java 11 should run if you have a higher version
(such as Java 17, Java 21).
Instructions for compiling and running the program:
1) Go to the “dist” directory of the project in CMD.
2) You may opt to choose from running either of these commands:
● To run the file compiled in Java 21, run: java -jar "MP_1.jar"
● To run the file compiled in Java 11, run: java -jar "MP_1_11.jar"
OR, you may do this:
1) Go to the src directory (the directory of the project with the source code) in CMD.
2) Run these commands:
● Run this command first: javac TFTP.java
● And then, run this command: java TFTP.java
Note:
- TFTP.java contains the main method.
Test Cases
NOTE:
- I included wireshark packet captures for the output, for verification purposes.
Sample Test Case Output
Downloading a file
Uploading a file
Downloading a file
with blksize set
Downloading a file
with tsize set
Downloading a file
with both options
set
Uploading a file
with blksize set
Uploading a file
with tsize set
Uploading a file
with both options
set
