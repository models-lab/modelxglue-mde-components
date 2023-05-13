import argparse
import json
import os

import torch.cuda
from transformers import RobertaTokenizerFast, RobertaForMaskedLM

DEVICE = "cuda" if torch.cuda.is_available() else "cpu"
DEFAULT_MODEL = "/model/models/ecorebert-large"
DEFAULT_TOKENIZER = "/model/tokenizers/ecorebert-bpe-30k"
MAX_LEN = 2046


def merge_ranked_lists(indices_1, indices_2, values_1, values_2):
    tuples_1 = list(zip(indices_1, values_1))
    tuples_2 = list(zip(indices_2, values_2))
    tuples = tuples_1 + tuples_2
    tuples.sort(key=lambda x: x[1], reverse=True)
    result = []
    for x, _ in tuples:
        if x not in result:
            result.append(x)
    return result[:len(tuples_1)]


def get_input(input_tok, tokenizer):
    mask_token_index_1 = torch.where(input_tok == tokenizer.mask_token_id)[1][-1].unsqueeze(0)
    if mask_token_index_1 < MAX_LEN:
        return input_tok[:, 0:MAX_LEN]
    else:
        init = mask_token_index_1 - MAX_LEN
        return input_tok[:, init:init + MAX_LEN]


def recommend(input, model, tokenizer, k):
    input_tok = tokenizer.encode(input,
                                 return_tensors='pt',
                                 padding='longest').to(DEVICE)
    input_tok = get_input(input_tok, tokenizer)
    mask_token_index_1 = torch.where(input_tok == tokenizer.mask_token_id)[1][-1].unsqueeze(0)

    token_logits = model(input_tok)[0]
    mask_token_logits_1 = token_logits[0, mask_token_index_1, :]

    top_1 = torch.topk(mask_token_logits_1.squeeze(0), k)
    values_1, indices_1 = top_1.values.tolist(), top_1.indices.tolist()
    return values_1, indices_1


def main(args):
    with open(os.path.join(args.root, args.file), "r") as f:
        as_json = json.load(f)

    print(args.model_path)
    model = RobertaForMaskedLM.from_pretrained(args.model_path).to(DEVICE)
    tokenizer = RobertaTokenizerFast.from_pretrained(args.tokenizer_path)

    all_suggestions = []
    for data in as_json:
        suggestions = []
        try:
            input_attr = data['tree-attr']
            input_assoc = data['tree-assoc']
            values_1, indices_1 = recommend(input_attr, model, tokenizer, args.k)
            values_2, indices_2 = recommend(input_assoc, model, tokenizer, args.k)
            final_indices = merge_ranked_lists(indices_1, indices_2, values_1, values_2)
            suggestions = [tokenizer.decode([token]).strip() for token in final_indices]
            print(f'Suggestions: {suggestions}')
        except Exception as e:
            print(e)
            print(f"Error in {data['ids']}, owner {data['owner']}, target {data['target']}")
        all_suggestions.append(suggestions)
    with open(os.path.join(args.root, "y_pred.json"), "w") as f:
        json.dump(all_suggestions, f)


if __name__ == '__main__':
    # generate arguments
    parser = argparse.ArgumentParser()
    parser.add_argument('--root', type=str, default=None, required=True)
    parser.add_argument('--file', type=str, default="X.json")
    parser.add_argument('--k', type=int, default=5)
    parser.add_argument(
        '--model_path', type=str, default=DEFAULT_MODEL,
        help='Path to the pretrained RoBERTa model.'
    )
    parser.add_argument(
        '--tokenizer_path', type=str, default=DEFAULT_TOKENIZER,
        help='Path to the BPE tokenizer.'
    )
    args = parser.parse_args()
    main(args)
