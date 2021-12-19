.ONESHELL:
.PHONY: FORCE
all: compile

compile: $(shell find . -name "*.java" -type f)
	- javac -cp /usr/local/lib/java/tinyb.jar $^

run: compile FORCE
	- java -Djava.library.path=/usr/local/lib/ -cp /usr/local/lib/java/tinyb.jar:. HelloTinyB

clean: FORCE
	- find . -name "*.class" -type f -delete

watch: FORCE
	- git ls-files | entr make
