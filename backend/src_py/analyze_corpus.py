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
import operator
import matplotlib.pyplot as plt
import numpy as np

from matplotlib.ticker import MaxNLocator


def main():
    difficulty_distribution = {1: 0, 2: 0, 3: 0, 4: 0, 5: 0}
    classes_distribution = {}
    query_length_distribution = {}
    query_length_ambiguity_tuples = []
    query_length_max_ambiguity = {}
    query_length_mentions = {}
    length_ambiguity_distribution = {}

    query_ambiguity_ranking = {}

    num_queries_without_entities = 0
    num_queries_with_explicit = 0
    num_queries_with_implicit = 0
    num_queries_with_wiki_entities = 0

    num_explicit_wiki_entities = 0

    query_length_mean = 0
    query_length_max = 0
    query_with_max_length = ""

    avg_interpretations = 0
    avg_explicit_entities = 0
    avg_implicit_entities = 0
    avg_mentions = 0

    overall_mentions = {}

    with open(
            "../data/corpora/corpus-webis-query-interpretation/webis-query-interpretation-corpus-train.json") as jsonFile:
        json_dict = json.loads(jsonFile.read())
        print(len(json_dict["queries"]))

    for query in json_dict["queries"]:
        difficulty_distribution[query["difficulty"]] += 1

        if str(query["categories"]) in classes_distribution:
            classes_distribution[str(query["categories"])] += 1
        else:
            classes_distribution[str(query["categories"])] = 1

        avg_explicit_entities += len(query["explicit_entities"])
        avg_implicit_entities += len(query["implicit_entities"])
        avg_interpretations += len(query["interpretations"])

        if len(query["explicit_entities"]) > 0:
            num_queries_with_explicit += 1

        if len(query["implicit_entities"]) > 0:
            num_queries_with_implicit += 1

        query_length = len(query["query"].split(" "))

        query_length_mean += query_length

        if query_length > query_length_max:
            query_length_max = query_length
            query_with_max_length = query["query"]

        if query_length not in length_ambiguity_distribution:
            length_ambiguity_distribution[query_length] = 0

        length_ambiguity_distribution[query_length] += len(query["interpretations"])

        if query_length not in query_length_distribution:
            query_length_distribution[query_length] = 0

        query_length_distribution[query_length] += 1

        query_length_ambiguity_tuples.append((query_length, len(query["interpretations"])))

        if query_length not in query_length_max_ambiguity:
            query_length_max_ambiguity[query_length] = 0

        if query_length_max_ambiguity[query_length] < len(query["interpretations"]):
            query_length_max_ambiguity[query_length] = len(query["interpretations"])

        if len(query["explicit_entities"]) == 0 == len(query["implicit_entities"]):
            num_queries_without_entities += 1

        query_ambiguity_ranking[query["query"]] = len(query["interpretations"])

        mention_count = {}
        distinct_mentions = set()

        has_wiki_entity = False
        has_expl_wiki_entity = False

        if query_length not in query_length_mentions:
            query_length_mentions[query_length] = 0

        for entity in query["explicit_entities"]:
            if entity["mention"] not in mention_count:
                mention_count[entity["mention"]] = 0

            mention_count[entity["mention"]] += 1

            distinct_mentions.add(entity["mention"])

            if "en.wikipedia.org" in entity["entity"][0]:
                has_wiki_entity = True
                has_expl_wiki_entity = True

                num_explicit_wiki_entities += 1

        for entity in query["implicit_entities"]:
            distinct_mentions.add(entity["mention"])

            if "en.wikipedia.org" in entity["entity"][0]:
                has_wiki_entity = True

        query_length_mentions[query_length] += len(distinct_mentions)
        avg_mentions += len(distinct_mentions)

        overall_mentions = {**overall_mentions, **mention_count}

        if has_wiki_entity:
            num_queries_with_wiki_entities += 1

    for i in range(1, 20):
        if i not in query_length_max_ambiguity:
            query_length_max_ambiguity[i] = 0

    query_length_mean /= len(json_dict["queries"])

    difficulty_mean = sum([k * v for k, v in difficulty_distribution.items()]) / sum(difficulty_distribution.values())

    avg_explicit_entities /= len(json_dict["queries"])
    avg_implicit_entities /= len(json_dict["queries"])
    avg_interpretations /= len(json_dict["queries"])

    query_length_values = [x[1] for x in sorted(query_length_distribution.items(), key=operator.itemgetter(0))]
    query_length_bins = [x[0] for x in sorted(query_length_distribution.items(), key=operator.itemgetter(0))]

    query_length_ambiguity_correlation = [x[1] / query_length_distribution[x[0]]
                                          for x in
                                          sorted(length_ambiguity_distribution.items(), key=operator.itemgetter(0))]

    query_length_ambiguity_reg = np.polyfit(query_length_bins, query_length_ambiguity_correlation, 1)
    query_length_ambiguity_reg_fn = np.poly1d(query_length_ambiguity_reg)

    query_length_ambiguity_lengths = [x[0] for x in query_length_ambiguity_tuples if x[1] > 0]
    query_length_ambiguity_ambiguity = [x[1] for x in query_length_ambiguity_tuples if x[1] > 0]

    reg = np.polyfit(query_length_ambiguity_lengths, query_length_ambiguity_ambiguity, 1)
    reg_fn = np.poly1d(reg)

    query_length_ambiguity_same_tuples = {}

    for element in query_length_ambiguity_tuples:
        if element[1] > 0:
            if element not in query_length_ambiguity_same_tuples:
                query_length_ambiguity_same_tuples[element] = 0

            query_length_ambiguity_same_tuples[element] += 1

    query_length_ambiguity_areas = [query_length_ambiguity_same_tuples[x] * 5 for x in query_length_ambiguity_tuples if
                                    x[1] > 0]

    query_length_max_ambiguity = sorted(query_length_max_ambiguity.items(), key=operator.itemgetter(0))

    query_ambiguity_ranking = sorted(query_ambiguity_ranking.items(), key=operator.itemgetter(1), reverse=True)

    overall_mentions = sorted(overall_mentions.items(), key=operator.itemgetter(1), reverse=True)

    print()
    print("Difficulty")
    print("---------------")
    print("Distribution: " + str(difficulty_distribution))
    print("Mean: {:.3}".format(difficulty_mean))
    print("---------------")

    print()
    print("Classification")
    print("---------------")
    print("Distribution: " + str(classes_distribution))

    print()
    print("Query Length")
    print("---------------")
    print("Query length distribution: " + str(query_length_distribution))
    print("Query length / ambiguity: " + str(query_length_ambiguity_correlation))
    print("Query Length Mean: " + str(query_length_mean))
    print("Query max length: " + str(query_length_max) + " | " + query_with_max_length)

    print()
    print("Entities")
    print("---------------")
    print("#Queries with explicit: " + str(num_queries_with_explicit))
    print("#Queries with implicit: " + str(num_queries_with_implicit))
    print("#Queries without entities: " + str(num_queries_without_entities))
    print("#Queries with wiki entities: " + str(num_queries_with_wiki_entities))
    print("#Explicit entities from Wikipedia: " +str(num_explicit_wiki_entities))
    print("Avg explicit entities: " + str(avg_explicit_entities))
    print("Avg implicit entities: " + str(avg_implicit_entities))
    print("Avg interpretations: " + str(avg_interpretations))
    print("Avg mentions: " + str(avg_mentions / len(json_dict["queries"])))

    print()
    print("Most ambigue queries")
    print("---------------")
    print("Ambiguity ranking: " + str(query_ambiguity_ranking))

    print()
    print("Ambiguous mentions: " + str(overall_mentions))

    for length, mentions in query_length_mentions.items():
        print(str(length) + ": " + str(mentions / query_length_distribution[length]))

    out_path = "../"

    plt.style.use("seaborn")

    plt.figure(1)
    plt.bar(query_length_bins, query_length_values, tick_label=query_length_bins)
    plt.xlabel("Number of query terms")
    plt.ylabel("Number of queries")
    # plt.title("Distribution of query lengths")

    plt.tight_layout()
    plt.savefig(out_path + "query_length_distribution.png")

    ax = plt.figure(2).gca()
    ax.xaxis.set_major_locator(MaxNLocator(integer=True))
    ax.locator_params(nbins=len(query_length_bins))
    ax.plot(query_length_bins, query_length_ambiguity_correlation,
            marker="o", markersize=5, label="lin. spline")
    ax.plot(query_length_bins, query_length_ambiguity_reg_fn(query_length_bins), "-.r", linewidth=1, alpha=0.5,
            label="regression line")
    ax.set_xlabel("Query Terms")
    ax.set_ylabel("Avg. Number of Interpretations")
    # ax.set_title("Correlation of Query Length and Number of Interpretations")
    ax.legend()

    plt.tight_layout()
    plt.savefig(out_path + "correlation_length_ambiguity.png")

    ax = plt.figure(3).gca()
    ax.xaxis.set_major_locator(MaxNLocator(integer=True))
    ax.locator_params(nbins=40)
    ax.set_xlabel("Number of query terms")
    ax.set_ylabel("Number of interpretations")
    # ax.set_title("Query Length/Ambiguity Correlation")
    ax.set_xticklabels(range(21))
    ax.set_ylim(0, 42)
    ax.scatter(query_length_ambiguity_lengths, query_length_ambiguity_ambiguity, marker="o",
               label="Number of queries", s=query_length_ambiguity_areas, alpha=0.25, linewidths=0.3, edgecolor="white")
    ax.plot(query_length_ambiguity_lengths, reg_fn(query_length_ambiguity_lengths), "-r", label="Reg. line",
            marker="x", linewidth=0.8)
    ax.plot([x[0] for x in query_length_max_ambiguity], [x[1] for x in query_length_max_ambiguity], ":", color="black",
            linewidth=0.8, label="Peak line")
    ax.legend()
    plt.tight_layout()
    plt.savefig(out_path + "correlation_length_ambiguity_scatter.png", dpi=300)
    plt.show()


if __name__ == '__main__':
    main()
