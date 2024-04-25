import os
import sys

sys.path.append(os.path.dirname(__file__))
from utils.model_io import ModelInOut, execute_model, ModelImplementation
from GNN import GNNClassifier

def set_seed(seed: int):
    import random
    import numpy as np
    import torch
    random.seed(seed)
    np.random.seed(seed)
    torch.manual_seed(seed)
    torch.cuda.manual_seed_all(seed)

class Model(ModelImplementation):
    def train(self, X, y, inout: ModelInOut):
        set_seed(inout.seed)
        model = GNNClassifier(**inout.hyper)
        model.fit(X['graph'], y)
        return model

    def test(self, loaded_model, X, inout: ModelInOut):
        y_pred = loaded_model.predict(X['graph'])
        return y_pred


def main(inout: ModelInOut):
    inout.execute_model(Model())


if __name__ == "__main__":
    execute_model(main)
    exit(0)
