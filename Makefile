node_modules:
	npm install

extension: node_modules
	npx shadow-cljs release browser-extension
	cp resources/public/dataspex/inspector.css browser-extension/inspector.css

resources/public/dataspex/inspector.js:
	npx shadow-cljs release remote-inspector

clean:
	rm -fr target resources/public/app dev-resources/public/dataspex resources/public/dataspex/cljs-runtime resources/public/dataspex/inspector.js resources/public/dataspex/manifest.edn browser-extension/manifest.edn browser-extension/inspector.css browser-extension/extension.js dev-resources/public/portfolio

dataspex.jar: resources/public/dataspex/inspector.js
	clojure -M:jar

deploy: dataspex.jar
	clojure -X:deploy

.PHONY: extension clean deploy
