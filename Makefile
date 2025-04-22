node_modules:
	npm install

extension: node_modules
	npx shadow-cljs release browser-extension
	cp resources/public/dataspex/inspector.css browser-extension/inspector.css

.PHONY: extension
