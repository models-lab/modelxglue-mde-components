import logging
from collections import Counter, OrderedDict

import numpy as np
import torch
from networkx.readwrite import json_graph
from sklearn.base import BaseEstimator, ClassifierMixin
from torch import nn
from torch_geometric.loader import DataLoader
from torch_geometric.nn import SAGEConv, global_mean_pool
from torch_geometric.utils.convert import from_networkx
from torchtext.vocab import vocab

#from utils.txt_utils import tokenizer

METAMODEL_ATTRIBUTES = ['name', 'eClass']
logger = logging.getLogger()

from re import finditer


def camel_case_split(identifier):
    matches = finditer('.+?(?:(?<=[a-z])(?=[A-Z])|(?<=[A-Z])(?=[A-Z][a-z])|$)', identifier)
    return [m.group(0) for m in matches]


def tokenizer(doc):
    words = doc.split('\n')
    # split _
    words = [w2 for w1 in words for w2 in w1.split('_') if w2 != '']
    # camelcase
    words = [w2.lower() for w1 in words for w2 in camel_case_split(w1) if w2 != '']
    return words

#install torch, torch - geometric, and torchtext

class GCN(torch.nn.Module):
    def __init__(self, embedding_dim, embedding_model,
                 vocabulary_size, num_classes,
                 pad_idx, hidden_dim):
        super().__init__()
        weights = torch.zeros(vocabulary_size, embedding_dim)
        print(len(embedding_model.vectors))
        print(vocabulary_size)
        assert len(embedding_model.vectors) == vocabulary_size - 2
        weights[2:vocabulary_size, 0:embedding_dim] = torch.FloatTensor(embedding_model.vectors)
        self.embedding = nn.EmbeddingBag.from_pretrained(weights, padding_idx=pad_idx)
        self.conv1 = SAGEConv(embedding_dim, hidden_dim)
        self.conv2 = SAGEConv(hidden_dim, hidden_dim)
        self.out = nn.Linear(hidden_dim, num_classes)

    def forward(self, data):
        x, edge_index, batch = data.x, data.edge_index, data.batch
        x = self.embedding(x)
        x = self.conv1(x, edge_index)
        x = torch.relu(x)
        x = self.conv2(x, edge_index)
        x = torch.relu(x)
        x = global_mean_pool(x, batch)
        x = self.out(x)
        return x


PAD_TOKEN = '<pad>'
UNK_TOKEN = '<unc>' # before was unk but it collides with GloVe's <unk> token


class GNNClassifier(BaseEstimator, ClassifierMixin):
    def __init__(self, embedding_size=256, hidden_dim=128, batch_size=32, epochs=60, step_print=10, verbose=True):
        self.vocab_catergories = None
        self.global_vocab = None
        self.device = torch.device('cuda' if torch.cuda.is_available() else 'cpu')
        # Hyper-parameters
        self.embedding_size = embedding_size
        self.batch_size = batch_size
        self.epochs = epochs
        self.hidden_dim = hidden_dim
        self.step_print = step_print
        self.verbose = verbose

        from worde4mde import load_embeddings
        self.embedding_model = load_embeddings('sgram-mde')

    def json_to_nx(self, X):
        nx_graphs = []
        for g in X:
            networkx_graph = json_graph.node_link_graph(data=g)
            for n in networkx_graph:
                to_remove = []
                for att in networkx_graph.nodes[n]:
                    if att not in METAMODEL_ATTRIBUTES:
                        to_remove.append(att)
                for att in to_remove:
                    del networkx_graph.nodes[n][att]
            nx_graphs.append(networkx_graph)
        return nx_graphs

    def tokenize_atts(self, graphs):
        tokenized_graphs = []
        for g in graphs:
            g1 = g.copy()
            for att in METAMODEL_ATTRIBUTES:
                for n in g1:
                    if att not in g1.nodes[n]:
                        g1.nodes[n][att] = ['<none>']
                    else:
                        g1.nodes[n][att] = tokenizer(g1.nodes[n][att])
                    # g1.nodes[n][att] = g1.nodes[n][att] + ['<pad>' for _ in range(0, max_len - len(g1.nodes[n][att]))]
            tokenized_graphs.append(g1)
        return tokenized_graphs

    def generate_vocab(self, tokenized_graphs):
        counter = Counter()
        for tok in self.embedding_model.key_to_index.keys():
            counter[tok] += 1
        sorted_by_freq_tuples = sorted(counter.items(), key=lambda x: x[1], reverse=True)
        ordered_dict = OrderedDict(sorted_by_freq_tuples)
        tt_vocab = vocab(ordered_dict, specials=[UNK_TOKEN, PAD_TOKEN])
        tt_vocab.set_default_index(tt_vocab[UNK_TOKEN])
        return tt_vocab

    # def generate_vocab(self, tokenized_graphs):
    #     counter = Counter()
    #     for g in tokenized_graphs:
    #         for att in METAMODEL_ATTRIBUTES:
    #             for n in g:
    #                 for tok in g.nodes[n][att]:
    #                     counter[tok] += 1
    #     sorted_by_freq_tuples = sorted(counter.items(), key=lambda x: x[1], reverse=True)
    #     ordered_dict = OrderedDict(sorted_by_freq_tuples)
    #     tt_vocab = vocab(ordered_dict, specials=[UNK_TOKEN, PAD_TOKEN])
    #     tt_vocab.set_default_index(tt_vocab[UNK_TOKEN])
    #     return tt_vocab

    def to_torch_graph(self, networkx_graph, category=None, max_len=10):
        torch_graph = from_networkx(networkx_graph)
        all_atts = []
        for n in networkx_graph:
            atts = []
            for mt_att in METAMODEL_ATTRIBUTES:
                atts += networkx_graph.nodes[n][mt_att]
            atts += [PAD_TOKEN for _ in range(0, max_len - len(atts))]
            atts = [self.global_vocab[t] for t in atts][0:max_len]
            atts = torch.tensor(atts)
            all_atts.append(atts)
        # print(all_atts[0:50])
        torch_graph.x = torch.stack(all_atts, dim=0)
        if category:
            torch_graph.y = torch.tensor([self.vocab_catergories.index(category)])
        # torch_graph.x = torch_graph.x.type(torch.FloatTensor)
        return torch_graph

    def fit(self, X, y):
        nx_graphs = self.json_to_nx(X)
        tokenized_graphs = self.tokenize_atts(nx_graphs)
        self.global_vocab = self.generate_vocab(tokenized_graphs)
        self.vocab_catergories = list(np.unique(y))
        torch_graphs = [self.to_torch_graph(g, cat) for g, cat in zip(tokenized_graphs, y)]
        train_dataloader = DataLoader(torch_graphs, batch_size=self.batch_size, shuffle=True)

        self.__do_train(train_dataloader,
                        self.vocab_catergories,
                        self.global_vocab)

    def __do_train(self, dataloader, vocab_catergories, global_vocab):
        cross_loss = nn.CrossEntropyLoss()
        model = GCN(embedding_dim=self.embedding_size,
                    embedding_model=self.embedding_model,
                    vocabulary_size=len(global_vocab),
                    num_classes=len(vocab_catergories),
                    pad_idx=global_vocab[PAD_TOKEN],
                    hidden_dim=self.hidden_dim).to(self.device)
        # model.embedding.weight.requires_grad = False
        optimizer = torch.optim.Adam(model.parameters(), lr=0.0005)

        model.train()
        for epoch in range(self.epochs):
            epoch_loss = 0.0

            for data in dataloader:
                data = data.to(self.device)
                optimizer.zero_grad()
                out = model(data)

                y = data.y
                loss = cross_loss(out, y)
                epoch_loss += loss.item()

                loss.backward()
                optimizer.step()

            if (epoch + 1) % self.step_print == 0 and self.verbose:
                epoch_loss = epoch_loss / len(dataloader)
                logger.info(f'Epoch {epoch + 1} | loss train: {epoch_loss}')
        self.model = model
        self.model.eval()

    def predict(self, X):
        nx_graphs = self.json_to_nx(X)
        tokenized_graphs = self.tokenize_atts(nx_graphs)
        torch_graphs = [self.to_torch_graph(g) for g in tokenized_graphs]
        test_dataloader = DataLoader(torch_graphs, batch_size=self.batch_size, shuffle=False)
        pred_labels = []
        for data in test_dataloader:
            data = data.to(self.device)
            pred = self.model(data).argmax(dim=1).cpu().detach().numpy()
            labels = [self.vocab_catergories[p] for p in pred]
            pred_labels += labels
        return pred_labels
