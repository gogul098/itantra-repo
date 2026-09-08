import os
from datasets import load_dataset
import argparse

def download_multilingual_corpus(languages, output_dir="data/"):
    """
    Downloads and prepares a massively multilingual corpus.
    For this implementation, we use the FLORES-200 dataset which contains 
    high-quality translations across 200 languages.
    """
    os.makedirs(output_dir, exist_ok=True)
    
    print(f"Downloading FLORES-200 for languages: {languages}")
    
    # FLORES uses English (eng_Latn) as the pivot in many datasets, or we can load specific pairs.
    # To simulate the universal latent space, we'll download translation pairs and 
    # build a consolidated dataset mapping everything to a shared index.
    
    pivot = "eng_Latn"
    corpus_data = {}
    
    for lang in languages:
        if lang == pivot:
            continue
            
        pair = f"{pivot}-{lang}"
        print(f"Fetching pair: {pair}...")
        try:
            # FLORES dataset usually requires trust_remote_code=True
            dataset = load_dataset("facebook/flores", pair, trust_remote_code=True)
            
            # Save the raw dataset to disk for offline access
            pair_dir = os.path.join(output_dir, pair)
            dataset.save_to_disk(pair_dir)
            print(f"Saved {pair} to {pair_dir}")
            
        except Exception as e:
            print(f"Failed to fetch {pair}: {e}")

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Download multilingual corpus")
    parser.add_argument("--langs", nargs="+", 
                        default=["hin_Deva", "tam_Taml", "spa_Latn", "fra_Latn"],
                        help="List of target FLORES language codes")
    parser.add_argument("--out", type=str, default="data/", help="Output directory")
    
    args = parser.parse_args()
    
    # Always include English as the base pivot
    langs = ["eng_Latn"] + args.langs
    download_multilingual_corpus(langs, args.out)
    print("Corpus download complete.")
