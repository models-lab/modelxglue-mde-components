
SHARED=$1

RELATIVE_XMI_FOLDER=`cat "$SHARED/X_attrs.json" | jq -r '.xmi_folder'`
XMI_FOLDER="$SHARED/$RELATIVE_XMI_FOLDER"

# Because recommend.sh may leave spureous files there
rm $XMI_FOLDER/*.txt

rm -rf "$SHARED/memorec_output_cls_attr/"
rm -rf "$SHARED/memorec_output_pkg_cls/"

java -cp /lib/MemoRecLite-1.0.jar utils.ExtractMany $XMI_FOLDER "$SHARED/memorec_output_" 0
