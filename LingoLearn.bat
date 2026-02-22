@echo off
set JAR_PATH=target\lal-0.0.2-SNAPSHOT.jar
if exist %JAR_PATH% (
    echo Starting LingoLearn...
    java -jar %JAR_PATH% --app.ocr.tesseract-path="C:\Program Files\Tesseract-OCR\tesseract.exe"
) else (
    echo Error: JAR file not found at %JAR_PATH%
    echo Please run 'mvn package' first to build the application.
    pause
)
