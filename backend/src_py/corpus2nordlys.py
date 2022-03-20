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

import argparse
import json
import os


def getJSONQueries(corpusPath, delimiter):
    queries = {}

    with open(corpusPath) as corpusFile:
        for line in corpusFile:
            attribs = line.replace('\n', '').split(delimiter)

            for i in range(2, len(attribs)):
                if "+" not in attribs[i]:
                    queries[attribs[0] + "-" + str(i - 2)] = attribs[i]

        corpusFile.close()

    return json.dumps(queries, separators=(',\n', ': '))


def getJSONNordlysConfig(jsonQueryPath, outPath):
    return json.dumps({'query_file': jsonQueryPath, 'output_file': outPath}, separators=(',\n', ': '))


def runNordlys(configPath):
    os.system('cd ../nordlys;python -m nordlys.services.el -c ' + configPath)


def writeFile(outPath, content):
    with open(outPath, 'w') as outFile:
        outFile.write(content)

        outFile.close()


def main(corpusPath, delimiter):
    pathParts = corpusPath.split("/")

    corpusFileName = pathParts[len(pathParts) - 1].replace('.csv', '')
    corpusDirPath = '/'.join(pathParts[0:len(pathParts) - 1])

    jsonString = getJSONQueries(corpusPath, delimiter)
    queryOutPath = corpusDirPath + '/' + corpusFileName + '-linked.json'
    writeFile(queryOutPath, jsonString)

    jsonString = getJSONNordlysConfig(queryOutPath, queryOutPath)

    configOutPath = '../config/nordlys/' + corpusFileName + '-config.json'
    writeFile(configOutPath, jsonString)

    runNordlys(configOutPath)


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description='Convert corpus to a nordlys compatible format...')
    parser.add_argument('-p', metavar='corpus_path', required=True, dest='corpusPath', type=str)
    parser.add_argument('-d', metavar='delimiter', required=False, dest='delimiter', type=str, default=';')
    args = parser.parse_args()

    main(args.corpusPath, args.delimiter)
