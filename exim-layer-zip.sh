#!/bin/sh

mvn dependency:copy-dependencies -DoutputDirectory=Exim-Code/java/lib
cd Exim-Code
zip -r Exim-Layer.zip java/
rm -r java
open .
