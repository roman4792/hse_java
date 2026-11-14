#!/bin/sh
# Компиляция проекта в папку out
mkdir -p out
javac -encoding UTF-8 -d out src/pizzadb/*.java
