jkobuki-2
=========

This libary replaces the old jkobuki library.

A pure Java implementation of the serial protocol to use a Yujin Kobuki robot. This library utilizes the new Java Simple Serial Connector library, [jSSC](https://code.google.com/p/java-simple-serial-connector/).

Ensure that you have installed the jSSC library on your system. The Kobuki robot will manifest itself as a serial port on your chose OS. In Linux it shows up as `/dev/ttyUSB*` where the `*` represents the integer value the Linux kernel assigns.

The following is an example of the initialization of a KobukiRobot on Linux:  
   
    KobukiRobot megatron = new KobukiRobot("/dev/ttyUSB0"); 
    
Once this has been implemented, data will immediately begin being gathered.  For movement, the `baseControl()` method in
KobukiRobot must be called:  

    megatron.baseControl((short) 100, (short) 0); 
    
The first value is the desired speed of the robot in mm/s (millimeters per second). The second value is the desired
turn radius of the robot in mm (millimeters). The "short" casts are needed because the packet data allocation for the velocity and radius is two bytes (one for each value).
   
The currently implemented commands for the robot are baseControl (movement) and soundSequence (sound). However, the sound command is untested.  More commands may be added in the future. Regarding data, all gathered information is available on request from the KobukiRobot.

## Clojure Interop

Since this driver is pure Java, we can use it within Clojure! Simply fire up an namespace and import:

    (ns jkobuki-clj
     (:import edu.ycp.robotics KobukiRobot))
      
    (def robot (KobukiRobot. "/dev/ttyUSB0"))
    (baseControl. robot (short 100) (short 0))
