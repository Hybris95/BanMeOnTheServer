@echo off
javac -Xlint:unchecked -Xlint:deprecation -cp "./class;./jars/craftbukkit.jar" -d "./class" "./src/com/hybris/bukkit/banMeOnTheServer/BanMeOnTheServer.java"
cd ./class
jar cvf "BanMeOnTheServer.jar" ./plugin.yml ./com/
move /Y BanMeOnTheServer.jar ../jars/
pause
