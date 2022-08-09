.ONESHELL:
.PHONY: FORCE
all: testCompile

TINYB = /usr/local/lib/java/tinyb.jar
RUN_COMMAND = /usr/lib/jvm/java-11-openjdk-amd64/bin/java -Djava.library.path=/usr/local/lib/

compile: $(shell find src/main -name "*.java" -type f)
	rm -rf target/main
	javac -d target/main -Xlint:all -cp $(TINYB) $^

run: compile FORCE
	$(RUN_COMMAND) -cp $(TINYB):target/main yihuchess/Main

testCompile: $(shell find src -name "*.java" -type f)
	rm -rf target/test
	javac -d target/test -Xlint:all -cp lib/*:$(TINYB) $^

test: testCompile FORCE
	$(RUN_COMMAND) -cp lib/*:$(TINYB):target/test yihuchess/Tests

record: testCompile FORCE
	$(RUN_COMMAND) -cp lib/*:$(TINYB):target/test yihuchess/Record

clean: FORCE
	rm -rf target

watch: FORCE
	git ls-files | entr make
