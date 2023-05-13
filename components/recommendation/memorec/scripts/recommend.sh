#!/bin/bash

echo "Executing recommend.sh"

#set -o xtrace

SHARED=$1
CLASS_ATTR_FOLDER="$SHARED/memorec_output_cls_attr/"

json=$(cat "$SHARED/X.json")

# Generate a newline-separated list of objects
obj_list=$(echo "${json}" | jq -c '.[]')

echo -e "[\n" > $SHARED/y_pred.json
SEPARATOR=""
# Loop through each object in the list
while IFS= read -r obj; do
    # Extract the values of the ids and xmi_path fields
    ids=$(echo "${obj}" | jq -r '.ids')
    xmi_path=$(echo "${obj}" | jq -r '.xmi_path')
    owner=$(echo "${obj}" | jq -r '.owner')

    XMI_FULL_PATH="$SHARED/$xmi_path"

    echo "Processing $owner - $ids - $XMI_FULL_PATH"
#    echo "Processing $owner - $ids - $XMI_FULL_PATH" >> $SHARED/y_pred.json

    java -cp lib/MemoRecLite-1.0.jar utils.ExtractOne "$XMI_FULL_PATH" Class 0

    # replace the extension of the file $SHARED/$xmi_path with txt
    TXT_PATH=${XMI_FULL_PATH%%.*}.txt
    #TXT_PATH_WITHOUT_CLASS="${XMI_FULL_PATH%%.*}-$owner.txt"

    #grep -v "$owner#" $TXT_PATH > $TXT_PATH_WITHOUT_CLASS
    #echo "$owner#dummy" >> $TXT_PATH_WITHOUT_CLASS

  	java -cp lib/MemoRecLite-1.0.jar memoRecCore.Runner $TXT_PATH $CLASS_ATTR_FOLDER $owner $SHARED/recommendations.txt
    RES="$?"
    echo "Recommendations for this..."
    #cat $SHARED/recommendations.txt
    echo
    echo

    # Check the exit code of the previous command
    if [ $RES -eq 0 ]; then
      # Construct a JSON list with the recommendations with jq
      #recs=$(cat $SHARED/recommendations.txt | jq -R -s -c 'split("\n")[:-1]')
      recs=$(cat $SHARED/recommendations.txt | jq -r '.Results[] | keys | .[]'  | head -n 5 | jq -R -c -s 'split("\n")[:-1]')
      echo "Adding to y_pred.json: $recs"
      echo $SEPARATOR >> $SHARED/y_pred.json
      echo $recs >> $SHARED/y_pred.json
    else
      echo $SEPARATOR >> $SHARED/y_pred.json
      echo "[]" >> $SHARED/y_pred.json
    fi

    SEPARATOR=","
#
#    cat $SHARED/y_pred.json
done <<< "${obj_list}"
echo "Finished processing all objects"
echo -e "\n]\n" >> $SHARED/y_pred.json
