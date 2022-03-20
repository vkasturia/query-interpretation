import json
import xml.etree.ElementTree as ET

class QueryInterpretationCorpus:
    queries = []

    def __init__(self, path):
        self.parse(path)

    def parse(self, path):
        with open(path) as file:
            self.queries = json.loads(file.read())["queries"]

    def contains(self, query_text):
        for query in self.queries:
            if query["query"] == query_text:
                return True

        return False


class ESDBCorpus(QueryInterpretationCorpus):
    def __init__(self, path):
        super().__init__(path)

    def parse(self, path):
        query_list = {}

        with open(path + "queries-v2.txt") as query_file:
            for line in query_file:
                attribs = line.split("\t")
                query_list[attribs[0]] = {"id": attribs[0], "query": attribs[1].replace("\n", ""), "entities": []}

        with open(path + "qrels-v2.txt") as qrels_file:
            for line in qrels_file:
                attribs = line.split("\t")

                query_list[attribs[0]]["entities"].append({"entity": attribs[2], "relevance": int(attribs[3])})

        self.queries = query_list.values()

    def get_irrelevant_entities(self):
        results = []
        for query in self.queries:
            for entity in query["entities"]:
                if entity["relevance"] == 0:
                    results.append((query["query"], entity["entity"]))

        return results


class GerdaqCorpus(QueryInterpretationCorpus):
    def __init__(self, path):
        super().__init__(path)

    def parse(self, path):
        files = [path + "gerdaq_test.xml",
                 path + "gerdaq_devel.xml",
                 path + "gerdaq_trainingA.xml"]

        for file in files:
            tree = ET.parse(file)
            root = tree.getroot()

            text_iter = root.itertext()

            query = ""

            for text in text_iter:
                if text == "\n":
                    self.queries.append({"query": query})
                    query = ""
                else:
                    query += text
