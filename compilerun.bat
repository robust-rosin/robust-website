@echo off
:start
javac generator.java
java -cp . generator
pause
goto start