# Copyright 2022 Vaibhav Kasturia <vaibhav.kasturia@informatik.uni-halle.de>
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

"""
@author: Vaibhav Kasturia (vaibhav.kasturia@informatik.uni-halle.de)
"""
import random

import streamlit as st
import wikipedia
from urllib.parse import unquote

from PIL import Image

import requests
import json
import os

from streamlit_lottie import st_lottie_spinner

COLOR = "Black"
BACKGROUND_COLOR = "#FFFFFF"
hide_menu = """
<style>
#MainMenu {
    visibility: hidden;
}

footer {
    visibility: hidden;
}
footer:after {
    content: '\\00a9 Kasturia/Gohsen/Hagen 2022';
    visibility: visible;
    display: block;
    position: relative;
    color: black;
    padding: 5px;
    top: 2px;
}
</style>
"""


# Main
def main():
    st.set_page_config(layout="wide")

    st.title("Query Interpretations from Entity-Linked Segmentations")
    st.markdown(hide_menu, unsafe_allow_html=True)
    ### Sidebar
    st.sidebar.title("About")
    st.sidebar.text('')
    st.sidebar.markdown('<div style="text-align: justify;">Given some query, the task is to derive all reasonable '\
                        'ways of linking suitable parts of the query to semantically compatible entities in a '\
                        'background knowledge base.</div>', unsafe_allow_html=True)
    st.sidebar.text('')
    st.sidebar.markdown('<div style="text-align: justify;">Our suggested approach focuses on effectiveness but'\
                        ' also on efficiency since web search response times should not exceed some hundreds'\
                        ' of milliseconds. In our approach, we use query segmentation as a preprocessing step'\
                        ' that finds promising segment-based “interpretation skeletons”. The individual segments'
                        ' from these skeletons are then linked to entities from a knowledge base and the reasonable'\
                        ' combinations are ranked in a final step.</div>', unsafe_allow_html=True)
    st.sidebar.text('')
    st.sidebar.markdown('<div style="text-align: justify;">More information can be found in the following'\
                        ' publication:</div>', unsafe_allow_html=True)
    st.sidebar.text('')
    st.sidebar.markdown('> Vaibhav Kasturia, Marcel Gohsen, and Matthias Hagen. *Query Interpretations from Entity-Linked'\
                        ' Segmentations.* The 15th ACM International Conference on Web Search and Data Mining (WSDM’22).'
                        ' Phoenix (Arizona, USA). 2022')
    st.sidebar.markdown('#')

    img1 = Image.open("images/mlu-logo.png")
    img2 = Image.open("images/buw-logo.jpeg")
    img3 = Image.open("images/webis-logo.png")
    layout = st.sidebar.columns([1, 1, 1])
    with layout[0]:
        st.image(img1, width=80)
    with layout[-2]:
        st.image(img2, width=70)
    with layout[-1]:
        st.image(img3, width=40)

    lottie_url_loading = "https://assets8.lottiefiles.com/packages/lf20_w6xlywkv.json"
    lottie_loading = load_lottieurl(lottie_url_loading)

    not_found_img = Image.open("images/404.png")

    col1, col2 = st.columns([4, 1])
    with col1:
        query = st.text_input('', 'tom cruise ship scene')

    with col2:
        st.markdown('#')
        go_button = st.button('Get Interpretations')

    if go_button:
        os.environ['NO_PROXY'] = '127.0.0.1'
        base_url = "http://127.0.0.1:4567/query"
        linker = "WEBIS"
        payload = f"text={query.replace(' ', '+')}&linker={linker}"
        payload = f"text={query.replace(' ', '+')}&linker={linker}"
        input_query = f"{base_url}?{payload}"

        #query_result = requests.get(input_query)
        #data = query_result.json()
        
        #For testing purposes
        query_result = '[ { "id": 0, "interpretation": [ "https://en.wikipedia.org/wiki/Tom_Cruise", "ship scene" ], "relevance": 1.1701927945243493, "containedEntities": [ "https://en.wikipedia.org/wiki/Tom_Cruise" ], "contextWords": [ "ship", "scene" ] }, { "id": 0, "interpretation": [ "tom", "https://en.wikipedia.org/wiki/Cruise_ship", "scene" ], "relevance": 1.1673680362987984, "containedEntities": [ "https://en.wikipedia.org/wiki/Cruise_ship" ], "contextWords": [ "tom", "scene" ] }, { "id": 0, "interpretation": [ "tom", "https://en.wikipedia.org/wiki/Cruise_ship", "https://en.wikipedia.org/wiki/Scene_(UK_TV_series)" ], "relevance": 0.8569681445704831, "containedEntities": [ "https://en.wikipedia.org/wiki/Cruise_ship", "https://en.wikipedia.org/wiki/Scene_(UK_TV_series)" ], "contextWords": [ "tom" ] }, { "id": 0, "interpretation": [ "tom", "cruise ship", "https://en.wikipedia.org/wiki/Scene_(UK_TV_series)" ], "relevance": 0.23045130946024894, "containedEntities": [ "https://en.wikipedia.org/wiki/Scene_(UK_TV_series)" ], "contextWords": [ "tom", "cruise", "ship" ] } ]'
        data = json.loads(query_result)
        
        interpretation_score_dict = {}

        sum_relevance = 0
        sum_element_count = 0
        key_count = 0

        for query_interpretation in data:
            sum_element_count += len(query_interpretation['interpretation'])
            relevance = query_interpretation['relevance']
            sum_relevance += relevance

        for query_interpretation in data:
            interpretation = query_interpretation['interpretation']
            score = query_interpretation['relevance'] / sum_relevance
            interpretation_score_dict[tuple(interpretation)] = score

        element_count = 0
        interpretation_count = 1

        for query_interpretation in interpretation_score_dict:
            element_count += len(query_interpretation)
            keys = random.sample(range(1000, 9999), sum_element_count)

            score = interpretation_score_dict[query_interpretation]*100

            st.markdown('#')
            st.markdown('### Interpretation ' + str(interpretation_count) + ' (Probability: ' + str(round(score, 2)) + '%)')

            ncol = len(query_interpretation)
            wcol = 6
            cols = st.columns(ncol)
            animation_keys = random.sample(range(10000, 11000), len(interpretation_score_dict))
            animation_key_count = 0
            for i in range(ncol):
                # col = cols[i]
                col = cols[i % wcol]
                _, subcol2, _ = st.columns([1, 1, 1])
                with subcol2:
                    with st_lottie_spinner(lottie_loading, key=animation_keys[animation_key_count], height=200, width=200):
                        animation_key_count += 1
                        if query_interpretation[i].startswith("https"):
                            entity_url = query_interpretation[i]
                            entity_title = unquote(entity_url).split('/')[-1]
                            entity_title = entity_title.replace("_", " ")
                            try:
                                entity_data = get_entity_info(article=entity_title)
                                col.markdown('##')
                                with col.expander(entity_title):
                                    st.image(entity_data['image'], use_column_width='auto')
                                    st.markdown(entity_data['summary'][:200] + '... [Read more on Wikipedia](' + entity_url + ')')
                            except:
                                with col.expander(entity_title):
                                    st.image(not_found_img)
                                    st.markdown("404 Page not Found")

                        else:
                            concept = query_interpretation[i]
                            col.text_input('', concept, disabled=True, key=keys[key_count])
                            key_count += 1
            element_count = 0
            interpretation_count += 1

def get_entity_info(article):
    entity_data = {}
    page = wikipedia.page(article, auto_suggest=False)
    entity_data['title'] = page.title
    entity_data['url'] = page.url
    if page.images:
        try:
            main_image_url = get_wiki_main_image(article)
        except KeyError:
            main_image_url = page.images[0]
        if main_image_url.lower().endswith('.svg'):
            image_resized = resize_image(page.images[0])
        else:
            image_resized = resize_image(main_image_url)
        entity_data['image'] = image_resized
    else:
        entity_data['image'] = None
    entity_data['summary'] = page.summary
    return entity_data


def resize_image(image_url):
    headers = {
        'User-Agent': 'My User Agent 1.0'
    }
    response = requests.get(image_url, headers=headers, stream=True)
    img = Image.open(response.raw)

    thumb_width = 400
    im_thumb = crop_max_square(img).resize((thumb_width, thumb_width), Image.LANCZOS)
    return im_thumb


def get_wiki_main_image(title):
    url = 'https://en.wikipedia.org/w/api.php'
    data = {
        'action': 'query',
        'format': 'json',
        'formatversion': 2,
        'prop': 'pageimages|pageterms',
        'piprop': 'original',
        'titles': title
    }
    response = requests.get(url, data)
    json_data = json.loads(response.text)
    return json_data['query']['pages'][0]['original']['source'] if len(json_data['query']['pages']) > 0 else 'Not found'


def crop_max_square(pil_img):
    return crop_center(pil_img, min(pil_img.size), min(pil_img.size))


def crop_center(pil_img, crop_width, crop_height):
    img_width, img_height = pil_img.size
    return pil_img.crop(((img_width - crop_width) // 2,
                         (img_height - crop_height) // 2,
                         (img_width + crop_width) // 2,
                         (img_height + crop_height) // 2))


def load_lottieurl(url: str):
    r = requests.get(url)
    if r.status_code != 200:
        return None
    return r.json()

if __name__ == '__main__':
    main()
