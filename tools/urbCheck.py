seenRef = set()

with open('./out/proc09.output') as f:
    count = 0
    for line in f:
        line_lower = line.lower()
        if line_lower in seenRef:
            print(line)
        else:
            seenRef.add(line_lower)
            count += 1

for i in range(1,20):
    if i == 6 or i==13 or i==14: continue
    stri = str(i)
    if(i< 10):
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
                
