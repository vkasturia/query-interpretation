#!/bin/bash -e

if [ ! -e resources/classifiers/english.all.3class.distsim.crf.ser.gz ]
then
	echo "Download: resources/classifiers/english.all.3class.distsim.crf.ser.gz"
	rm -Rf resources
	mkdir -p resources/classifiers/
	wget 'https://github.com/sosolimited/recon_backend/raw/master/named-entity/classifiers/english.all.3class.distsim.crf.ser.gz' -P resources/classifiers/
	echo "Done: Downloaded resources/classifiers/english.all.3class.distsim.crf.ser.gz"
fi

mvn clean install

LD_LIBRARY_PATH=lib/de/trenkmann/multimap.io/0.5.0/ \
	mvn exec:java -Dexec.mainClass=de.webis.processer.NamedEntityExtractor
