# SmartFind - Download Best Object Detection Models
# Run this script to download high-accuracy models

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "SmartFind Model Downloader" -ForegroundColor Cyan
Write-Host "========================================`n" -ForegroundColor Cyan

$assetsDir = "app\src\main\assets\models"

# Create directory if it doesn't exist
if (!(Test-Path $assetsDir)) {
    New-Item -ItemType Directory -Path $assetsDir -Force | Out-Null
    Write-Host "[✓] Created directory: $assetsDir`n" -ForegroundColor Green
}

# Model Download Options
Write-Host "Available Models:" -ForegroundColor Yellow
Write-Host "1. YOLOv8n (Recommended - Fast & Accurate - 6MB)" -ForegroundColor White
Write-Host "2. EfficientDet-Lite4 (Best Accuracy - 20MB)" -ForegroundColor White
Write-Host "3. SSD MobileNet V2 (Balanced - 6MB)" -ForegroundColor White
Write-Host "4. Download All (Will take time)" -ForegroundColor White
Write-Host ""

$choice = Read-Host "Enter your choice (1-4)"

function Download-Model {
    param (
        [string]$url,
        [string]$filename,
        [string]$description
    )
    
    $outputPath = Join-Path $assetsDir $filename
    
    Write-Host "`n[*] Downloading $description..." -ForegroundColor Yellow
    Write-Host "    URL: $url" -ForegroundColor Gray
    Write-Host "    Saving to: $outputPath" -ForegroundColor Gray
    
    try {
        # Use Invoke-WebRequest with progress
        $ProgressPreference = 'SilentlyContinue'
        Invoke-WebRequest -Uri $url -OutFile $outputPath -UseBasicParsing
        
        if (Test-Path $outputPath) {
            $size = (Get-Item $outputPath).Length / 1MB
            Write-Host "[✓] Downloaded successfully! Size: $([math]::Round($size, 2)) MB`n" -ForegroundColor Green
            return $true
        } else {
            Write-Host "[✗] Download failed!`n" -ForegroundColor Red
            return $false
        }
    }
    catch {
        Write-Host "[✗] Error: $($_.Exception.Message)`n" -ForegroundColor Red
        return $false
    }
}

# Model URLs
$models = @{
    "yolov8n" = @{
        "url" = "https://github.com/ultralytics/assets/releases/download/v8.2.0/yolov8n_saved_model.zip"
        "filename" = "yolov8n.tflite"
        "description" = "YOLOv8 Nano"
        "note" = "Will need extraction from zip"
    }
    "efficientdet" = @{
        "url" = "https://tfhub.dev/tensorflow/efficientdet/lite4/detection/metadata/1?lite-format=tflite"
        "filename" = "efficientdet_lite4.tflite"
        "description" = "EfficientDet Lite4"
    }
    "ssd_mobilenet" = @{
        "url" = "https://storage.googleapis.com/download.tensorflow.org/models/tflite/coco_ssd_mobilenet_v1_1.0_quant_2018_06_29.zip"
        "filename" = "ssd_mobilenet_v2.tflite"
        "description" = "SSD MobileNet V2"
        "note" = "Will need extraction from zip"
    }
}

Write-Host "`n========================================`n" -ForegroundColor Cyan

# Manual Download Instructions
Write-Host "IMPORTANT: Automatic download may not work for all models." -ForegroundColor Yellow
Write-Host "If download fails, please download manually:`n" -ForegroundColor Yellow

Write-Host "Option 1: YOLOv8n (RECOMMENDED - Best for SmartFind)" -ForegroundColor Cyan
Write-Host "1. Visit: https://github.com/ultralytics/ultralytics" -ForegroundColor White
Write-Host "2. Download pre-converted TFLite model" -ForegroundColor White
Write-Host "3. Or use: pip install ultralytics && yolo export model=yolov8n.pt format=tflite" -ForegroundColor White
Write-Host "4. Save as: $assetsDir\yolov8n.tflite`n" -ForegroundColor Green

Write-Host "Option 2: EfficientDet-Lite4" -ForegroundColor Cyan
Write-Host "1. Visit: https://tfhub.dev/tensorflow/lite-model/efficientdet/lite4/detection/metadata/1" -ForegroundColor White
Write-Host "2. Click 'Download' button" -ForegroundColor White
Write-Host "3. Save as: $assetsDir\efficientdet_lite4.tflite`n" -ForegroundColor Green

Write-Host "Option 3: SSD MobileNet (Quick Test)" -ForegroundColor Cyan
Write-Host "1. Visit: https://www.tensorflow.org/lite/examples/object_detection/overview" -ForegroundColor White
Write-Host "2. Download SSD MobileNet V2 model" -ForegroundColor White
Write-Host "3. Save as: $assetsDir\ssd_mobilenet_v2.tflite`n" -ForegroundColor Green

Write-Host "`n========================================`n" -ForegroundColor Cyan

# Try automatic download for EfficientDet as it's most reliable
$tryAuto = Read-Host "Try automatic download for EfficientDet-Lite4? (y/n)"

if ($tryAuto -eq 'y' -or $tryAuto -eq 'Y') {
    Write-Host "`nAttempting automatic download...`n" -ForegroundColor Yellow
    
    $success = Download-Model -url "https://storage.googleapis.com/tfhub-lite-models/tensorflow/lite-model/efficientdet/lite4/detection/metadata/1.tflite" `
                               -filename "efficientdet_lite4.tflite" `
                               -description "EfficientDet-Lite4"
    
    if ($success) {
        Write-Host "`n[SUCCESS] Model downloaded and ready to use!" -ForegroundColor Green
        Write-Host "Rebuild your app and the new model will be automatically detected.`n" -ForegroundColor Green
    }
}

# Alternative: Provide Python script for YOLOv8
Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "Quick Python Script for YOLOv8n Export:" -ForegroundColor Yellow
Write-Host "========================================`n" -ForegroundColor Cyan

$pythonScript = @"
# Install ultralytics
# pip install ultralytics

from ultralytics import YOLO

# Load YOLOv8n model
model = YOLO('yolov8n.pt')

# Export to TFLite (INT8 quantization for mobile)
model.export(
    format='tflite',
    imgsz=640,
    int8=True,
    optimize=True
)

print("Model exported to: yolov8n_saved_model/yolov8n_int8.tflite")
print("Copy this file to: $assetsDir\yolov8n.tflite")
"@

$pythonScript | Out-File -FilePath "export_yolov8.py" -Encoding UTF8
Write-Host "[✓] Created export_yolov8.py`n" -ForegroundColor Green
Write-Host "Run: python export_yolov8.py`n" -ForegroundColor White

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "Next Steps:" -ForegroundColor Yellow
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "1. Place downloaded .tflite model in: $assetsDir" -ForegroundColor White
Write-Host "2. Rebuild the app (Build > Clean Project > Rebuild Project)" -ForegroundColor White
Write-Host "3. Run the app - it will auto-detect the best model!" -ForegroundColor White
Write-Host "4. Check logcat for: 'Using model: models/[modelname].tflite'`n" -ForegroundColor White

Write-Host "[INFO] For best results, use YOLOv8n or EfficientDet-Lite4" -ForegroundColor Cyan
Write-Host "[INFO] Lower confidence threshold in settings if you see too few detections`n" -ForegroundColor Cyan

Write-Host "Press any key to exit..." -ForegroundColor Gray
$null = $Host.UI.RawUI.ReadKey('NoEcho,IncludeKeyDown')
