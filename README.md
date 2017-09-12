# CZ3006-Net_Centric_Computing Sliding Window Protocol

This assignment aims to demonstrate the student understanding of the network protocol hierarchy and flow control and error control techniques by implementing a sliding window protocol using an ideal retransmission strategy in a simulated communication network system. The assignment will be build based on Java programming language.
This network simulator will demonstrate and simulate a physical transmission media with 2 communicating virtual machines. The simulator can be operated at 4 levels of quality:
-	Level 0: error-free transmission media
-	Level 1: transmission may lose frames
-	Level 2: transmission may damage frames (generating checksum-errors)
-	Level 3: transmission media may lose or damage frame


## Feature of Sliding Window Protocol
1.	Full duplex communication.
2.	In-order delivery of packets to network layer.
3.	Selective repeat retransmission strategy.
4.	Synchronization with the network layer by granting credits.
5.	Negative acknowledgement.
6.	Separate acknowledgement when the reverse traffic is light or none.

## Setup/Deployment
**Step 1:** Compile SWP.java 
```
javac SWP.java
```

**Step 2:** Start a network simulator by running the command with level of quality(0-3)
```
java NetSim 0
```

**Step 3:** Start two virtual on two different console window
```
java VMach 1
java VMach 2
```

**Step 4:** Verify the two output files: `receive_file_1.txt` & `receive_file_2.txt` is generated. 
If Sliding Window Protocol (Protocol 6) is implemented correctly, `receive_file_1.txt` should be similar to `send_file_2.txt` and vice versa for `receive_file_2.txt` and `send_file_1.txt`

## Author
Thomas Lim Jun Wei

## Acknowledgement
1) Prof Sun for the assignment and guide
2) To all seniors that have previously cleared this assignment

