from sklearn.neighbors import KDTree


class KNNRecommendationEngine:

    def __init__(self, leaf_size=40):
        self.targets = None
        self.tree = None
        self.leaf_size = leaf_size

    def build(self, features, targets):
        self.tree = KDTree(features, self.leaf_size)
        self.targets = targets

    def query(self, queries, k, rest_attributes):
        max_len = max([len(a) for a in rest_attributes])
        recommendations = self.tree.query(queries, k=k + max_len, return_distance=False)
        recommendations = [[self.targets[i] for i in y if self.targets[i] not in rest][0:k]
                           for y, rest in zip(recommendations, rest_attributes)]
        return recommendations
