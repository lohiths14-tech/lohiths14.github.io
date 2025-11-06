# SmartFind Verification Script
Write-Host "`nSmartFind Verification Script v1.0`n" -ForegroundColor Cyan

# Check Gradle wrapper
if (Test-Path ".\gradlew.bat") {
    Write-Host "✅ Gradle wrapper found" -ForegroundColor Green
} else {
    Write-Host "❌ Gradle wrapper missing" -ForegroundColor Red
}

# Check model file
if (Test-Path ".\app\src\main\assets\models\detect.tflite") {
    $size = [math]::Round((Get-Item ".\app\src\main\assets\models\detect.tflite").Length / 1MB, 2)
    Write-Host "✅ Model file found ($size MB)" -ForegroundColor Green
} else {
    Write-Host "❌ Model file missing - run download_model.bat" -ForegroundColor Red
}

# Count source files
$kotlinFiles = (Get-ChildItem -Path ".\app\src\main\java" -Filter "*.kt" -Recurse).Count
Write-Host "✅ Kotlin source files: $kotlinFiles" -ForegroundColor Green

# Count test files
$testFiles = (Get-ChildItem -Path ".\app\src\test" -Filter "*.kt" -Recurse -ErrorAction SilentlyContinue).Count
Write-Host "✅ Unit test files: $testFiles" -ForegroundColor Green

# Count layout files
$layoutFiles = (Get-ChildItem -Path ".\app\src\main\res\layout" -Filter "*.xml" -ErrorAction SilentlyContinue).Count
Write-Host "✅ Layout files: $layoutFiles" -ForegroundColor Green

Write-Host "`nVerification Complete! Run '.\gradlew.bat test' to execute tests" -ForegroundColor Cyan
