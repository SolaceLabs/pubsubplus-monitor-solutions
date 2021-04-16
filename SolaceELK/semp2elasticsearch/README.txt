Basic JAVA process to query SEMP statistics and post them to an ElasticSearch server for indexing.

All ElasticSearch interactions can be done via HTTP REST Operations: HEAD, GET, PUT, POST.

XML data is queried from Solace via HTTP POST operations, the results are converted to JSON, then uploaded to ElasticSearch via POST operations.

Existence of indexes are tested via HTTP HEAD operations, and if they do not exist they are created via HTTP PUT operations.
