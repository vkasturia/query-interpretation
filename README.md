# Query Interpretations from Entity-Linked Segmentations

This repository contains the preliminary Code for Query Interpretations from Entity-Linked Segmentations Paper (WSDM'22). A better version of the  code will be made available in due time [here](https://github.com/webis-de/wsdm22-query-interpretations-from-entity-linked-segmentations).

Web search queries can be ambiguous: is *source of the nile* meant to find information on the actual river or on a board game of that
name? We tackle this problem by deriving entity-based query interpretations: given some query, the task is to derive all reasonable
ways of linking suitable parts of the query to semantically compatible entities in a background knowledge base. Interpreting ambiguous search engine queries can be used to show more relevant results to the user, answer the query or help fill search engine's knowledge boxes. 

In the example, the query *tom cruise ship scene* could have the following interpretations: 

* **Tom Cruise | ship scene** : Scene of a movie of the Hollywood actor Tom Cruise on a ship (most likely)     
* **tom | Cruise ship | scene** : Scene of some Tom on a Cruise ship   
* **tom | Cruise ship | Scene (UK TV Series)** : Some Tom on a Cruise ship in the British TV Series Scene (least likely)


  


