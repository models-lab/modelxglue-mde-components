import os
import pickle
import sys

import numpy as np
from numpy import ndarray

sys.path.append(os.path.dirname(__file__))
from utils.model_io import ModelInOut, execute_model, ModelImplementation
from knn import KNNRecommendationEngine


class Model(ModelImplementation):
    def train(self, X, y, inout: ModelInOut):
        matrix = X.select_dtypes(include=[np.number])
        y = X['target'].values
        if isinstance(y, ndarray):
            y = y.tolist()

        model = KNNRecommendationEngine()
        model.build(matrix, y)
        return model

    def test(self, loaded_model, X, inout: ModelInOut):
        matrix = X.select_dtypes(include=[np.number])
        rest_attributes = [r.split(' ')[1:] for r in X['context'].tolist()]
        y_pred = loaded_model.query(matrix, 5, rest_attributes)
        return y_pred

    def get_serialized_model_name(self):
        return 'model.pkl'

    def dump_model(self, model, path):
        with open(path, "wb") as f:
            pickle.dump(model, f)

    def load_model(self, path):
        return pickle.load(open(path, "rb"))

def main(inout: ModelInOut):
    inout.execute_model(Model())


if __name__ == "__main__":
    execute_model(main)
    exit(0)



