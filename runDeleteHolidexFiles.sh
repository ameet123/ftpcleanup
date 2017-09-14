#!/bin/bash

TYPE="INT-STG"
#RECIPIENTS="ameet.chaubal@ihg.com"
RECIPIENTS="Abhijith.Basavaraj@IHG.com neil.ashmore@ihg.com Mohamed.Mohsin@ihg.com ameet.chaubal@ihg.com"
SUBJECT="ftp-cleanup[${TYPE}]:`date +'%Y-%m-%d'`"
LOG=/home/synap/log/clean.log

OUT="->[`date '+%Y-%m-%d'`] Clean up ftp files from 10.211.133.25: INT & STG"
OUT="${OUT}\n---------------------------------------------------------------------------------\n"
OUT="${OUT}\n\n$(ssh sf1ap@iadd1slsf1ap001.ihgint.global sh /EdwData/HAPI/Inventory/int_cleanup.sh)"
OUT="${OUT%x}"
OUT="${OUT}\n\n-----------------------------------------------------------------------\n"

OUT="${OUT}\n\n$(ssh sf1ap@iadd1slsf1ap001.ihgint.global sh /EdwData/HAPI/Inventory/stg_cleanup.sh)"
OUT="${OUT%x}"
OUT="${OUT}\n\n---------------------------------------------------------------------------------\n"


echo -e "$OUT"| mailx -s $SUBJECT $RECIPIENTS

echo -e "$OUT" >> $LOG