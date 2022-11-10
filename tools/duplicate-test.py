for i in range(1,21):
    stri = str(i)
    if(i< 10):
        stri = "0" + stri
    with open('./out/proc'+stri+'.output') as f:
        seen = set()
        count = 0
        for line in f:
            line_lower = line.lower()
            if line_lower in seen:
                print(line)
            else:
                seen.add(line_lower)
                count += 1
    print('Unique line count: ' + str(count))
