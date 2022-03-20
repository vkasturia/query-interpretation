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

import MySQLdb
import pandas as pd
import csv


def main():
    print("Open database \"wikipedia\"...", end="")
    db = MySQLdb.connect(host="localhost",
                         user="root",
                         passwd="amneziaHAZE",
                         db="wikipedia")

    print("Done")

    print("Create cursor...", end="")
    cursor = db.cursor()
    print("Done")

    statement = (
        "SELECT rd_from, rd_title, page_title FROM redirect "
        "LEFT JOIN page ON redirect.rd_from=page.page_id "
        "WHERE rd_namespace=0 "
    )

    print("Execute SQL...", end="")
    cursor.execute(statement)
    print("Done")


    redirects = {}

    for data in cursor.fetchall():
        if data[1] is not None and data[2] is not None:
            redirects[data[2].decode("UTF-8")] = data[1].decode("UTF-8")
        else:
            print(data)

    print("Write to file...", end="")
    dataframe = pd.DataFrame.from_dict(redirects, orient="index")

    dataframe.to_csv("/media/storage1/corpora/corpus-wikipedia/redirects-20180220/redirects.csv",
                     header=False, quoting=csv.QUOTE_ALL)
    print("Done")


if __name__ == '__main__':
    main()

