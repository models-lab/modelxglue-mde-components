import os
import random

import datasets
import numpy as np
import pandas as pd
import torch
import torch.nn as nn
from torch.optim import AdamW
from torch.utils.data import DataLoader
from tqdm import tqdm
from transformers import RobertaTokenizerFast, RobertaForMaskedLM

from utils.model_io import ModelInOut, execute_model, ModelImplementation

from inference import recommend

DEVICE = "cuda" if torch.cuda.is_available() else "cpu"
FEATURE_TAG = '<FEATURES>'
FINE_TUNED_PATH_MODEL = os.path.join(os.path.dirname(__file__), 'finetuned_model')
FINE_TUNED_PATH_TOKENIZER = os.path.join(os.path.dirname(__file__), 'finetuned_tokenizer')
MAX_LEN_TRAINING = 512
SEED = 123


def set_seed(seed: int):
    random.seed(seed)
    np.random.seed(seed)
    torch.manual_seed(seed)
    torch.cuda.manual_seed_all(seed)

    os.environ["CUDA_LAUNCH_BLOCKING"] = "1"
    os.environ["CUBLAS_WORKSPACE_CONFIG"] = ":16:8"
    torch.use_deterministic_algorithms(True)

    # Enable CUDNN deterministic mode
    torch.backends.cudnn.deterministic = True
    torch.backends.cudnn.benchmark = False


def set_up_tokenizer_train(model, tokenizer):
    tokenizer.add_tokens([FEATURE_TAG], special_tokens=True)
    print(tokenizer.tokenize(f' EString <ATTRS> EString'))
    print(tokenizer.tokenize(f' EString {FEATURE_TAG} EString'))
    print(tokenizer.tokenize(f' EString <mask> EString'))
    assert len(tokenizer.tokenize(f'{FEATURE_TAG}')) == 1
    model.resize_token_embeddings(len(tokenizer))


def train(model, tokenizer, data):
    set_up_tokenizer_train(model, tokenizer)

    dataset = datasets.Dataset.from_pandas(pd.DataFrame(data=data))

    def tokenization(example):
        return tokenizer(example["tree"], padding="max_length", truncation=True, max_length=MAX_LEN_TRAINING)

    dataset = dataset.map(tokenization, batched=True)
    dataset = dataset.map(lambda sample: {"target": tokenizer.encode(' ' + sample["target"])[1]})
    dataset = dataset.remove_columns(['tree', 'ids', 'owner'])
    dataset.set_format("torch")
    dataset = dataset.filter(lambda sample: len([i for i in sample["input_ids"]
                                                 if i == tokenizer.mask_token_id]) == 2)

    optimizer = AdamW(model.parameters(), lr=1e-5)
    num_epochs = 3
    train_dataloader = DataLoader(dataset, shuffle=True, batch_size=4)
    num_training_steps = num_epochs * len(train_dataloader)
    progress_bar = tqdm(range(num_training_steps))
    criterion = nn.CrossEntropyLoss()

    model.train()
    for epoch in range(num_epochs):
        total_loss = 0.
        for step, batch in enumerate(train_dataloader):
            batch = {k: v.to(DEVICE) for k, v in batch.items()}
            outputs = model(batch['input_ids'], batch['attention_mask'])[0]
            outputs = outputs[batch['input_ids'] == tokenizer.mask_token_id][1::2, :]
            loss = criterion(outputs, batch['target'])
            loss.backward()
            optimizer.step()
            optimizer.zero_grad()
            progress_bar.update(1)
            total_loss += loss.item()
            if step % 200 == 0 and not step == 0:
                print(f"Epoch {epoch + 1} | Batch {step} | {total_loss / (step + 1):.4f} ")

    return (model, tokenizer)


class Model(ModelImplementation):
    def get_model_paths(self, inout: ModelInOut):
        model_path = os.path.join(inout.original, 'models/ecorebert-large')
        tokenizer_path = os.path.join(inout.original, 'tokenizers/ecorebert-bpe-30k')
        return model_path, tokenizer_path

    def dump_model(self, ml_model: object, path):
        model, tokenizer = ml_model
        model.save_pretrained(FINE_TUNED_PATH_MODEL)
        tokenizer.save_pretrained(FINE_TUNED_PATH_TOKENIZER)

    def load_model(self, path):
        model = RobertaForMaskedLM.from_pretrained(FINE_TUNED_PATH_MODEL).to(DEVICE)
        tokenizer = RobertaTokenizerFast.from_pretrained(FINE_TUNED_PATH_TOKENIZER)
        return (model, tokenizer)

    def train(self, X, y, inout: ModelInOut):
        model_path, tokenizer_path = self.get_model_paths(inout)
        model = RobertaForMaskedLM.from_pretrained(model_path).to(DEVICE)
        tokenizer = RobertaTokenizerFast.from_pretrained(tokenizer_path)
        return train(model, tokenizer, X)

    def test(self, loaded_model, X, inout: ModelInOut):
        all_data = X
        model, tokenizer = loaded_model

        all_suggestions = []
        for idx, data in tqdm(all_data.iterrows(), desc='Loop over the data'):
            suggestions = []
            try:
                tree = data['tree']
                values_1, indices_1 = recommend(tree, model, tokenizer, 5, MAX_LEN_TRAINING)
                suggestions = [tokenizer.decode([token]).strip() for token in indices_1]
                print(f'Suggestions: {suggestions}')
            except Exception as e:
                print(e)
                print(f"Error in {data['ids']}, owner {data['owner']}, target {data['target']}")
            all_suggestions.append(suggestions)

        return all_suggestions


def main(inout: ModelInOut):
    inout.execute_model(Model())


if __name__ == "__main__":
    set_seed(SEED)
    execute_model(main)
    exit(0)
