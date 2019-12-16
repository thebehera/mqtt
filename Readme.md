[![Build Status](https://travis-ci.org/thebehera/mqtt.svg?branch=master)](https://travis-ci.org/thebehera/mqtt)


This is currently a WIP project with the goal of creating a lightweight, performant, MQTT3.1.1 & 5 easy to use library with two simple APIs

 
 
# Prerequisites 
- [Install JDK (at least java 8)](https://www.oracle.com/technetwork/java/javase/downloads/index.html)
   - Used for the gradle build process
- [Install Mosquitto](https://mosquitto.org/download/)
   - Used for client integration tests
   - Windows
       - TBD
   - Mac 
       - `brew install mosquitto` #Install mosquitto using [Homebrew](https://brew.sh/)
       - `echo "" >> ~/.bashrc`  # Add a new line to your bashrc
       - `echo export mosquitto=/usr/local/sbin/mosquitto >> ~/.bashrc` # Add mosquitto to your path so gradle can pick up the integration tests
   - Linux 
       - Ubuntu
            - `sudo apt-add-repository ppa:mosquitto-dev/mosquitto-ppa` # Add mosquitto's dev as the source of truth for the mosquitto package
            - `sudo apt-get update` # update local references to packages with the newly added PPA
            - `sudo apt-get install mosquitto` # install mosquitto and add it to your path
       - Snap `snap install mosquitto`
- [Install Chrome] (https://www.google.com/chrome/)
    - Used for headless unit testing browser JS code

