.ONESHELL:
.PHONY: FORCE
all: compile

compile: $(shell find . -name "*.java" -type f)
	- javac -cp /usr/local/lib/java/tinyb.jar $^

run: compile FORCE
	- /usr/lib/jvm/java-11-openjdk-amd64/bin/java -Djava.library.path=/usr/local/lib/ -cp /usr/local/lib/java/tinyb.jar:. yihuchess/Main

clean: FORCE
	- find . -name "*.class" -type f -delete

watch: FORCE
	- git ls-files | entr make
