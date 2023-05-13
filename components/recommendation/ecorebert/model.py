import os
import sys

sys.path.append(os.path.dirname(__file__))
from utils.model_io import ModelInOut, execute_model, ModelImplementation
import torch
from tqdm import tqdm
from transformers import RobertaTokenizerFast, RobertaForMaskedLM

from inference import recommend, merge_ranked_lists

DEVICE = "cuda" if torch.cuda.is_available() else "cpu"

k = 5

class Model(ModelImplementation):
    def train(self, X, y, inout: ModelInOut):
        pass

    def ignore_train(self):
        return True

    def test(self, loaded_model, X, inout: ModelInOut):
        all_data = X

        model_path = os.path.join(inout.original, 'models/ecorebert-large')
        tokenizer_path = os.path.join(inout.original, 'tokenizers/ecorebert-bpe-30k')

        model = RobertaForMaskedLM.from_pretrained(model_path).to(DEVICE)
        tokenizer = RobertaTokenizerFast.from_pretrained(tokenizer_path)

        all_suggestions = []
        all_data.reset_index()
        for idx, data in tqdm(all_data.iterrows(), desc='Loop over the data'):
            suggestions = []
            try:
                input_attr = data['tree-attr']
                input_assoc = data['tree-assoc']
                values_1, indices_1 = recommend(input_attr, model, tokenizer, k)
                values_2, indices_2 = recommend(input_assoc, model, tokenizer, k)
                final_indices = merge_ranked_lists(indices_1, indices_2, values_1, values_2)
                suggestions = [tokenizer.decode([token]).strip() for token in final_indices]
                #print(f'Suggestions: {suggestions}')
            except Exception as e:
                print(e)
                print(f"Error in {data['ids']}, owner {data['owner']}, target {data['target']}")
            all_suggestions.append(suggestions)

        return all_suggestions


def main(inout: ModelInOut):
    inout.execute_model(Model())


if __name__ == "__main__":
    execute_model(main)
    exit(0)
