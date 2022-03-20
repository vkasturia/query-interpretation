#!/bin/bash

mvn clean install

LD_LIBRARY_PATH=lib/de/trenkmann/multimap.io/0.5.0/ \
	mvn exec:java -Dexec.mainClass=de.webis.recognizer.EntityRecognizer
