#!/bin/bash

export CLOJARS_USERNAME=$(xmllint --xpath 'string(//server[id="clojars"]/username)' ~/.m2/settings.xml)
export CLOJARS_PASSWORD=$(xmllint --xpath 'string(//server[id="clojars"]/password)' ~/.m2/settings.xml)

make clean deploy
