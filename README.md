# Query Interpretations from Entity-Linked Segmentations

This repository contains the preliminary Code for Query Interpretations from Entity-Linked Segmentations Paper (WSDM'22). A better version of the  code will be made available in due time [here](https://github.com/webis-de/wsdm22-query-interpretations-from-entity-linked-segmentations).

Web search queries can be ambiguous: is *source of the nile* meant to find information on the actual river or on a board game of that
name? We tackle this problem by deriving entity-based query interpretations: given some query, the task is to derive all reasonable
ways of linking suitable parts of the query to semantically compatible entities in a background knowledge base. Interpreting ambiguous search engine queries can be used to show more relevant results to the user, answer the query or help fill search engine's knowledge boxes. 

![Alt Text](https://github.com/vkasturia/query-interpretation/blob/master/frontend/demo/query-interpretation.gif)

In the example, the query *tom cruise ship scene* could have the following interpretations: 

* **Tom Cruise | ship scene** : Scene of a movie of the Hollywood actor Tom Cruise on a ship (most likely)     
* **tom | Cruise ship | scene** : Scene of some Tom on a Cruise ship   
* **tom | Cruise ship | Scene (UK TV Series)** : Some Tom on a Cruise ship in the British TV Series Scene (least likely)

Our suggested approach focuses on effectiveness but also on efficiency since web search response times should not exceed some hundreds of milliseconds. In our approach, we use query segmentation as a preprocessing step that finds promising segment-based “interpretation
skeletons”. The individual segments from these skeletons are then linked to entities from a knowledge base and the reasonable combinations are ranked in a final step. An experimental comparison on a combined corpus of all existing query entity linking datasets (available [here](https://webis.de/data/webis-qinc-22.html)) shows our approach to have a better interpretation accuracy at a better run time than the previously most effective methods.
  
For a quick overview of the paper, you can have a look at the [poster](https://webis.de/downloads/publications/posters/kasturia_2022.pdf) and the [video](https://dl.acm.org/doi/10.1145/3488560.3498532#video_stream_uuid%3Abe35fc1b-e5e4-49a4-8eb2-9985619abf84). More information can be found in the following [publication](https://arxiv.org/pdf/2105.08581.pdf):

> Vaibhav Kasturia, Marcel Gohsen, and Matthias Hagen. *Query Interpretations from Entity-Linked Segmentations.* The 15th ACM International Conference on Web Search and Data Mining (WSDM’22). Phoenix (Arizona, USA). 2022



