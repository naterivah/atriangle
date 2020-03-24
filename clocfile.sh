#!/usr/bin/env bash
set -e # fail script on error

rm -f cloc.*
mvn clean
cloc  common core microservices --out=cloc.out
