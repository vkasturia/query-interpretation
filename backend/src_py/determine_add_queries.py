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
        "../data/corpora/corpus-webis-query-interpretation/webis-query-interpretation-corpus.json"
    )

    gerdaq_corpus = corpora.GerdaqCorpus("../data/corpora/corpus-gerdaq/")

    with open("../data/corpora/corpus-webis-query-interpretation/queries_to_extend.txt", "w+") as out_file:
        for gerdaq_query in gerdaq_corpus.queries:
            if not webis_corpus.contains(gerdaq_query["query"]):
                out_file.write(gerdaq_query["query"] + "\n")


if __name__ == '__main__':
    main()
