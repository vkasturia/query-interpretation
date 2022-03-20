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

import mysql.connector as mysql
import urllib.parse


def parse_dbpedia(path, unique_pages):
    with open(path) as in_file:
        for line in in_file:
            title = line.split(" ")[0].replace("http://dbpedia.org/resource/", "").strip("<").strip(">")

            url = "https://en.wikipedia.org/wiki/" + urllib.parse.quote(title)

            # if url not in unique_pages:
            #     print(url)

            unique_pages.add(
                url
            )


def main():
    unique_pages = set()

    connection = mysql.connect(
        user="root",
        password="amneziaHAZE",
        database="wikipedia_20190701",
        host="localhost",
        port=3306
    )

    cursor = connection.cursor()

    cursor.execute("SELECT * FROM disambiguation_pages;")

    for row in cursor.fetchall():
        if row[2] is None:
            unique_pages.add(
                "https://en.wikipedia.org/wiki/" + urllib.parse.quote(row[1])
            )
        else:
            unique_pages.add(
                "https://en.wikipedia.org/wiki/" + urllib.parse.quote(row[2])
            )

    print(len(unique_pages))
    parse_dbpedia("/media/storage1/corpora/corpus-dbpedia-2016-10/disambiguations_en.tql", unique_pages)
    print(len(unique_pages))

    with open("../data/disambiguation_pages_20190701.csv", "w+") as outfile:
        for url in unique_pages:
            outfile.write(url + "\n")



if __name__ == '__main__':
    main()
