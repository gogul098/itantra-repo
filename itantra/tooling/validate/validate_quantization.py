import numpy as np

def cosine_similarity(a, b):
    return np.dot(a, b) / (np.linalg.norm(a) * np.linalg.norm(b))

def validate_quantization():
    print("Validating Float32 -> INT8 -> Float32 round-trip quantization...")
    
    # Generate 100 random semantic embeddings to simulate SONAR output
    # (100 sentences, 1024-dim Float32)
    embeddings = np.random.randn(100, 1024).astype(np.float32)
    
    similarities = []
    
    for emb in embeddings:
        # Quantize: scale = max(abs(embedding)) / 127
        scale = np.max(np.abs(emb)) / 127.0
        
        # int8_vec = round(embedding / scale).clip(-127, 127)
        int8_vec = np.round(emb / scale).clip(-127, 127).astype(np.int8)
        
        # Dequantize: float32_reconstructed = int8_vec * scale
        reconstructed = int8_vec.astype(np.float32) * scale
        
        # Calculate cosine similarity
        sim = cosine_similarity(emb, reconstructed)
        similarities.append(sim)
        
    mean_sim = np.mean(similarities)
    min_sim = np.min(similarities)
    max_sim = np.max(similarities)
    
    print(f"Quantization Validation Results:")
    print(f"Mean Cosine Similarity: {mean_sim:.4f}")
    print(f"Min Cosine Similarity:  {min_sim:.4f}")
    print(f"Max Cosine Similarity:  {max_sim:.4f}")
    
    assert mean_sim > 0.99, f"Mean cosine similarity {mean_sim:.4f} is below threshold 0.99!"
    print("Validation SUCCESS: INT8 round-trip preserves semantic meaning.")

if __name__ == "__main__":
    validate_quantization()
