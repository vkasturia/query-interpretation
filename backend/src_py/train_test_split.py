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
import random
import copy
import time

def compute_session_pool(corpus_data):
    session_pool = {}

    for query in corpus_data["queries"]:
        if query["id"].startswith("ysqle"):
            id = query["id"]
            id_attribs = id.split("-")

            id_without_session = "-".join(id_attribs[:-1])

            if id_without_session not in session_pool:
                session_pool[id_without_session] = []

            session_pool[id_without_session].append(query)
        else:
            session_pool[query["id"]] = [query]

    return session_pool


def compute_cat_distribution(queries):
    distribution = {}
    for query in queries:
        if ",".join(query["categories"]) not in distribution:
            distribution[",".join(query["categories"])] = 0

        distribution[",".join(query["categories"])] += 1

    distribution = {key: value / len(queries) for key, value in distribution.items()}
    return distribution


def compute_size(session_pool):
    num_queries = 0

    for session in session_pool:
        num_queries += len(session)

    return num_queries


def get_init_split(session_pool, num_queries, train_portion=0.8):
    session_pool = copy.deepcopy(list(session_pool.values()))

    train_set = []
    test_set = []

    train_size = round(num_queries * train_portion)

    while compute_size(train_set) < train_size:
        value = random.choice(list(session_pool))
        train_set.append(value)
        session_pool.remove(value)

    for session in session_pool:
        test_set.append(session)

    print("Init train portion: " + str(compute_size(train_set) / num_queries))
    print("Init test portion: " + str(compute_size(test_set) / num_queries))

    return train_set, test_set


def split_error(goal, train, test):
    absolute_error = 0

    for key, value in goal.items():
        try:
            absolute_error += abs(goal[key] - train[key])
        except KeyError:
            absolute_error += goal[key]

        try:
            absolute_error += abs(goal[key] - test[key])
        except KeyError:
            absolute_error += goal[key]

    return absolute_error


def compose_set(split_set):
    res_set = []
    for entry in split_set:
        res_set.extend(entry)

    return res_set


def swap_elements(train_set, test_set):
    train_element = random.choice(train_set)
    test_element = random.choice(test_set)

    train_set.append(test_element)
    test_set.remove(test_element)

    test_set.append(train_element)
    train_set.remove(train_element)

    return train_set, test_set


def main():
    with open(
            "../data/corpora/corpus-webis-query-interpretation/webis-query-interpretation-corpus.json") as corpus_file:
        corpus_data = json.load(corpus_file)
        session_pool = compute_session_pool(corpus_data)
        goal_distribution = compute_cat_distribution(corpus_data["queries"])

        train_set, test_set = get_init_split(session_pool, len(corpus_data["queries"]))

        while True:
            train_distribution = compute_cat_distribution(compose_set(train_set))
            test_distribution = compute_cat_distribution(compose_set(test_set))
            error = split_error(goal_distribution, train_distribution, test_distribution)

            tweaked_train_set, tweaked_test_set = swap_elements(copy.deepcopy(train_set), copy.deepcopy(test_set))

            train_distribution = compute_cat_distribution(compose_set(tweaked_train_set))
            test_distribution = compute_cat_distribution(compose_set(tweaked_test_set))
            tweaked_error = split_error(goal_distribution, train_distribution, test_distribution)

            if tweaked_error < error:
                train_set = tweaked_train_set
                test_set = tweaked_test_set

                print("-------------------------------------")
                print("Error: " + str(tweaked_error))
                print("Train portion: " + str(compute_size(train_set) / len(corpus_data["queries"])))
                print("Test portion: " + str(compute_size(test_set) / len(corpus_data["queries"])))

            if tweaked_error < 0.025:
                break

        train_file = {"queries": []}
        train_file["queries"].extend(compose_set(train_set))

        with open("../data/corpora/corpus-webis-query-interpretation/webis-query-interpretation-corpus-train.json", "w+") as out_file:
            json.dump(train_file, out_file, indent=4)

        test_file = {"queries": []}
        test_file["queries"].extend(compose_set(test_set))

        with open("../data/corpora/corpus-webis-query-interpretation/webis-query-interpretation-corpus-test.json", "w+") as out_file:
            json.dump(test_file, out_file, indent=4)

        print(len(train_file["queries"]))
        print(len(test_file["queries"]))

        print(train_distribution)
        print(test_distribution)


if __name__ == '__main__':
    main()
