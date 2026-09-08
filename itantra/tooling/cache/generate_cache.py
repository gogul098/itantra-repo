import sqlite3
import numpy as np
import os

TACTICAL_PHRASES = [
    "Move to the rally point",
    "Need medical assistance",
    "All clear, sector is safe",
    "Hold position, wait for orders",
    "Enemy spotted at grid reference",
    "Request for evacuation",
]

def generate_cache(output_db_path="vector-cache.db"):
    print(f"Generating tactical vector cache at {output_db_path}")
    
    conn = sqlite3.connect(output_db_path)
    cursor = conn.cursor()
    
    # Create the Room-compatible table structure
    cursor.execute("""
        CREATE TABLE IF NOT EXISTS cached_phrases (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            label TEXT,
            embedding BLOB NOT NULL,
            audio_path TEXT NOT NULL,
            lang_code TEXT NOT NULL,
            hit_count INTEGER NOT NULL DEFAULT 0
        )
    """)
    
    # We will mock the embedding generation and TTS generation for the script
    # In reality, you would run SONAR encoder + Piper TTS here for each language
    target_langs = ["hin_Deva", "tam_Taml"]
    
    for lang in target_langs:
        for phrase in TACTICAL_PHRASES:
            # Mock 1024-dim float32 embedding
            embedding = np.random.randn(1024).astype(np.float32)
            embedding_blob = embedding.tobytes()
            
            # Mock audio file path
            audio_filename = f"{lang}_{phrase.replace(' ', '_').replace(',', '')}.wav"
            audio_path = os.path.join(os.path.dirname(output_db_path), "audio", audio_filename)
            
            cursor.execute("""
                INSERT INTO cached_phrases (label, embedding, audio_path, lang_code, hit_count)
                VALUES (?, ?, ?, ?, ?)
            """, (phrase, embedding_blob, audio_path, lang, 0))
            
    conn.commit()
    conn.close()
    
    print("Vector cache generation complete. You can bundle this .db file with the Android APK.")

if __name__ == "__main__":
    generate_cache()
