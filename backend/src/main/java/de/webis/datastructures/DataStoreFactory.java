/*
* Copyright 2018-2022 Vaibhav Kasturia <vbh18kas@gmail.com>
*
* Permission is hereby granted, free of charge, to any person obtaining a copy of this software 
* and associated documentation files (the "Software"), to deal in the Software without restriction, 
* including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, 
* and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, 
* subject to the following conditions:
*
* The above copyright notice and this permission notice shall be included in all copies or substantial 
* portions of the Software.
*
* THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT 
* LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. 
* IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, 
* WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE 
* OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
*/

package de.webis.datastructures;

import de.webis.datastructures.persistent.PersistentStore;
import de.webis.parser.WikipediaSQLStreamReader;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.StringValue;

import java.text.Normalizer;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DataStoreFactory {
    private static final String WIKI_PAGES_DUMP = "/media/storage1/corpora/corpus-wikipedia/enwiki-20200220-page.sql";
    private static final String WIKI_PAGE_PROPS_DUMP = "/media/storage1/corpora/corpus-wikipedia/enwiki-20200220-page_props.sql";
    private static final String WIKI_REDIRECTS_DUMP = "/media/storage1/corpora/corpus-wikipedia/enwiki-20200220-redirect.sql";

    public static PersistentStore<String, Set<String>> WIKI_ENTITY_INDEX;

    static {
        initWikiEntityIndex();
    }

    private static void initWikiEntityIndex() {
        WIKI_ENTITY_INDEX = new PersistentStore<>("./data/persistent/wiki-entity-index");

        if (WIKI_ENTITY_INDEX.isEmpty()) {

            Map<Long, Set<String>> redirectTitles = new HashMap<>();
            Set<Long> disambiguationIds = new HashSet<>();

            WikipediaSQLStreamReader pagePropsReader = new WikipediaSQLStreamReader(WIKI_PAGE_PROPS_DUMP);

            System.out.println("Collect disambiguations...");
            while (pagePropsReader.hasNext()) {
                List<List<String>> data = pagePropsReader.next();

                for (List<String> values : data) {
                    String property = values.get(1);

                    if (!property.equals("disambiguation")) {
                        continue;
                    }


                    System.out.print("\rFound disambiguations: " + disambiguationIds.size());

                    long id = Long.parseLong(values.get(0));

                    disambiguationIds.add(id);
                }
            }

            pagePropsReader.close();

            List<String> searchTerms = new ArrayList<>();
            WikipediaSQLStreamReader pagesReader = new WikipediaSQLStreamReader(WIKI_PAGES_DUMP);

            int pageCounter = 0;
            System.out.println("\nIndex pages...");
            while (pagesReader.hasNext()) {
                List<List<String>> data = pagesReader.next();

                for (List<String> values : data) {
                    if (pageCounter % 10000 == 0) {
                        System.out.print("\rIndexed pages: " + pageCounter);
                    }
                    pageCounter++;

                    String title = values.get(WikiPage.TITLE_IDX);
                    String namespace = values.get(WikiPage.NAMESPACE_IDX);
                    boolean isRedirect = "1".equals(values.get(WikiPage.REDIRECT_IDX));
                    long id = Long.parseLong(values.get(WikiPage.ID_IDX));

                    if (!namespace.equals("0")) {
                        continue;
                    }

                    if (isRedirect) {
                        redirectTitles.put(id, WikiPage.normalizeTitle(title));
                        continue;
                    }

                    if (disambiguationIds.contains(id)) {
                        continue;
                    }

                    if (WikiPage.DISAMBIGUATION_PATTERN.matcher(title).matches()) {
                        disambiguationIds.add(id);
                        continue;
                    }

                    searchTerms.addAll(WikiPage.normalizeTitle(title));
                    String entity = WikiPage.PREFIX + title;

                    for (String term : searchTerms) {
                        if (WIKI_ENTITY_INDEX.contains(term)) {
                            Set<String> entities = WIKI_ENTITY_INDEX.get(term);
                            entities.add(entity);

                            WIKI_ENTITY_INDEX.put(term, entities);
                        } else {
                            WIKI_ENTITY_INDEX.put(term, new HashSet<>(Collections.singletonList(entity)));
                        }
                    }


                    searchTerms.clear();
                }
            }

            pagesReader.close();

            WikipediaSQLStreamReader redirectsReader = new WikipediaSQLStreamReader(WIKI_REDIRECTS_DUMP);

            while (redirectsReader.hasNext()) {
                List<List<String>> data = redirectsReader.next();

                for (List<String> values : data) {
                    long id = Long.parseLong(values.get(0));

                    if (redirectTitles.containsKey(id)) {
                        String entity = WikiPage.PREFIX + values.get(2);

                        for (String title : redirectTitles.get(id)) {
                            if (WIKI_ENTITY_INDEX.contains(title)) {
                                Set<String> entities = WIKI_ENTITY_INDEX.get(title);
                                entities.add(entity);

                                WIKI_ENTITY_INDEX.put(title, entities);
                            } else {
                                WIKI_ENTITY_INDEX.put(title, new HashSet<>(Collections.singletonList(entity)));
                            }
                        }
                    }
                }
            }
        }
    }

    public static void loadAll() {
        try {
            Class.forName(DataStoreFactory.class.getName());
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static void closeAll() {
        WIKI_ENTITY_INDEX.close();
    }

    public static void main(String[] args) {
        DataStoreFactory.loadAll();

        Set<String> entities = DataStoreFactory.WIKI_ENTITY_INDEX.get("barack");
        System.out.println(entities);

        DataStoreFactory.closeAll();
    }
}

class WikiPage {
    public static final int ID_IDX = 0;
    public static final int NAMESPACE_IDX = 1;
    public static final int TITLE_IDX = 2;
    public static final int REDIRECT_IDX = 4;

    public static final String PREFIX = "https://en.wikipedia.org/wiki/";

    public static final Pattern UNDERSCORE_PATTERN = Pattern.compile("_");
    public static final Pattern COMMA_PATTERN = Pattern.compile(", ");
    public static final Pattern PARENTHESES_PATTERN = Pattern.compile("\\((.)+\\)");
    public static final Pattern ACCENT_PATTERN = Pattern.compile("\\p{M}");
    public static final Pattern SPECIAL_CHAR_PATTERN = Pattern.compile("[^a-z0-9\\s]");
    public static final Pattern MULTIPLE_WHITESPACES_PATTERN = Pattern.compile("\\s\\s*");
    public static final Pattern DISAMBIGUATION_PATTERN = Pattern.compile(".*\\(disambiguation\\)$");

    public static Set<String> normalizeTitle(String title) {
        Set<String> normalizedTitles = new LinkedHashSet<>();

        title = UNDERSCORE_PATTERN.matcher(title).replaceAll(" ");
        title = title.toLowerCase().trim();

        normalizedTitles.add(title);

        title = Normalizer.normalize(title, Normalizer.Form.NFD);
        title = ACCENT_PATTERN.matcher(title).replaceAll("");

        normalizedTitles.add(title);

        Matcher parenthesesMatcher = PARENTHESES_PATTERN.matcher(title);
        if (parenthesesMatcher.find()) {
            title = parenthesesMatcher.replaceAll("").trim();
            title = MULTIPLE_WHITESPACES_PATTERN.matcher(title).replaceAll(" ");

            normalizedTitles.add(title);
        }

        Matcher commaMatcher = COMMA_PATTERN.matcher(title);
        if (commaMatcher.find()) {
            normalizedTitles.add(COMMA_PATTERN.split(title)[0]);
        }

        Matcher specialCharMatcher = SPECIAL_CHAR_PATTERN.matcher(title);
        if (specialCharMatcher.find()) {
            title = specialCharMatcher.replaceAll("").trim();
            normalizedTitles.add(title);
        }

        return normalizedTitles;
    }
}
