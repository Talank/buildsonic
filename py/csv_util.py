import csv
from getPR import csv_dir
import os

def parseCSV(fileList):
    result = []
    for fileName in fileList:
        csvPath = os.path.join(csv_dir, fileName.replace('txt', 'csv'))
        with open(csvPath, "r") as file:
            f_csv = csv.reader(file)
            for row in f_csv:
               if row[2] == 'True':
                   print(row[0])
                   result.append(row[0])
    result = list(set(result))
    print(len(result))

if __name__ == '__main__':
    fileList = ["travis_cache.txt", "travis_shallow_clone.txt", "travis_retry.txt", "travis_wait.txt", "travis_fast_finish.txt"]
    parseCSV(fileList)