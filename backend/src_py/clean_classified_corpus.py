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
import numpy as np
import re


def clean(data):
    data.set_index("ID", inplace=True)

    data = filter_by_difficulty(data)
    lower_queries(data)
    normalize_queries(data)
    remove_duplicates(data)
    data.sort_values("QUERY", inplace=True)

    return data


def lower_queries(data):
    data["QUERY"] = data["QUERY"].map(lambda q: q.lower())


def normalize_queries(data):
    data["QUERY"] = data["QUERY"].map(lambda q: re.sub("[ ]+", " ", q))
    data["QUERY"] = data["QUERY"].map(lambda q: q.replace("\'", ""))


def filter_by_difficulty(data):
    return data[data["DIFFICULTY"] >= 3]


def remove_duplicates(data):
    data.drop_duplicates(subset="QUERY", inplace=True)


def main():
    data = pd.read_csv("../data/corpora/corpus-webis-aggregated/webis-aggregated-corpus-classification.csv",
                       delimiter=";")
    data = clean(data)

    data.to_csv("../data/corpora/corpus-webis-aggregated/webis-aggregated-corpus-high-difficulty.csv")

    # data.to_csv(path_or_buf="../data/corpora/corpus-webis-aggregated/webis-aggregated-corpus-cc.csv", sep=";")

    data["MERGE-KEY"] = 0

    tasks = pd.DataFrame({"MERGE-KEY": [0, 0], "TASK": ["exer", "imer"]})
    merged = data.reset_index().merge(tasks, on="MERGE-KEY", how="left").set_index(["ID", "TASK"])
    merged.drop(columns=["MERGE-KEY"], inplace=True)

    for i in range(10):
        merged["MENTION#" + str(i)] = ""
        merged["URL#" + str(i)] = ""
        merged["RELEVANCE#" + str(i)] = ""

    merged.to_csv(path_or_buf="../data/corpora/corpus-webis-query-interpretation/discarded-queries-er.csv", sep=";")

    print(merged)


if __name__ == '__main__':
    main()
