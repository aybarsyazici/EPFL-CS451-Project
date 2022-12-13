import argparse

parser = argparse.ArgumentParser(
    prog='Lattice Agreement Check',
    description='Iterates over all the output files to check for the consistency property of the algorithm'
)

parser.add_argument('proc_count', type=int, help='Number of processes')
args = parser.parse_args()

proc_count = args.proc_count
crashed_procs = [30, 36, 19, 32, 23, 1, 25, 17, 8, 28,
                 40, 16, 10, 2, 18, 21, 44, 5, 38, 12, 42, 3, 31, 7]

for i in range(1, proc_count+1):
    stri = str(i)
    if (i < 10):
        stri = "0" + stri
    allLines = []
    okay = True
    try:
        with open('./out/proc'+stri+'.output') as f:
            reading_line_count = 0
            for line in f:
                # line contains numbers seperated by spaces
                # add them all into an array
                numbers = line.split()
                # convert the numbers from string to int
                numbers = [int(i) for i in numbers]
                allLines.append(numbers)
                reading_line_count += 1
        #  Now check all these lines with other output files
        try:
            for j in range(i, proc_count+1):
                if i == j:
                    continue
                strj = str(j)
                if (j < 10):
                    strj = "0" + strj
                with open('./out/proc'+strj+'.output') as f:
                    lineCount = 0
                    for line in f:
                        # line contains numbers seperated by spaces
                        # add them all into an array
                        numbers = line.split()
                        # convert the numbers from string to int
                        numbers = [int(i) for i in numbers]
                        # Check if numbers is a subset of allLines[lineCount]
                        # or if allLines[lineCount] is a subset of numbers
                        if not (set(numbers).issubset(allLines[lineCount]) or set(allLines[lineCount]).issubset(numbers)):
                            print("Line " + str(lineCount) + " in file " + stri +
                                  " is not a subset of line " + str(lineCount) + " in file " + strj)
                            okay = False
                            exit()

                        lineCount += 1
        except:
            print()
    except:
        print("File " + stri + " has not been found, has it crashed?")
    print("File " + stri + " is " + ("a-okay! :)" if okay else "not okay :(") + " lineCount: " +
          str(len(allLines)) + " " + ("" if i not in crashed_procs else " (crashed)"))

print("All a-okay! :)")
