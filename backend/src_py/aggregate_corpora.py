# Copyright 2018-2022 Vaibhav Kasturia <vaibhav.kasturia@informatik.uni-halle.de>, Marcel Gohsen <marcel.gohsen@uni-weimar.de>
#
# Permission is hereby granted, free of charge, to any person obtaining a copy of this software 
# and associated documentation files (the "Software"), to deal in the Software without restriction, 
# including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, 
# and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, 
# subject to the following conditions:
#
# The above copyright notice and this permission notice shall be included in all copies or substantial 
# portions of the Software.
#
# THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT 
# LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. 
# IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, 
# WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE 
# OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

import json
import xml.etree.ElementTree as ET
import collections as col


class Query(object):
    def __init__(self, id, text):
        self.id = id
        self.text = text

        self.annotations = []

    def __str__(self):
        return self.id + ';\"' + self.text.replace("\"", "") + '\";' + ';'.join(self.annotations)

    def __eq__(self, other):
        return self.text == other.text


class Corpus(object):
    def __init__(self, qrels_path, query_path):
        self.qrels_path = qrels_path
        self.query_path = query_path

        self.qrels_file = open(self.qrels_path, 'r')

        if query_path is not None:
            self.queryFile = open(self.query_path, 'r')
        else:
            self.qrels_file = None

    def close(self):
        self.qrels_file.close()
        if self.qrels_file is not None:
            self.queryFile.close()


class ERDCorpus(Corpus):
    def get_qrels(self):
        queries = json.load(self.queryFile)
        qrels = {}

        for key in queries:
            qrels["erd14-" + key] = (Query("erd14-" + key, queries[key].strip()))

        for line in self.qrels_file:
            attribs = line.replace('\n', '').split('\t')

            if len(attribs) > 1:
                qrels["erd14-" + attribs[0]].annotations.extend(attribs[2:len(attribs)])

        return qrels


class ESDBpediaV2Corpus(Corpus):
    def get_qrels(self):
        queries = {}

        for line in self.queryFile:
            attribs = line.replace('\n', '').split('\t')
            queries["esdbpediav2-" + attribs[0]] = Query("esdbpediav2-" + attribs[0], attribs[1].strip())

        for line in self.qrels_file:
            attribs = line.replace('\n', '').split('\t')

            if int(attribs[3]) > 0:
                queries["esdbpediav2-" + attribs[0]].annotations.append(attribs[2])

        return queries


class YSQLECorpus(Corpus):
    def get_qrels(self):
        qrels = {}

        tree = ET.parse(self.qrels_path)

        root = tree.getroot()

        for session in root:
            currentID = session.attrib['id']
            qid = 0
            for query in session:
                qrels["ysqle-" + currentID + '-' + str(qid)] = Query("ysqle-" + currentID + '-' + str(qid),
                                                                     query[0].text.strip())

                for annotation in query:
                    if annotation.tag == 'annotation':
                        for target in annotation:
                            if target.tag == 'target':
                                qrels["ysqle-" + currentID + '-' + str(qid)].annotations.append(target.text)

                qid += 1

        return qrels


def get_num_queries_with_annotations(qrels):
    i = 0
    for id in qrels:
        if len(qrels[id].annotations) > 0:
            i = i + 1

    return i


def merge_dicts(dict1, dict2):
    for id2 in dict2:
        if id2 in dict1:
            dict1[id2].annotations.extend(dict2[id2].annotations)
        else:
            if dict2[id2] in dict1.values():
                for id1 in dict1:
                    if dict2[id2] == dict1[id1]:
                        dict1[id1].annotations.extend(dict2[id2].annotations)

            else:
                dict1[id2] = dict2[id2]

    return dict1


def print_stats(corpus, qrels):
    print(corpus + " Corpus")
    print("------------------")
    print("#Queries: " + str(len(qrels)))
    print("#Queries with annotations: " + str(get_num_queries_with_annotations(qrels)))
    print("#Queries without annotations: " + str(len(qrels) - get_num_queries_with_annotations(qrels)))
    print('\n')


def main():
    corpora_dir = '../data/corpora/'
    aggregated_qrels = {}

    erd_corpus = ERDCorpus(corpora_dir + 'corpus-erd-elq/qrels_ERD_elq.txt',
                          corpora_dir + 'corpus-erd-elq/queries_ERD.json')

    erd_qrels = erd_corpus.get_qrels()
    print_stats("ERD14", erd_qrels)
    erd_corpus.close()

    es_dbpedia_corpus = ESDBpediaV2Corpus(corpora_dir + 'corpus-entity-search-dbpedia-v2/qrels-v2.txt',
                                          corpora_dir + 'corpus-entity-search-dbpedia-v2/queries-v2.txt')

    es_dbpedia_qrels = es_dbpedia_corpus.get_qrels()

    print_stats("Entity Search DBpedia V2", es_dbpedia_qrels)

    ysqle_corpus = YSQLECorpus(corpora_dir + 'corpus-ysqle/ydata-search-query-log-to-entities-v1_0.xml', None)
    ysqle_qrels = ysqle_corpus.get_qrels()

    print_stats("YSQLE", ysqle_qrels)

    aggregated_qrels = merge_dicts(aggregated_qrels, erd_qrels)
    aggregated_qrels = merge_dicts(aggregated_qrels, es_dbpedia_qrels)
    aggregated_qrels = merge_dicts(aggregated_qrels, ysqle_qrels)

    print_stats("Aggregated", aggregated_qrels)

    sorted_qrels = col.OrderedDict(sorted(aggregated_qrels.items(), key=lambda t: t[1].id.lower()))

    with open(corpora_dir + 'aggregated-corpus.csv', 'w') as output:
        for id in sorted_qrels:
            output.write(str(aggregated_qrels[id]) + '\n')


if __name__ == '__main__':
    main()
