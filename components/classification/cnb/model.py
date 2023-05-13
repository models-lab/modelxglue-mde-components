import os
import sys

from sklearn.naive_bayes import ComplementNB

sys.path.append(os.path.dirname(__file__))
from utils.model_io import ModelInOut, execute_model, ModelImplementation


class Model(ModelImplementation):
    def train(self, X, y, inout: ModelInOut):
        X = self.get_vector_input(X)
        model = ComplementNB(**inout.hyper)
        model.fit(X, y)
        return model

    def test(self, loaded_model, X, inout: ModelInOut):
        X = self.get_vector_input(X)
        y_pred = loaded_model.predict(X)
        return y_pred


def main(inout: ModelInOut):
    inout.execute_model(Model())


if __name__ == "__main__":
    execute_model(main)
    exit(0)
