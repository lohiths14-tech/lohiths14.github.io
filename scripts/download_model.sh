#!/bin/bash

# SmartFind - TensorFlow Lite Model Download Script
# Downloads SSD MobileNet V1 (COCO) model for object detection

set -e

MODEL_DIR="../app/src/main/assets/models"
# Using the TensorFlow Lite Task Vision model (better maintained)
MODEL_URL="https://storage.googleapis.com/download.tensorflow.org/models/tflite/task_library/object_detection/android/lite-model_ssd_mobilenet_v1_1_metadata_2.tflite"
MODEL_FILE="detect.tflite"

echo "SmartFind Model Download Script"
echo "================================"
echo ""

# Create model directory
echo "Creating model directory..."
mkdir -p "$MODEL_DIR"

# Check if model already exists
if [ -f "$MODEL_DIR/$MODEL_FILE" ]; then
    echo "Model file already exists at $MODEL_DIR/$MODEL_FILE"
    read -p "Do you want to re-download? (y/n) " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        echo "Using existing model file."
        exit 0
    fi
fi

echo "Downloading SSD MobileNet V1 model..."
if command -v curl &> /dev/null; then
    curl -L --progress-bar "$MODEL_URL" -o "$MODEL_DIR/$MODEL_FILE"
elif command -v wget &> /dev/null; then
    wget --progress=bar:force "$MODEL_URL" -O "$MODEL_DIR/$MODEL_FILE"
else
    echo "Error: Neither curl nor wget is installed. Please install one of them."
    exit 1
fi

# Verify file was downloaded
if [ ! -f "$MODEL_DIR/$MODEL_FILE" ]; then
    echo "Error: Model file was not downloaded successfully."
    exit 1
fi

# Check file size (should be around 3-4 MB)
FILE_SIZE=$(wc -c < "$MODEL_DIR/$MODEL_FILE")
if [ "$FILE_SIZE" -lt 1000000 ]; then
    echo "Warning: Downloaded file seems too small ($FILE_SIZE bytes). Download may have failed."
    echo "Please check your internet connection and try again."
    exit 1
fi

echo ""
echo "✅ Model downloaded successfully!"
echo "Location: $MODEL_DIR/$MODEL_FILE"
echo "Size: $(numfmt --to=iec-i --suffix=B $FILE_SIZE 2>/dev/null || echo "$FILE_SIZE bytes")"
echo ""
echo "Model Information:"
echo "- Name: SSD MobileNet V1 (COCO)"
echo "- License: Apache 2.0"
echo "- Input: 300x300 RGB image"
echo "- Output: Detections with bounding boxes, classes, and scores"
echo "- Classes: 90 COCO object classes"
echo ""
echo "labelmap.txt is already included in app/src/main/assets/models/"
echo ""
echo "✅ You can now build and run the SmartFind app!"
echo ""
echo "Next steps:"
echo "  cd .."
echo "  ./gradlew assembleDebug"
echo "  ./gradlew installDebug"
