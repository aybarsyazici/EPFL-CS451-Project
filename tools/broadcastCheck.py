proc_count = 15

for i in range(1, proc_count+1):
    stri = str(i)
    if (i < 10):
        stri = "0" + stri
    with open('./out/proc'+stri+'.output') as f:
        seen = set()
        for line in f:
            line_lower = line.lower()
            if line[0:1] == "b":
                seen.add(int(line_lower[2:]))
            elif line.split(" ")[1] == i:
                deliver_num = int(line.split(" ")[2])
                if deliver_num not in seen:
                    print("Error: process " + stri +
                          " did not broadcast " + deliver_num)
                    exit(1)

print("All a-okay! :)")
