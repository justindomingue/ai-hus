#!/bin/bash
# Runs a simulation and saves it

# How to use
#   ./run_simulation.sh NAME value1[, value2, value3, ...]
#     where NAME=name of the parameter being tested
#     value1, [value2, value3, ...] is a list of values to test
#
#   Example:
#     $ ./run_simulation.sh C .1 .01
#     >> AUTOPLAY WITH C=.1
#     >> Copying results to simulations/1457186564-C-.1.txt
#     >> AUTOPLAY WITH C=.01
#     >> Copying results to simulations/1457186564-C-.01.txt

# setup the environment
LOGDIR=logs
SIMDIR=simulations
DATE=`date +%s`
NGAMES=50
JAVA_CONSTANTS_FILE="constants.txt"

echo "Setting up the environment (creating simulations and emptying logs)"
mkdir -p simulations
rm -f $LOGDIR/*

# Grab the name of the parameter being tested
NAME=$1
shift

# Iterate over every value provided in
for i
do
  echo "Writing constant value to $JAVA_CONSTANTS_FILE"
  echo "$NAME:$i" > "$JAVA_CONSTANTS_FILE"

  echo "Emptying outcomes.txt"
  cat /dev/null > $LOGDIR/outcomes.txt

  echo "AUTOPLAY WITH $NAME=$i"
  ant autoplay -Dn_games=$NGAMES

  echo "Copying results to $SIMDIR/$DATE-$NAME-$i.txt"
  cp $LOGDIR/outcomes.txt $SIMDIR/$DATE-$NAME-$i.txt
done

