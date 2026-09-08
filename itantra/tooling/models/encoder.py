import torch
import torch.nn as nn
from transformers import AutoModel

class SemanticRadioEncoder(nn.Module):
    def __init__(self, text_model_name="xlm-roberta-base", audio_dim=128, sequence_length=128):
        """
        Takes input text tokens and converts them into an acoustic waveform 
        representing the universal semantic latent space.
        Designed to be highly quantized later (INT4/INT8) for legacy Android devices.
        """
        super().__init__()
        
        # 1. Semantic Extractor (Universal Latent Space)
        # In a real extreme-edge deployment, we'd replace this with a distilled micro-model
        self.text_encoder = AutoModel.from_pretrained(text_model_name)
        hidden_size = self.text_encoder.config.hidden_size
        
        # 2. Latent Bottleneck (Compression for Radio)
        # Compress the heavy embedding into a tight acoustic payload
        self.bottleneck = nn.Sequential(
            nn.Linear(hidden_size, 256),
            nn.GELU(),
            nn.Linear(256, audio_dim)
        )
        
        # 3. Acoustic Modulator (Neural learned modulation)
        # Maps the latent vectors to a raw 1D audio waveform
        # Using a transposed CNN (Generator architecture) to upsample to audio rate
        self.modulator = nn.Sequential(
            nn.ConvTranspose1d(audio_dim, 64, kernel_size=4, stride=2, padding=1),
            nn.GELU(),
            nn.ConvTranspose1d(64, 32, kernel_size=4, stride=2, padding=1),
            nn.GELU(),
            nn.ConvTranspose1d(32, 1, kernel_size=4, stride=2, padding=1),
            nn.Tanh() # Bound between -1 and 1 for audio
        )
        
    def forward(self, input_ids, attention_mask):
        """
        Args:
            input_ids: Tokenized text (Batch, Seq_Len)
            attention_mask: Mask for padding
        Returns:
            waveform: Acoustic payload ready for analog radio (Batch, 1, Audio_Len)
        """
        # Get semantic embeddings
        # shape: (Batch, Seq_Len, Hidden_Size)
        text_outputs = self.text_encoder(input_ids=input_ids, attention_mask=attention_mask)
        embeddings = text_outputs.last_hidden_state
        
        # Pass through bottleneck
        # shape: (Batch, Seq_Len, audio_dim)
        latent = self.bottleneck(embeddings)
        
        # Rearrange for Conv1D: (Batch, Channels, Length)
        latent = latent.transpose(1, 2)
        
        # Modulate into waveform
        # shape: (Batch, 1, Seq_Len * 8) -> Upsampled length
        waveform = self.modulator(latent)
        
        return waveform
