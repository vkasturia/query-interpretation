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

import pandas as pd
import csv
import math
import re

from django.core.validators import URLValidator
from django.core.exceptions import ValidationError


def load(file_path):
    return pd.read_csv(file_path, sep=";")


def split_to_task(data):
    exer = data[data["TASK"] == "exer"].drop(["TASK"], axis=1)
    imer = data[data["TASK"] == "imer"].drop(["TASK"], axis=1)

    exer.drop_duplicates(subset="QUERY", inplace=True)
    imer.drop_duplicates(subset="QUERY", inplace=True)

    exer = exer.set_index("ID")
    imer = imer.set_index("ID")

    return exer, imer


def filter_set(task_data):
    filtered = task_data[task_data["DIFFICULTY"] >= 10]
    task_data = task_data[task_data["DIFFICULTY"] < 4]

    return task_data, filtered


def create_markdown_table(filtered):
    markdown = ""
    for row in filtered.itertuples():
        markdown += "| " + row[0] + " | " + row[1] + " | | |\n"

    print(markdown)


def get_max_entities(data):
    max_entities = 0

    for row in data.itertuples():
        for i in range(len(row)):
            if isinstance(row[i], float):
                if math.isnan(row[i]):
                    num = (i - 5) / 3

                    if num > max_entities:
                        max_entities = num

                    break

    return max_entities


def validate_entities(data):
    print("Validate entities...")
    typos = 0
    special_chars = 0
    url_val = URLValidator()
    validata = True

    for row in data.itertuples():
        for i in range(5, len(row), 3):
            if not isinstance(row[i], float):
                if row[i] not in row[2]:
                    print("ERROR: Typo in row")
                    print(row)
                    typos += 1
                    validata = False

                if not re.match("[0-9a-z ]+", row[i]):
                    special_chars += 1
                    print("ERROR: Special char in row")
                    print(row)
                    validata = False

            if not isinstance(row[i + 1], float):
                if row[i+1].startswith("https"):
                    try:
                        url_val(row[i+1])
                    except ValidationError:
                        print("ERROR: Non valid url")
                        print(row)
                        validata = False

                if math.isnan(row[i + 2]):
                    print("ERROR: non existing relevance")
                    print(row)
                    validata = False

    print("---------")
    print("{} typos in mentions".format(typos))
    print("{} special chars in mentions".format(special_chars))
    return validata


def validate_corpus(data):
    data = data.set_index("ID")
    relevant, non_relevant = filter_set(data)

    print(len(relevant))

    max = get_max_entities(relevant)
    print(max)

    return validate_entities(relevant)


def main():
    data = load("../data/corpora/corpus-webis-aggregated/webis-aggregated-corpus-er.csv")

    if validate_corpus(data):
        exer, imer = split_to_task(data)

        exer, filtered = filter_set(exer)
        imer, filtered = filter_set(imer)

        exer.to_csv("../data/corpora/corpus-webis-aggregated/webis-aggregated-corpus-exer.csv", sep=";", quoting=csv.QUOTE_ALL)
        imer.to_csv("../data/corpora/corpus-webis-aggregated/webis-aggregated-corpus-imer.csv", sep=";", quoting=csv.QUOTE_ALL)

        create_markdown_table(filtered)


if __name__ == '__main__':
    main()
