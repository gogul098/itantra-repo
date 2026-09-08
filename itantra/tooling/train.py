import torch
import torch.nn as nn
import torch.optim as optim
from tqdm import tqdm
from data_ops.dataset import get_dataloader
from models.encoder import SemanticRadioEncoder
from models.decoder import SemanticRadioDecoder
from channel.noise_simulator import AnalogRadioChannel

def train():
    print("Initializing Cross-Lingual Edge Radio Training Pipeline...")
    
    # Hyperparameters
    batch_size = 16 # Small for low-spec dev machines, scale up if GPU allows
    epochs = 10
    learning_rate = 1e-4
    languages = ["hin_Deva", "tam_Taml"] # Start with a subset
    device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
    print(f"Using device: {device}")
    
    # 1. Load Data
    try:
        dataloader = get_dataloader("data/", languages, batch_size=batch_size)
    except Exception as e:
        print(f"Failed to load dataset: {e}")
        print("Please ensure you run 'python data_ops/download_corpus.py' first.")
        return
        
    # 2. Initialize Models
    encoder = SemanticRadioEncoder().to(device)
    channel = AnalogRadioChannel().to(device)
    decoder = SemanticRadioDecoder().to(device)
    
    # Freeze the pre-trained text extractor in the encoder to save memory
    # We only train the bottleneck, modulator, and the entire decoder.
    for param in encoder.text_encoder.parameters():
        param.requires_grad = False
        
    # Optimizer & Loss
    # We optimize the encoder (bottleneck+modulator) and decoder together
    params = list(encoder.bottleneck.parameters()) + \
             list(encoder.modulator.parameters()) + \
             list(decoder.parameters())
             
    optimizer = optim.AdamW(params, lr=learning_rate)
    criterion = nn.CrossEntropyLoss(ignore_index=1) # Assuming 1 is padding token for XLM-R
    
    # 3. Training Loop
    for epoch in range(epochs):
        encoder.train()
        decoder.train()
        epoch_loss = 0.0
        
        progress_bar = tqdm(dataloader, desc=f"Epoch {epoch+1}/{epochs}")
        for batch in progress_bar:
            source_ids = batch["source_ids"].to(device)
            source_mask = batch["source_mask"].to(device)
            target_ids = batch["target_ids"].to(device)
            
            optimizer.zero_grad()
            
            # Step 1: Encode to Acoustic Payload
            waveform = encoder(source_ids, source_mask)
            
            # Step 2: Pass through Noisy Analog Radio Channel
            noisy_waveform = channel(waveform)
            
            # Step 3: Decode from Acoustic Payload to Target Tokens
            logits = decoder(noisy_waveform)
            
            # Reshape for CrossEntropyLoss
            # logits: (Batch, Seq_Len, Vocab) -> (Batch * Seq_Len, Vocab)
            # target: (Batch, Seq_Len) -> (Batch * Seq_Len)
            loss = criterion(logits.view(-1, logits.size(-1)), target_ids.view(-1))
            
            loss.backward()
            optimizer.step()
            
            epoch_loss += loss.item()
            progress_bar.set_postfix({"loss": loss.item()})
            
        print(f"Epoch {epoch+1} Average Loss: {epoch_loss / len(dataloader):.4f}")
        
        # Save checkpoints
        torch.save({
            'encoder_state_dict': encoder.state_dict(),
            'decoder_state_dict': decoder.state_dict(),
        }, f"checkpoint_epoch_{epoch+1}.pt")
        
    print("Training Complete. Models saved.")

if __name__ == "__main__":
    train()
