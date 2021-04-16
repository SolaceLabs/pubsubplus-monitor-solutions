user="test_int_1"
pass="uri=%2Fsys_user.do%3Fsys_id%3D917"
#url="https://tmxdevelopment.service-now.com/api/now/table/em_event"
url="https://tmxdevelopment.service-now.com/api/global/em/jsonv2"
file=$1
[ $# -lt 1 ] && { echo $0 json-file; exit; }
[ ! -f $file ] && { echo file $file not found; exit; }
echo "---
File $file
"
cat $file
echo "
---
Posting file $file to $url ...
"
curl -v -H "Accept: application/json" \
 -H "Content-Type: application/json" \
 -X POST -d "@${file}" -u $user:$pass "$url"
