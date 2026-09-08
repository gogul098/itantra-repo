import torch
import torch.nn as nn
import torchaudio.functional as F
import math

class AnalogRadioChannel(nn.Module):
    def __init__(self, sample_rate=8000, snr_db_range=(5.0, 20.0), fade_prob=0.1):
        """
        Simulates an analog VHF/UHF Walkie-Talkie channel.
        It is fully differentiable, allowing the Neural Channel Autoencoder 
        to learn a robust modulation scheme that survives this specific noise profile.
        
        Args:
            sample_rate (int): Audio sample rate (8000Hz is typical for low bandwidth comms)
            snr_db_range (tuple): Range of Signal-to-Noise Ratios in dB to randomly apply during training.
            fade_prob (float): Probability of a severe signal fade occurring in a batch.
        """
        super().__init__()
        self.sample_rate = sample_rate
        self.snr_db_range = snr_db_range
        self.fade_prob = fade_prob

    def apply_bandpass_filter(self, waveform):
        """
        Limits bandwidth to 300Hz - 3000Hz, simulating standard analog voice channels.
        """
        # Lowpass at 3000 Hz
        waveform = F.lowpass_biquad(waveform, self.sample_rate, cutoff_freq=3000.0)
        # Highpass at 300 Hz
        waveform = F.highpass_biquad(waveform, self.sample_rate, cutoff_freq=300.0)
        return waveform

    def add_awgn(self, waveform):
        """
        Adds White Gaussian Noise based on a randomly sampled SNR.
        """
        # Calculate signal power
        signal_power = waveform.norm(p=2, dim=-1, keepdim=True) ** 2 / waveform.size(-1)
        
        # Sample SNR
        snr_db = torch.empty(waveform.size(0), 1, device=waveform.device).uniform_(*self.snr_db_range)
        snr_linear = 10 ** (snr_db / 10.0)
        
        # Calculate required noise power
        noise_power = signal_power / snr_linear
        
        # Generate noise
        noise = torch.randn_like(waveform)
        noise = noise * torch.sqrt(noise_power)
        
        return waveform + noise

    def apply_fading(self, waveform):
        """
        Simulates sudden signal drops or fading common in VHF/UHF movement.
        """
        # Create a fading mask for the batch
        batch_size = waveform.size(0)
        device = waveform.device
        
        # Determine which samples in the batch will experience fading
        do_fade = torch.rand(batch_size, 1, device=device) < self.fade_prob
        
        # Create a smooth fade out/in curve
        # For simplicity in this mock, we multiply by a random uniform scaler [0.1, 0.5] if faded
        fade_factors = torch.ones(batch_size, 1, device=device)
        fade_factors[do_fade] = torch.empty(do_fade.sum(), 1, device=device).uniform_(0.1, 0.5)
        
        return waveform * fade_factors

    def forward(self, waveform):
        """
        Forward pass simulating the radio transmission.
        Args:
            waveform: Tensor of shape (Batch, Length) or (Batch, 1, Length)
        """
        if waveform.dim() == 2:
            waveform = waveform.unsqueeze(1) # Add channel dim if missing
            
        # 1. Hardware limitations (bandpass)
        distorted = self.apply_bandpass_filter(waveform)
        
        # 2. Environmental Fading
        if self.training:
            distorted = self.apply_fading(distorted)
        
        # 3. Transmission Static (AWGN)
        if self.training:
            distorted = self.add_awgn(distorted)
            
        # Optional: Add clipping to simulate analog overdrive/distortion
        distorted = torch.clamp(distorted, min=-1.0, max=1.0)
        
        return distorted

if __name__ == "__main__":
    # Test the channel simulator
    channel = AnalogRadioChannel()
    # Mock a 1-second audio signal for a batch of 2
    mock_signal = torch.sin(2 * math.pi * 1000 * torch.linspace(0, 1, 8000)).unsqueeze(0).repeat(2, 1)
    
    noisy_signal = channel(mock_signal)
    print(f"Original shape: {mock_signal.shape}")
    print(f"Noisy shape: {noisy_signal.shape}")
    print(f"Is tensor differentiable? {noisy_signal.requires_grad}") # Should be False here as mock_signal doesn't require grad, but would in actual training
