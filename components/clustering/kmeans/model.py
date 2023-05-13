import os
import sys

from sklearn.cluster import KMeans

sys.path.append(os.path.dirname(__file__))
from utils.model_io import ModelInOut, execute_model, ModelImplementation


class Model(ModelImplementation):
    def build(self, X, inout: ModelInOut):
        X = self.get_vector_input(X)
        model = KMeans(**inout.hyper)
        model.fit(X)
        return model.labels_


def main(inout: ModelInOut):
    inout.execute_model(Model())


if __name__ == "__main__":
    execute_model(main, fixed_stage='build')
    exit(0)
