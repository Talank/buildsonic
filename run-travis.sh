#!/bin/bash

# bash run-travis.sh /home/jhilke/researh/buildsonic /home/jhilke/researh/ut-se/optimizing-ci-builds/data/repos /home/jhilke/researh/buildsonic/result.csv

# arg1: Path to the Gradle executable
# arg2: Path to the root directory of the repositories where all the repositories to inspect are located
# arg3: Path to the output CSV file

GRADLE_PATH="$1"
REPOS_PATH="$2"
OUTPUT_PATH="$3"

cd $REPOS_PATH

for dir in */; do
  echo "Processing $dir"
    cd $GRADLE_PATH
    gradle -q -b build.gradle PullRequestTravisCreator --args="$REPOS_PATH/$dir"

  if [ $? -ne 0 ]; then
    echo "Gradle command failed"
    echo "$dir,Failed" >> $OUTPUT_PATH
    continue
  fi
  
  cd $REPOS_PATH
  cd $dir

  if [[ $(git diff --exit-code) ]]; then
    echo "Git diff has content"
    output=$(echo "$(git diff)" | sed 's/"/""/g')
    echo "\"$dir\",\"yes\",\"$output\"" >> $OUTPUT_PATH
    git stash clear
  else
    echo "Git diff is empty"
    echo "$dir,No," >> $OUTPUT_PATH
    git stash clear
  fi

  cd $REPOS_PATH
done

