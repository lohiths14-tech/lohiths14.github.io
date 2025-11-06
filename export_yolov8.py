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
print("Copy this file to: app\src\main\assets\models\yolov8n.tflite")
