import torch
from torch.utils.data import Dataset, DataLoader
from transformers import AutoTokenizer
import os
from datasets import load_from_disk

class UniversalSemanticDataset(Dataset):
    def __init__(self, data_dir, languages, max_length=128, model_name="xlm-roberta-base"):
        """
        Loads the multilingual dataset and tokenizes it using a universal tokenizer.
        """
        self.tokenizer = AutoTokenizer.from_pretrained(model_name)
        self.max_length = max_length
        self.samples = []
        
        # Load datasets from disk
        pivot = "eng_Latn"
        for lang in languages:
            if lang == pivot:
                continue
                
            pair = f"{pivot}-{lang}"
            pair_dir = os.path.join(data_dir, pair)
            
            if os.path.exists(pair_dir):
                dataset = load_from_disk(pair_dir)
                # We'll extract training pairs (e.g., from 'dev' split if 'train' is unavailable in FLORES)
                split_name = 'dev' if 'dev' in dataset else list(dataset.keys())[0]
                
                for item in dataset[split_name]:
                    # Append both (pivot -> lang) and (lang -> pivot) to force the universal latent space
                    # Actually, for an autoencoder, the input and target are the SAME conceptually, 
                    # but they are expressed in different languages.
                    # Input: Lang A -> Target: Lang B
                    self.samples.append({
                        "source_text": item["sentence_eng_Latn"],
                        "target_text": item[f"sentence_{lang}"],
                        "source_lang": pivot,
                        "target_lang": lang
                    })
                    # And reverse
                    self.samples.append({
                        "source_text": item[f"sentence_{lang}"],
                        "target_text": item["sentence_eng_Latn"],
                        "source_lang": lang,
                        "target_lang": pivot
                    })

    def __len__(self):
        return len(self.samples)

    def __getitem__(self, idx):
        sample = self.samples[idx]
        
        # Tokenize source
        source_enc = self.tokenizer(
            sample["source_text"], 
            truncation=True, 
            padding="max_length", 
            max_length=self.max_length, 
            return_tensors="pt"
        )
        
        # Tokenize target
        target_enc = self.tokenizer(
            sample["target_text"], 
            truncation=True, 
            padding="max_length", 
            max_length=self.max_length, 
            return_tensors="pt"
        )
        
        return {
            "source_ids": source_enc["input_ids"].squeeze(),
            "source_mask": source_enc["attention_mask"].squeeze(),
            "target_ids": target_enc["input_ids"].squeeze(),
            "target_mask": target_enc["attention_mask"].squeeze(),
            "source_lang": sample["source_lang"],
            "target_lang": sample["target_lang"]
        }

def get_dataloader(data_dir, languages, batch_size=32, max_length=128):
    dataset = UniversalSemanticDataset(data_dir, languages, max_length)
    return DataLoader(dataset, batch_size=batch_size, shuffle=True)

if __name__ == "__main__":
    # Quick test
    print("Testing DataLoader initialization...")
    # This will only work if download_corpus.py has been run
    # dataloader = get_dataloader("data/", ["hin_Deva"])
    # batch = next(iter(dataloader))
    # print(batch["source_ids"].shape)
