# A Test Collection for Entity Retrieval in DBpedia

## Collection

The data collection used is DBpedia 3.7. You may obtain it from <http://wiki.dbpedia.org/Downloads37>.

## Queries

The `queries.txt` file contains one query per line, 485 in total, where each line has two tab-separated fields: queryID
and query text. QueryID-s follow a `{source}-{X}` pattern, where `{source}` denotes the source and `{X}` refers to the
original queryID (used at the given benchmark where the query originates from).

- `INEX-LD-{X}`: 100 queries from the INEX 2012 Linked Data track
- `INEX-XER-{X}`: 55 queries from the INEX 2009 Entity Ranking track
- `QALD2_te-{X}`, `QALD2-tr-{X}`: 140 queries from the Question Answering over Linked Data Challenge (`tr` and `te` are
  training test queries in the original query set, respectively)
- `SemSearch_ES-{X}`: 130 queries from the entity search task of the 2010 and 2011 Semantic Search Challenge
- `SemSearch_LS-{X}`: 43 queries from the list search task of the 2011 Semantic Search Challenge
- `TREC_Entity-{X}`: 17 queries from the TREC 2009 Entity track

In all cases, we use only the keyword part of the query and ignore any additional markup, type information, or other
hints (like example entities) that may be available as part of the topic definition according to the original task
setup.

## Qrels

The `qrels.txt` file contains relevance jugments in standard TREC format. A couple of remarks:

- The file contains URIs of relevant entities, and only those. (That is, URIs that were judged as non-relevant are not
  included.)
- It is reasonable to assume that all relevant entities have been found (as of version 3.7 of DBpedia), although this
  has not been checked.
- Relevance is taken to be binary. Two query subsets, however, have graded judgments and these have been preserved in
  the file. Specifically, `SemSearch_ES` and `SemSearch_LS` queries have fair (1) and excellent (2) relevance levels.
- DBpedia URIs have been normalized to conform with the encoding used by the official DBpedia dump; we replaced redirect
  pages with the URIs they redirect to, and filtered out URIs that are not entity pages.

### DBpedia 3.9 version

An updated version of the qrels is made available for DBpedia 3.9.

- URIs are prefixed and enclosed in angle brackets. I.e., `http://dbpedia.org/resource/XXX` is now written
  as `<dbpedia:XXX>`
- URI encoding has changed. Specifically, some characters are no longer percent-encoded in URIs as of DBpedia 3.8  (
  see <http://wiki.dbpedia.org/URIencoding>)
- Fragment identifiers are removed. I.e., `<dbpedia:XXX#YYY>` is replaced with `<dbpedia:XXX>`.
- URIs that became redirected in version 3.9 are replaced with the page they redirect to.
- In case of URI collision (i.e., the replacement URI already exists in the qrels file with a conflicting judgment), the
  highest relevance level is kept. (Since relevance is intended to be taken binary, it does not really matter.)
- URIs that are not in DBpedia 3.9 or are not proper entity pages (including `http://dbpedia.org/resource/Portal:XXX`
  and `http://dbpedia.org/resource/Book:XXX`) are removed
- Mind that no additional relevance assessments have been performed; the result sets have only been filtered but not
  extended. It means that there might be additional relevant entities out there that have been added to DBpedia since
  the release of the 3.7 version.
- In total, 132 URIs have been removed. This should have negligible impact. The new qrels file contains 12958 relevant
  entities in total (the number of queries is unchanged, 485).

## Baseline runs

The `baseline-runs.zip` file contains 7 baseline runs in standard TREC runfile format; these correspond to the runs
reported in Table 2 of the paper. See Sections 3.1 and 3.2 in the paper for further details.

## Reference

If using our collection, please refer to is as the "\textsc{DBpedia-Entity} test set" and cite the following paper:

<pre>
@inproceedings{Balog:2013:TCE, 
   author = {Balog, Krisztian and Neumayer, Robert},
   title = {A Test Collection for Entity Retrieval in DBpedia},
   booktitle = {Proceedings of the 36th international ACM SIGIR conference on Research and development in Information Retrieval},
   series = {SIGIR '13},
   pages = {737--740},
   year = {2013},
   publisher = {ACM}
} 
</pre>

## Contact

In case of questions, feel free to contact me at <krisztian.balog@uis.no>.
