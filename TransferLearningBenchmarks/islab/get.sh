 #!/bin/bash
folder='/home/mofeed/Projects/Transfer_Learning/TransferLearningBenchmarks/islab'
units=0
tens=0
hundreds=0
while [  $tens -ne 4 ];
do
    units=$((units+1))
    fileURL= 'http://islab.di.unimi.it/iimb/0'$tens$units'/abox.owl'    #"http://ontologi.es/rail/stations/gb/"$name".nt"
    echo "$fileURL"
 #    wget $fileURL  -O "0"$tens$units
   if [ $units -eq 9 ]
	then
         units=0
 	 tens=$((tens+1))
   fi
done
