import os
import sys

from sklearn.cluster import AgglomerativeClustering

sys.path.append(os.path.dirname(__file__))
from utils.model_io import ModelInOut, execute_model, ModelImplementation


class Model(ModelImplementation):
    def build(self, X, inout: ModelInOut):
        X = self.get_vector_input(X)
        #linkage = inout.hyper['linkage'] if inout.hyper and 'linkage' in inout.hyper else 'complete'
        #metric = inout.hyper['cosine'] if inout.hyper and 'linkage' in inout.hyper else 'complete'
        model = AgglomerativeClustering(**inout.hyper)
        # n_clusters=inout.hyper['n_clusters'], affinity=inout.hyper['affinity'], linkage=inout.hyper['linkage']
        #model = AgglomerativeClustering(linkage=linkage)
        model.fit(X)
        return model.labels_


def main(inout: ModelInOut):
    inout.execute_model(Model())


if __name__ == "__main__":
    execute_model(main, fixed_stage='build')
    exit(0)
