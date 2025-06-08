VERSION := $(shell sed 's/xmlns="[^"]*"//g' pom.xml | xmllint --xpath 'string(/project/version)' -)

node_modules:
	npm install

browser-extension/extension.js: node_modules shadow-cljs.edn $(wildcard src/dataspex/*.clj*)
	npx shadow-cljs release browser-extension

browser-extension/chrome/manifest.json: pom.xml
	sed 's/VERSION/$(VERSION)/' browser-extension/chrome/manifest.tpl.json > browser-extension/chrome/manifest.json

chrome-extension: browser-extension/extension.js browser-extension/chrome/manifest.json
	cp resources/public/dataspex/inspector.css browser-extension/chrome/inspector.css
	cp browser-extension/extension.js browser-extension/chrome/extension.js
	cp browser-extension/content-script.js browser-extension/chrome/content-script.js
	cp browser-extension/devtools.html browser-extension/chrome/devtools.html
	cp browser-extension/panel.html browser-extension/chrome/panel.html

browser-extension/firefox/manifest.json:
	sed 's/VERSION/$(VERSION)/' browser-extension/firefox/manifest.tpl.json > browser-extension/firefox/manifest.json

firefox-extension: browser-extension/extension.js
	cp resources/public/dataspex/inspector.css browser-extension/firefox/inspector.css
	cp browser-extension/extension.js browser-extension/firefox/extension.js
	cp browser-extension/content-script.js browser-extension/firefox/content-script.js
	cp browser-extension/devtools.html browser-extension/firefox/devtools.html
	cp browser-extension/panel.html browser-extension/firefox/panel.html

resources/public/dataspex/inspector.js:
	npx shadow-cljs release remote-inspector

clean:
	rm -fr target resources/public/app dev-resources/public/dataspex resources/public/dataspex/cljs-runtime resources/public/dataspex/inspector.js resources/public/dataspex/manifest.edn browser-extension/manifest.edn browser-extension/inspector.css browser-extension/extension.js dev-resources/public/portfolio dataspex.jar

dataspex.jar: resources/public/dataspex/inspector.js
	clojure -M:jar

deploy: dataspex.jar
	env CLOJARS_USERNAME=$$(xmllint --xpath "string(/settings/servers/server[id='clojars']/username)" ~/.m2/settings.xml) \
	    CLOJARS_PASSWORD=$$(xmllint --xpath "string(/settings/servers/server[id='clojars']/password)" ~/.m2/settings.xml) \
	    clojure -X:deploy

.PHONY: extension clean deploy
