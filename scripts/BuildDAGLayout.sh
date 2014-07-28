#! /bin/bash

NUM_NODES=1000
NUM_LINKS=4000
MAIN_CLASS=org.systemsbiology.biofabric.layoutTools.MultiModeDagLayout
JAR_FILE=BioFabricModalDAGLayout.jar

FILE_ROOT=${NUM_NODES}-${NUM_LINKS}
SIF_IN_FILE=${FILE_ROOT}.sif
NOA_OUT_FILE=${FILE_ROOT}.noa

java -cp ${JAR_FILE} ${MAIN_CLASS} ${SIF_IN_FILE} ${NOA_OUT_FILE}