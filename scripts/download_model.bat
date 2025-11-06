@echo off
REM SmartFind - TensorFlow Lite Model Download Script (Windows)
REM Downloads SSD MobileNet V1 (COCO) model for object detection

echo SmartFind Model Download Script
echo ================================
echo.

set MODEL_DIR=..\app\src\main\assets\models
set MODEL_URL=https://storage.googleapis.com/download.tensorflow.org/models/tflite/task_library/object_detection/android/lite-model_ssd_mobilenet_v1_1_metadata_2.tflite
set MODEL_FILE=detect.tflite

REM Create model directory
echo Creating model directory...
if not exist "%MODEL_DIR%" mkdir "%MODEL_DIR%"

REM Check if model already exists
if exist "%MODEL_DIR%\%MODEL_FILE%" (
    echo Model file already exists at %MODEL_DIR%\%MODEL_FILE%
    set /p REPLY="Do you want to re-download? (y/n): "
    if /i not "%REPLY%"=="y" (
        echo Using existing model file.
        goto :end
    )
)

echo Downloading SSD MobileNet V1 model...
powershell -Command "& {$ProgressPreference = 'SilentlyContinue'; Invoke-WebRequest -Uri '%MODEL_URL%' -OutFile '%MODEL_DIR%\%MODEL_FILE%' -ErrorAction Stop}"

REM Verify file was downloaded
if not exist "%MODEL_DIR%\%MODEL_FILE%" (
    echo Error: Model file was not downloaded successfully.
    echo Please check your internet connection and try again.
    pause
    exit /b 1
)

REM Check file size
for %%A in ("%MODEL_DIR%\%MODEL_FILE%") do set FILE_SIZE=%%~zA
if %FILE_SIZE% LSS 1000000 (
    echo Warning: Downloaded file seems too small (%FILE_SIZE% bytes^). Download may have failed.
    echo Please check your internet connection and try again.
    pause
    exit /b 1
)

echo.
echo Model downloaded successfully!
echo Location: %MODEL_DIR%\%MODEL_FILE%
echo Size: %FILE_SIZE% bytes
echo.
echo Model Information:
echo - Name: SSD MobileNet V1 (COCO)
echo - License: Apache 2.0
echo - Input: 300x300 RGB image
echo - Output: Detections with bounding boxes, classes, and scores
echo - Classes: 90 COCO object classes
echo.
echo labelmap.txt is already included in app\src\main\assets\models\
echo.
echo You can now build and run the SmartFind app!
echo.
echo Next steps:
echo   cd ..
echo   gradlew.bat assembleDebug
echo   gradlew.bat installDebug
echo.

:end
pause
