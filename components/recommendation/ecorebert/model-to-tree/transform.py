import argparse
import json
import os

OPEN_CHAR = '('
CLOSE_CHAR = ')'
ROOT_TAG = '<MODEL>'
CLS_TAG = '<CLS>'
CLS_NAME_TAG = '<NAME>'
ATTRS_TAG = '<ATTRS>'
ASSOCS_TAG = '<ASSOCS>'
MASK = '<mask>'


def transform(data, owner, what='attr'):
    """
            Generate the complete tree structure.
            """
    tree_string = OPEN_CHAR + ' '
    tree_string += ROOT_TAG
    for cls in data['children']:
        tree_string += ' ' + OPEN_CHAR + ' ' + CLS_TAG + ' '
        tree_string += OPEN_CHAR + ' ' + CLS_NAME_TAG + ' ' + cls['name'] + ' ' + CLOSE_CHAR
        if cls['name'] == owner:
            if what == 'attr':
                cls['attrs'].append({MASK: MASK})
            else:
                cls['assocs'].append({MASK: MASK})
        if len(cls['attrs']) > 0:
            tree_string += ' ' + OPEN_CHAR + ' ' + ATTRS_TAG
            for attrs in cls['attrs']:
                for key, value in attrs.items():
                    tree_string += ' ' + OPEN_CHAR + ' ' + key + ' ' + value + ' ' + CLOSE_CHAR
            tree_string += ' ' + CLOSE_CHAR
        if len(cls['assocs']) > 0:
            tree_string += ' ' + OPEN_CHAR + ' ' + ASSOCS_TAG
            for assocs in cls['assocs']:
                for key, value in assocs.items():
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
        data['tree-attr'] = transform(json.loads(data['tree']), data['owner'], 'attr')
        data['tree-assoc'] = transform(json.loads(data['tree']), data['owner'], 'assoc')
    with open(os.path.join(args.root, args.file), "w") as f:
        json.dump(as_json, f)


if __name__ == '__main__':
    # generate arguments
    parser = argparse.ArgumentParser()
    parser.add_argument('--root', type=str, default=None, required=True)
    parser.add_argument('--file', type=str, default="transformed.json")
    args = parser.parse_args()
    main(args)
