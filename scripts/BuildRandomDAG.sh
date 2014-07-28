#! /bin/bash

NUM_MODES=3
NUM_NODES=1000
NUM_LINKS=4000
OUTFILE=${NUM_NODES}-${NUM_LINKS}.sif
MAIN_CLASS=org.systemsbiology.biofabric.layoutTools.GenRandNModeDAG
JAR_FILE=BioFabricModalDAGLayout.jar

java -cp ${JAR_FILE} ${MAIN_CLASS} ${NUM_MODES} ${NUM_NODES} ${NUM_LINKS} ${OUTFILE}