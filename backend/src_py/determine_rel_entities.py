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

import parsing.corpora as corpora


def main():
    webis_corpus = corpora.QueryInterpretationCorpus(
        "../data/corpora/corpus-webis-query-interpretation/webis-query-interpretation-corpus.json")

    esdb_corpus = corpora.ESDBCorpus(
        "../data/corpora/corpus-entity-search-dbpedia-v2/"
    )

    irrelevant = esdb_corpus.get_irrelevant_entities()

    with open("../data/corpora/corpus-webis-query-interpretation/related-entity-candidates.txt", "w+") as out_file:
        for entity in irrelevant:
            if webis_corpus.contains(entity[0].lower()):
                out_file.write(entity[0] + "\t" + entity[1] + "\n")


if __name__ == '__main__':
    main()
