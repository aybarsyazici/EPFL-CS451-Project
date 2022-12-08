proc_count = 50
for i in range(1, proc_count+1):
    stri = str(i)
    if (i < 10):
        stri = "0" + stri
    with open('./out/proc'+stri+'.output') as f:
        reading_line_count = 0
        for line in f:
            # Check for every number in this line
            # If the number is present in the any of the config files at the same line
            numbersToCheck = line.split()
            configNumberArray = []
            for configChecker in range(1, proc_count+1):
                strConfigChecker = str(configChecker)
                if (configChecker < 10):
                    strConfigChecker = "0" + str(configChecker)
                config_i = './out/proc'+strConfigChecker+'.config'
                with open(config_i) as f2:
                    lineCount = -1
                    for line2 in f2:
                        if lineCount == reading_line_count:
                            # line2 contains numbers seperated by spaces
                            # add them all into an array
                            numbers2 = line2.split()
                            # convert the numbers from string to int
                            numbers2 = [int(i) for i in numbers2]
                            configNumberArray.append(numbers2)
                            break
                        lineCount += 1
            for number in numbersToCheck:
                number = int(number)
                found = False
                for configNumbers in configNumberArray:
                    if number in configNumbers:
                        found = True
                        break
                if not found:
                    print("Number " + str(number) + " in file proc" + stri + ".output at line " +
                          str(reading_line_count+1) + " is not present in any config file")
                    exit()
            self_config = configNumberArray[i-1]
            for config_number in self_config:
                if str(config_number) not in numbersToCheck:
                    print("Number " + str(config_number) + " in file proc" + stri + ".config at line " +
                          str(reading_line_count+1) + " is not present in output file")
                    exit()
            reading_line_count += 1

print("All numbers in output files are present in config files")
