import time
import numpy as np
import sys

def print_header(text):
    print(f"\n{'='*60}\n{text}\n{'='*60}")

def demo_pipeline():
    print_header("iTantra End-to-End Pipeline Demo")
    
    source_lang = "eng_Latn"
    target_lang = "hin_Deva"
    tactical_phrase = "Enemy spotted at grid reference"
    
    print(f"Scenario: Soldier A (English) speaking to Soldier B (Hindi)")
    time.sleep(1.5)

    # ---------------------------------------------------------
    print_header("DEVICE A: TRANSMITTER PIPELINE")
    print("[AudioCapture] Capturing 16kHz PCM audio stream...")
    time.sleep(1)
    
    print("[VadProcessor] Silero VAD analyzing chunks...")
    time.sleep(1)
    print(f"[VadProcessor] SPEECH_START detected.")
    time.sleep(0.5)
    print(f"[VadProcessor] SPEECH_END detected. Utterance length: 48,000 samples (3.0s)")
    
    print(f"\n[AsrEngine] Offline IndicConformer processing audio...")
    time.sleep(1.5)
    confidence = 0.92
    print(f"[AsrEngine] Result: '{tactical_phrase}' (Confidence: {confidence:.2f} > 0.70)")
    
    print("\n[SonarEncoder] Encoding text into 1024-dimensional semantic space...")
    time.sleep(1)
    # Mock embedding
    embedding = np.random.randn(1024).astype(np.float32)
    print(f"               Embedding shape: {embedding.shape}, dtype: {embedding.dtype}")
    
    print("\n[Quantization] Compressing Float32 to INT8 payload...")
    scale = np.max(np.abs(embedding)) / 127.0
    int8_vec = np.round(embedding / scale).clip(-127, 127).astype(np.int8)
    print(f"               Payload size: 1024 bytes (INT8) + 4 bytes (Scale Float32) = 1028 bytes")
    
    target_lang_bytes = target_lang[:4].ljust(4, '_').encode('utf-8')
    packet = int8_vec.tobytes() + np.array([scale], dtype=np.float32).tobytes() + target_lang_bytes
    print(f"               Final Packet Size (with Lang Code): {len(packet)} bytes")
    
    # ---------------------------------------------------------
    print_header("BLUETOOTH L2CAP TRANSPORT")
    print(f"[Transport] Sending {len(packet)} bytes over BLE L2CAP CoC...")
    time.sleep(1)
    print(f"[Transport] Packet Hex Dump (first 32 bytes): {packet[:32].hex().upper()}...")
    print(f"[Transport] Received ACK from Device B.")

    # ---------------------------------------------------------
    print_header("DEVICE B: RECEIVER PIPELINE")
    print(f"[Receiver] Received {len(packet)} byte packet.")
    time.sleep(0.5)
    
    print("[Quantization] Dequantizing payload back to Float32...")
    time.sleep(0.5)
    
    print(f"\n[CosineCache] Querying local SQLite Room DB for matches...")
    time.sleep(1)
    
    # Simulate Cache Hit for this tactical phrase
    cache_hit = True
    if cache_hit:
        print(f"[CosineCache] CACHE HIT! Similarity: 0.982 > 0.95")
        print(f"[AudioPlayback] Fast-Path: Playing pre-rendered 'hin_Deva_Enemy_spotted.wav'")
        print(f"                Total Latency: ~120ms")
    else:
        print(f"[CosineCache] CACHE MISS. Running slow path...")
        print(f"[SonarDecoder] Autoregressive decoding to {target_lang}...")
        time.sleep(2)
        print(f"[PiperTTS] Synthesizing speech for generated text...")
        time.sleep(1)
        print(f"[AudioPlayback] Playing generated PCM audio.")
        print(f"                Total Latency: ~2.5s")
        
    print("\nDemo Complete.")

if __name__ == "__main__":
    demo_pipeline()
