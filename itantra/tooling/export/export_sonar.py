import os
import torch
from sonar.models.sonar_text import load_sonar_text_encoder_model, load_sonar_text_decoder_model
import onnx
from onnxsim import simplify

def export_sonar():
    # Make sure we create the output directory in the android app
    output_dir = os.path.join(os.path.dirname(__file__), "..", "..", "android", "app", "src", "main", "assets")
    os.makedirs(output_dir, exist_ok=True)
    
    device = torch.device("cpu")
    print("Loading SONAR encoder...")
    encoder = load_sonar_text_encoder_model("text_sonar_basic_encoder", device=device)
    encoder.eval()
    
    # Try exporting encoder
    # The input to encoder is usually a padded batch of tokenized integers.
    dummy_input_ids = torch.randint(0, 250000, (1, 128), dtype=torch.long, device=device)
    
    encoder_path = os.path.join(output_dir, "sonar_encoder.onnx")
    print(f"Attempting to export SONAR encoder to {encoder_path}...")
    
    try:
        torch.onnx.export(
            encoder,
            (dummy_input_ids,),
            encoder_path,
            export_params=True,
            opset_version=17, 
            do_constant_folding=True,
            input_names=['input_ids'],
            output_names=['embedding'],
            dynamic_axes={'input_ids': {0: 'batch_size', 1: 'seq_len'}, 'embedding': {0: 'batch_size'}}
        )
        print("Encoder exported successfully.")
        
        # Simplify
        model = onnx.load(encoder_path)
        model_simp, check = simplify(model)
        assert check, "Simplified ONNX model could not be validated"
        onnx.save(model_simp, encoder_path)
        print("Encoder simplified successfully.")
        
    except Exception as e:
        print(f"Failed to export encoder: {e}")
        print("Note: fairseq2 models might require PT2 export (torch.export) or optimum.")
        
    print("\nLoading SONAR decoder...")
    decoder = load_sonar_text_decoder_model("text_sonar_basic_decoder", device=device)
    decoder.eval()
    
    # The decoder takes semantic embeddings (batch_size, 1024) and target sequence 
    dummy_embedding = torch.randn((1, 1024), dtype=torch.float32, device=device)
    dummy_target_seq = torch.randint(0, 250000, (1, 10), dtype=torch.long, device=device)
    
    decoder_path = os.path.join(output_dir, "sonar_decoder.onnx")
    print(f"Attempting to export SONAR decoder to {decoder_path}...")
    try:
        torch.onnx.export(
            decoder,
            (dummy_target_seq, dummy_embedding),
            decoder_path,
            export_params=True,
            opset_version=17,
            do_constant_folding=True,
            input_names=['target_ids', 'encoder_embedding'],
            output_names=['logits'],
            dynamic_axes={
                'target_ids': {0: 'batch_size', 1: 'seq_len'},
                'encoder_embedding': {0: 'batch_size'},
                'logits': {0: 'batch_size', 1: 'seq_len'}
            }
        )
        print("Decoder exported successfully.")
        
        # Simplify
        model = onnx.load(decoder_path)
        model_simp, check = simplify(model)
        assert check, "Simplified ONNX model could not be validated"
        onnx.save(model_simp, decoder_path)
        print("Decoder simplified successfully.")
        
    except Exception as e:
        print(f"Failed to export decoder: {e}")

if __name__ == "__main__":
    export_sonar()
