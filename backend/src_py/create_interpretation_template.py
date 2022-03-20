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


def load(file_path):
    return pd.read_csv(file_path, sep=",", quoting=csv.QUOTE_ALL)


def main():
    data = load("../data/corpora/corpus-webis-query-interpretation/discarded-queries-er.csv")
    data = data.drop_duplicates(subset="ID")

    data = data[["ID", "QUERY", "CAT", "DIFFICULTY"]]
    data["INT-ID"] = ""
    data["REL"] = ""
    data["EQV"] = ""
    data["COMMENT"] = ""

    data = data.set_index("ID")
    data.to_csv("../data/corpora/corpus-webis-query-interpretation/discarded-queries-interpretation.csv",
                sep=",", quoting=csv.QUOTE_ALL)


if __name__ == '__main__':
    main()
