"""
Export the ResNet-18 orientation model from .pth to .onnx.

Input:  orientation_cnn.pth
Output: orientation_cnn.onnx

The model expects:
  - Input:  [1, 3, 224, 224] float32, ImageNet-normalized (mean=[0.485,0.456,0.406], std=[0.229,0.224,0.225])
  - Output: [1, 4] logits for classes 0°, 90°, 180°, 270°
"""

import torch
import torchvision
import os

MODEL_PATH = r"D:\Github\rummikub-counter\backend\models\orientation_cnn.pth"
OUTPUT_PATH = r"D:\Github\rummikub-counter-android\app\src\main\assets\orientation_cnn.onnx"

print(f"PyTorch: {torch.__version__}")
print(f"torchvision: {torchvision.__version__}")

# Load model
print("Loading model...")
model = torchvision.models.resnet18(weights=None)
num_ftrs = model.fc.in_features
model.fc = torch.nn.Linear(num_ftrs, 4)  # 4-class classifier
checkpoint = torch.load(MODEL_PATH, map_location="cpu", weights_only=True)
# The checkpoint wraps the state_dict; extract it
if "model_state_dict" in checkpoint:
    state_dict = checkpoint["model_state_dict"]
    print(f"Checkpoint metadata: val_acc={checkpoint.get('val_acc')}, imgsz={checkpoint.get('imgsz')}, orientations={checkpoint.get('orientations')}")
else:
    state_dict = checkpoint
model.load_state_dict(state_dict)
model.eval()
print("Model loaded.")

# Export to ONNX
dummy_input = torch.randn(1, 3, 224, 224)

print("Exporting to ONNX...")
torch.onnx.export(
    model,
    dummy_input,
    OUTPUT_PATH,
    input_names=["input"],
    output_names=["output"],
    dynamic_axes={"input": {0: "batch"}, "output": {0: "batch"}},
    opset_version=17,
)

print(f"Exported to: {OUTPUT_PATH}")
print(f"Size: {os.path.getsize(OUTPUT_PATH) / (1024*1024):.1f} MB")

# Validate
import onnx
onnx_model = onnx.load(OUTPUT_PATH)
onnx.checker.check_model(onnx_model)
print("ONNX model validation passed.")
