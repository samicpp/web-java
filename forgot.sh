PROJECT_DIR=~/dev-stuff/web-java

# compile external class
javac -cp $PROJECT_DIR/http/build/classes/kotlin/main Program.java
javac -cp $PROJECT_DIR/http/build/classes/kotlin/main:$PROJECT_DIR/web/build/classes/kotlin/main Index.java
