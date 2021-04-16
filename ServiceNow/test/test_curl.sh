PASS="uri=%2Fsys_user.do%3Fsys_id%3D917"
curl -v -H "Accept: application/json" -H "Content-Type: application/json" -X POST --data "{ 
    "records":
    [ {
        \"source\" : \"nram-testing\",
        \"node\" : \"solace-test\",
        \"type\" : \"High Virtual Memory\",
        \"resource\" : \"C:\",
        \"severity\" : \"5\",
        \"description\" : \"Virtual memory usage exceeds 98%\",
        \"ci_type\":\"cmdb_ci_app_server_tomcat\",
       \"additional_info\":\"{\\\"name\\\":\\\"Ramesh Natarajan\\\"}\"
      }
   ]
}" -u test_int_1:$PASS https://tmxdevelopment.service-now.com/api/global/em/jsonv2
