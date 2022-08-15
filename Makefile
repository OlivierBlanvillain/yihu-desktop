.ONESHELL:
.PHONY: FORCE
all: testCompile

TINYB = /usr/local/lib/java/tinyb.jar
RUN_COMMAND = /usr/lib/jvm/java-11-openjdk-amd64/bin/java -Djava.library.path=/usr/local/lib/
RUN_CLASSPATH = -cp $(TINYB):target/main
TEST_CLASSPATH = -cp $(TINYB):lib/*:target/test
COVERAGE_AGENT = -javaagent:lib/jacocoagent.jar=append=false,destfile=target/coverage.exec

compile: $(shell find src/main -name "*.java" -type f)
	rm -rf target/main
	javac -d target/main -Xlint:all -cp $(TINYB) $^

run: compile FORCE
	$(RUN_COMMAND) $(RUN_CLASSPATH) yihuchess/Main

testCompile: $(shell find src -name "*.java" -type f)
	rm -rf target/test
	javac -d target/test -Xlint:all -cp lib/*:$(TINYB) $^

test: testCompile FORCE
	$(RUN_COMMAND) $(TEST_CLASSPATH) yihuchess/Tests

record: testCompile FORCE
	$(RUN_COMMAND) $(TEST_CLASSPATH) yihuchess/Record

coverage: clean testCompile FORCE
	$(RUN_COMMAND) $(TEST_CLASSPATH) $(COVERAGE_AGENT) yihuchess/Tests
	java -jar lib/jacococli.jar report target/coverage.exec --sourcefiles src/main/java --classfiles target/ --html target/coverage/

clean: FORCE
	rm -rf target

watch: FORCE
	git ls-files | entr make
