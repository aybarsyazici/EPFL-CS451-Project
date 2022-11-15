
crashed_procs = [13, 2, 10, 17, 18, 19, 20, 4, 9]

proc_count = 20

correct = True
non_crashed_procs = [i for i in range(
    1, proc_count+1) if i not in crashed_procs]


for crashed in crashed_procs:

    str_crashed = str(crashed)
    if (crashed < 10):
        str_crashed = "0" + str_crashed

    seenRef = set()

    with open('./out/proc'+str_crashed+'.output') as f:
        count = 0
        for line in f:
            line_lower = line.lower()
            if line_lower in seenRef:
                print(line)
            else:
                seenRef.add(line_lower)
                count += 1

    for i in non_crashed_procs:
        stri = str(i)
        if (i < 10):
            stri = "0" + stri
        with open('./out/proc'+stri+'.output') as f:
            seen = set()
            for line in f:
                line_lower = line.lower()
                if line_lower in seen:
                    print(line)
                else:
                    seen.add(line_lower)

        checkIndex = 4 if i < 10 else 5
        for line in seenRef:
            if line[0:checkIndex] != "d " + str(i) + " " and line not in seen:
                print('In file: ' + stri + ' ' + line + ' not found')
                correct = False

if correct:
    print('UNIFORM delivery correct')

# check if all non crashed processes have the same output

for index in range(1, len(non_crashed_procs)-1):
    i = non_crashed_procs[index]
    stri = str(i)
    seen = set()
    if (i < 10):
        stri = "0" + stri
    with open('./out/proc'+stri+'.output') as f:
        for line in f:
            line_lower = line.lower()
            if line_lower in seen:
                print(line)
            else:
                seen.add(line_lower)

    for index2 in range(i+1, len(non_crashed_procs)):
        j = non_crashed_procs[index2]
        strj = str(j)
        if (j < 10):
            strj = "0" + strj
        with open('./out/proc'+strj+'.output') as f:
            seen2 = set()
            for line in f:
                line_lower = line.lower()
                if line_lower in seen2:
                    print("DUPLICATE IN " + strj + " " + line)
                else:
                    seen2.add(line_lower)

        checkIndex = 4 if i < 10 else 5
        for line in seen:
            if line[0:checkIndex] != "d " + str(i) + " " and line not in seen2:
                print('In file: ' + stri + ' ' + line + ' not found')
                correct = False

if correct:
    print('All correct processes have the same output')
