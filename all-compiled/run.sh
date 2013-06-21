#!/bin/bash
java -Xmx512m -Dsun.java2d.noddraw=true -classpath classes:lib/gdal.jar:lib/gluegen-rt.jar:lib/jogl-all.jar si.xlab.gaea.core.examples.GaeaApplicationExample
