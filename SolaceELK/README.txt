This is a barebones HOWTO implementation of Syslog monitoring for Solace appliances based on the following open-source tools:
- ElasticSearch
- Logstash
- Kibana

It consists of instructions for setup plus a set of configuration files and convenience scripts for Solace-specific deployments that can be overlaid on top of an existing installation of the 3 products.

DOWNLOAD SITES:
- http://logstash.net/
- http://www.elasticsearch.org/download
- http://www.elasticsearch.org/overview/kibana/installation/


ASSUMPTIONS:
- You will run these packages on a 64-bit linux server (or VM)
- This server has network access (duh) and does not run a firewall (it must accept connections on ports 514, 9200, and whatever your httpd is configured for)
- You will run these packages on the same server together
- You will run these packages under the root user
- You have a functioning Apache httpd installed
- You have a functioning Java 1.7 runtime installed


SETUP YOUR SERVER: 
1) Download all the stuff (logstash, elasticsearch, kibana)
2) The barebones steps are in the script init.sh; it does the following:
	- enable cross-site scripting in the elasticsearch config
	- make the kibana directory is available under httpd document root
		cd /var/www/html
		ln -s /path/to/kibana-3.1.2 .
	- modify kibana config.js to point to 127.0.0.1:9200 for elasticsearch
3) The init.sh steps install some nice solace syslog event parsers
4) The init.sh steps add convenience start/stop scripts


STARTUP:
- The solace overlay provides convenience start/stop shell scripts for ElasticSearch and Logstash (Kibana runs under httpd so does not require them)
- Start them as root (logstash needs to bind to 514 which is in the privileged port range)
- There are also top-level scripts to stop/start everything, including the httpd server
- The order to start them doesn't much matter; you can bounce any one independently from the others and they will reconnect without problems
- Quick check that ElasticSearch is at least running (but without data):
		curl 'http://localhost:9200/_search?pretty'
		{
		  "took" : 1,
		  "timed_out" : false,
		  "_shards" : {
			"total" : 0,
			"successful" : 0,
			"failed" : 0
		  },
		  "hits" : {
			"total" : 0,
			"max_score" : 0.0,
			"hits" : [ ]
		  }
		}

SETUP SOLACE: set a syslog forwarder to your logstash service
	enable
	configure
	create syslog <syslog_name>
	host <ip-or-hostname-where-logstash-is> tcp
	facility event
	facility system
	facility command


VALIDATION:
- Keep running the simple search until you see syslog events (it should not take long)
	curl 'http://localhost:9200/_search?pretty'

Once events start showing up in elasticsearch, then you can start looking at kibana via:

	http://<http-server-address>:<http-server-port>/kibana-3.1.2/

Normally, dashboard configurations are saved via the GUI, and they are saved to an elasticsearch table. But the initial dashboard can be loaded in from a file (see 'Solace Dashboard.json' included in this directory). 

	Menu:Load->Advanced->Choose File

Then save it to your local instance.
