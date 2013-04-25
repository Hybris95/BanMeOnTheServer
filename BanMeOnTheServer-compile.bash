#!/bin/bash
javac -Xlint:unchecked -Xlint:deprecation -cp "./class:./jars/craftbukkit.jar" -d "./class" "./src/com/hybris/bukkit/banMeOnTheServer/BanMeOnTheServer.java"
cd ./class
jar cvf "BanMeOnTheServer.jar" ./plugin.yml ./com/
mv BanMeOnTheServer.jar ../jars/
