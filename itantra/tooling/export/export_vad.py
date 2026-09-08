import os
import urllib.request
import onnxruntime

def export_vad():
    # Make sure we create the output directory in the android app
    output_dir = os.path.join(os.path.dirname(__file__), "..", "..", "android", "app", "src", "main", "assets")
    os.makedirs(output_dir, exist_ok=True)
    
    # V4 of Silero VAD is distributed directly as an ONNX model
    url = "https://github.com/snakers4/silero-vad/raw/master/src/silero_vad/data/silero_vad.onnx"
    output_path = os.path.join(output_dir, "silero_vad.onnx")
    
    print(f"Downloading Silero VAD to {output_path}...")
    urllib.request.urlretrieve(url, output_path)
    
    # Verify it loads
    session = onnxruntime.InferenceSession(output_path)
    print("VAD model successfully downloaded and loaded by ONNX Runtime.")

if __name__ == "__main__":
    export_vad()
