.PHONY: build deploy run format frontend-build frontend-format

VERSION := 0.1.0
JAR := target/skygit-$(VERSION).jar
TARGET := $(HOME)/jars/skygit.jar

build: frontend-build
	sbt assembly

deploy: build
	mkdir -p "$(dir $(TARGET))"
	mv "$(JAR)" "$(TARGET)"
	chmod +x "$(TARGET)"

run: frontend-build
	sbt "run server"

format: frontend-format
	sbt scalafmt

frontend-install:
	cd frontend && npm install

frontend-build: frontend-install
	cd frontend && npm run build

frontend-format:
	cd frontend && npm run format