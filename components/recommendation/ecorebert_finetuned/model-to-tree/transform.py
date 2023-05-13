import argparse
import json
import os

OPEN_CHAR = '('
CLOSE_CHAR = ')'
ROOT_TAG = '<MODEL>'
CLS_TAG = '<CLS>'
CLS_NAME_TAG = '<NAME>'
FEATURE_TAG = '<FEATURES>'
MASK = '<mask>'


def transform(data, owner):
    """
    Generate the complete tree structure.
    """
    tree_string = OPEN_CHAR + ' '
    tree_string += ROOT_TAG
    for cls in data['children']:
        tree_string += ' ' + OPEN_CHAR + ' ' + CLS_TAG + ' '
        tree_string += OPEN_CHAR + ' ' + CLS_NAME_TAG + ' ' + cls['name'] + ' ' + CLOSE_CHAR
        features = cls['attrs'] + cls['assocs']
        if cls['name'] == owner:
            features.append({MASK: MASK})
        if len(features) > 0:
            tree_string += ' ' + OPEN_CHAR + ' ' + FEATURE_TAG
            for attrs in features:
                for key, value in attrs.items():
                    tree_string += ' ' + OPEN_CHAR + ' ' + key + ' ' + value + ' ' + CLOSE_CHAR
            tree_string += ' ' + CLOSE_CHAR
        tree_string += ' ' + CLOSE_CHAR
    tree_string += ' ' + CLOSE_CHAR
    return tree_string


def main(args):
    print("Running second preprecessing")
    with open(os.path.join(args.root, args.file), "r") as f:
        as_json = json.load(f)
    for data in as_json:
        data['tree'] = transform(json.loads(data['tree']), data['owner'])
    with open(os.path.join(args.root, args.file), "w") as f:
        json.dump(as_json, f)


if __name__ == '__main__':
    # generate arguments
    parser = argparse.ArgumentParser()
    parser.add_argument('--root', type=str, default=None, required=True)
    parser.add_argument('--file', type=str, default="transformed.json")
    args = parser.parse_args()
    main(args)
