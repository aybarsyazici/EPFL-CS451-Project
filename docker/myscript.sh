# find all files in this directory ending with .output
# and remove last 10 lines from them
for file in *.output
do
    tail -n +40 $file > $file.tmp
    mv $file.tmp $file
done


