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
import math
import pandas as pd
import re
from django.core.exceptions import ValidationError
from django.core.validators import URLValidator


class Query(object):

    def __init__(self, id, query, difficulty, categories,
                 explicit_entities=None, implicit_entities=None, related_entities=None,
                 interpretations=None):
        self.id = id
        self.query = query
        self.difficulty = difficulty
        self.categories = categories
        self.explicit_entities = explicit_entities
        self.implicit_entities = implicit_entities
        self.related_entities = related_entities
        self.interpretations = interpretations

        if self.explicit_entities is None:
            self.explicit_entities = []

        if self.implicit_entities is None:
            self.implicit_entities = []

        if self.related_entities is None:
            self.related_entities = []

        if self.interpretations is None:
            self.interpretations = []

    def __eq__(self, other):
        if isinstance(other, Query):
            return self.id == other.id

        return False

    def __ne__(self, other):
        return not self.__eq__(other)

    def __str__(self):
        return "{}:{}".format(self.id, self.query)

    def add_explicit_entity(self, entity):
        self.explicit_entities.append(entity)

    def add_implicit_entity(self, entity):
        self.implicit_entities.append(entity)

    def add_related_entity(self, entity):
        self.related_entities.append(entity)

    def add_interpretation(self, interpretation_tuple):
        self.interpretations.append(interpretation_tuple)

    def dict(self):
        values = self.__dict__

        values["explicit_entities"] = [x.__dict__ for x in self.explicit_entities]
        values["implicit_entities"] = [x.__dict__ for x in self.implicit_entities]
        values["interpretations"] = [x.__dict__ for x in self.interpretations]

        return values


class Entity(object):

    def __init__(self, mention, entity, relevance):
        self.mention = mention
        self.entity = entity
        self.relevance = relevance

    def __str__(self):
        return "({}, {}, {})".format(self.mention, self.entity, self.relevance)


class Interpretation(object):

    def __init__(self, id, interpretation, relevance, comment="", equivalent=""):
        self.id = id
        self.interpretation = interpretation
        self.relevance = relevance
        self.equivalent = equivalent
        self.comment = comment

    def __str__(self):
        return "({}, {}, {})".format(self.id, self.interpretation, self.relevance)


def load(file_path, sep=";", header="infer"):
    return pd.read_csv(file_path, sep=sep, header=header)


def pre_process(data):
    data = data[data["DIFFICULTY"] < 4]
    data = data[data["CAT"] != "RLQ"]

    return data


def split_in_tasks(data):
    exer_data = data[data["TASK"] == "exer"]
    imer_data = data[data["TASK"] == "imer"]

    exer_data = exer_data.drop("TASK", axis=1)
    imer_data = imer_data.drop("TASK", axis=1)

    return exer_data, imer_data


def validate_er(data):
    print("Validate ER...")
    url_val = URLValidator()

    for index, row in data.iterrows():
        if not field_is_empty(row[0]):
            if not re.match("[$a-z0-9 ]+", row[0]):
                print("ERROR: Contains character")
                print(index + " : " + row[0])
                print()

            if isinstance(row[2], int):
                if row[2] not in range(1, 5 + 1):
                    print("ERROR: Wrong difficulty")
                    print(index + " : " + row[0])
                    print()
            else:
                print("ERROR: No difficulty")
                print(index + " : " + row[0])
                print()

            for i in range(3, len(row), 3):
                if field_is_empty(row[i]):
                    if not field_is_empty(row[i + 1]) or not field_is_empty(row[i + 2]):
                        print("ERROR: Empty mention")
                        print(index + " : " + row[0])
                        print()
                    else:
                        break
                else:
                    if row[i] not in row[0]:
                        print("ERROR: Typo in mention")
                        print(index + " : " + row[0])
                        print()

                    if not field_is_empty(row[i + 1]):
                        if row[i + 1].startswith("http"):
                            for url in row[i + 1].split("|"):
                                try:
                                    url_val(url)
                                except ValidationError:
                                    print("ERROR: URL validation error: " + url)
                                    print(index + " : " + row[0])
                                    print()

                    if not field_is_empty(row[i + 2]):
                        if 0 > int(row[i + 2]) > 2:
                            print("ERROR: Wrong relevance")
                            print(index + " : " + row[0])
                            print()
                    else:
                        print("ERROR: Missing relevance")
                        print(index + " : " + row[0])
                        print()
        else:
            print("ERROR: Empty query")
            print(index + " : " + row[0])
            print()

    print("Done.")


def validate_interpretation(data):
    print("Validate interpretation...")
    for index, row in data.iterrows():
        if field_is_empty(index[1]):
            if not field_is_empty(row[4]):
                print("ERROR: Missing id of interpretation")
                print(str(index) + " : " + row[0])
                print()

        if field_is_empty(row[3]):
            if not field_is_empty(row[4]):
                print("ERROR: Missing relevance")
                print(str(index) + " : " + row[0])
                print()

        for i in range(6, len(row)):
            if field_is_empty(row[i]):
                break

            if not row[i].startswith("http"):
                if row[i] not in row[0] and re.match("[a-z0-9 ]+", row[i]):
                    print("ERROR: Typo in interpretation")
                    print(str(index) + " : " + row[i])
                    print()

    print("Done.")


def check_index_integrity(data):
    print("Check interpretation index integrity....")

    int_ids = []
    current_id = None

    for index, row in data.iterrows():
        if current_id is None:
            current_id = row[0]

        if row[0] != current_id:
            current_id = row[0]
            int_ids = []

        int_id = row[4]

        if int_id in int_ids:
            print("ERROR: Wrong int id")
            print(str(index) + " : " + row[1])
            print()

        int_ids.append(row[4])

        if len(int_ids) > 1:
            if int_ids[len(int_ids) - 1] - int_ids[len(int_ids) - 2] != 1:
                print("WARNING: Wrong int id assignment...")
                print(str(index) + ": " + row[1])
                print()

    print("Done")


def field_is_empty(field):
    if isinstance(field, float):
        if math.isnan(field):
            return True

    return False


def parse(exer_data, imer_data, rel_en_data, interpretations):
    queries = []

    for index, row in exer_data.iterrows():
        categories = row[1].split(", ")

        query = Query(index, row[0], row[2], categories)

        for i in range(3, len(row), 3):
            if field_is_empty(row[i]):
                break

            entity = Entity(row[i], row[i + 1].split("|"), int(row[i + 2]))
            query.add_explicit_entity(entity)

        for i in range(3, len(imer_data.loc[index]), 3):
            imer_row = imer_data.loc[index]

            if field_is_empty(imer_row[i]):
                break

            entity = Entity(imer_row[i], imer_row[i + 1].split("|"), int(imer_row[i + 2]))
            query.add_implicit_entity(entity)

        for _, rel_row in rel_en_data.loc[rel_en_data[0] == query.query].iterrows():
            rel_entity = rel_row[1].strip("<").strip(">").replace("dbpedia:", "https://en.wikipedia.org/wiki/")
            query.add_related_entity(rel_entity)

        for int_id, int_row in interpretations.loc[index].iterrows():
            if not field_is_empty(int_id):
                relevance = int_row[3]
                interpretation = []
                equivalent = None

                if not field_is_empty(int_row[4]):
                    equivalent = int(int_row[4])

                comment = int_row[5]
                if field_is_empty(int_row[5]):
                    comment = None

                for i in range(6, len(int_row)):
                    if field_is_empty(int_row[i]):
                        break

                    interpretation.append(int_row[i])

                query.add_interpretation(
                    Interpretation(int(int_id), interpretation, int(relevance), comment=comment, equivalent=equivalent))

        queries.append(query)

    return queries


def main():
    er_data = load("../data/corpora/corpus-webis-query-interpretation/webis-query-interpretation-corpus-er.csv")

    columns = er_data.columns

    er_data = pd.concat(
        [er_data, load("../data/corpora/corpus-webis-query-interpretation/manual-added-queries-er.csv")],
        ignore_index=True)

    er_data = pre_process(er_data)

    er_data = pd.concat(
        [er_data, load("../data/corpora/corpus-webis-query-interpretation/discarded-queries-er.csv")],
        ignore_index=True)

    er_data = er_data.reindex(columns, axis=1)
    er_data.set_index("ID", inplace=True)

    exer_data, imer_data = split_in_tasks(er_data)

    exer_data = exer_data.drop_duplicates(subset="QUERY")
    imer_data = imer_data.drop_duplicates(subset="QUERY")

    rel_en_data = load("../data/corpora/corpus-webis-query-interpretation/related-entity-candidates.tsv",
                       sep="\t", header=None)

    print("Queries: " + str(len(exer_data)))

    validate_er(exer_data)
    validate_er(imer_data)

    interpretation_data = load(
        "../data/corpora/corpus-webis-query-interpretation/webis-query-interpretation-corpus-interpretation.csv")

    columns = interpretation_data.columns

    interpretation_data = interpretation_data.append(
        load("../data/corpora/corpus-webis-query-interpretation/manual-added-queries-interpretation.csv"),
        ignore_index=True)

    interpretation_data = interpretation_data.append(
        load("../data/corpora/corpus-webis-query-interpretation/discarded-queries-interpretation.csv"),
        ignore_index=True)

    interpretation_data = interpretation_data.reindex(columns, axis=1)

    check_index_integrity(interpretation_data)

    interpretation_data.set_index(["ID", "INT-ID"], inplace=True)

    validate_interpretation(interpretation_data)

    queries = parse(exer_data, imer_data, rel_en_data, interpretation_data)

    with open("../data/corpora/corpus-webis-query-interpretation/webis-query-interpretation-corpus.json", "w", encoding="utf-8") as file:
        json.dump({"queries": [x.dict() for x in queries]}, file, indent=4, ensure_ascii=False)


if __name__ == '__main__':
    main()
