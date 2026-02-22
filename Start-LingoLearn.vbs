Set WshShell = CreateObject("WScript.Shell")
' Run the start.bat script silently (0 means hidden window, False means don't wait to finish)
WshShell.Run "cmd /c """"start.bat"" """, 0, False

' Wait explicitly for 8 seconds to give Spring Boot time to start up before opening the browser
WScript.Sleep 8000

' Open the application in the default web browser
WshShell.Run "http://localhost:8080/learn.html"
