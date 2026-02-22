@echo off
setlocal enabledelayedexpansion

echo ========================================================
echo   Tesseract OCR Setup for Windows (Eng + Hin + Tel)
echo ========================================================

:: 1. Check for winget
where winget >nul 2>nul
if %ERRORLEVEL% neq 0 (
    echo [ERROR] winget is not installed. Please install it from the Microsoft Store.
    pause
    exit /b 1
)

:: 2. Install Tesseract
echo [1/3] Installing Tesseract OCR via winget...
winget install UB.TesseractOCR --accept-source-agreements --accept-package-agreements
if %ERRORLEVEL% neq 0 (
    echo [ERROR] Failed to install Tesseract.
    pause
    exit /b 1
)

:: 3. Define paths (Default installation path)
set TESSDATA_PATH=C:\Program Files\Tesseract-OCR\tessdata

:: 4. Download Language Packs (Eng is usually included, but downloading high-quality best models)
echo [2/3] Downloading High-Quality (tessdata_best) models...

:: Function to download if missing (simplified for batch)
echo Downloading Hindi (hin)...
curl -L -o "!TESSDATA_PATH!\hin.traineddata" https://github.com/tesseract-ocr/tessdata_best/raw/main/hin.traineddata
if %ERRORLEVEL% neq 0 echo [WARN] Failed to download Hindi pack.

echo Downloading Telugu (tel)...
curl -L -o "!TESSDATA_PATH!\tel.traineddata" https://github.com/tesseract-ocr/tessdata_best/raw/main/tel.traineddata
if %ERRORLEVEL% neq 0 echo [WARN] Failed to download Telugu pack.

echo Downloading English (eng)...
curl -L -o "!TESSDATA_PATH!\eng.traineddata" https://github.com/tesseract-ocr/tessdata_best/raw/main/eng.traineddata
if %ERRORLEVEL% neq 0 echo [WARN] Failed to download English pack.

:: 5. Success message
echo [3/3] Configuration Complete!
echo.
echo ========================================================
echo   Tesseract Path: C:\Program Files\Tesseract-OCR\tesseract.exe
echo   Languages: eng+hin+tel
echo ========================================================
echo.
echo To run your application, use:
echo java -jar your-app.jar --app.ocr.tesseract-path="C:\Program Files\Tesseract-OCR\tesseract.exe" --app.ocr.languages=eng+hin+tel
echo.
pause
