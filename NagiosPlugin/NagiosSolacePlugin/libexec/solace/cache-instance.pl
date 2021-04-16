#!/usr/bin/perl -w
# nagios: -epn
use strict;
use Nagios::Plugin;
use Nagios::Plugin::DieNicely;
use XML::LibXML;
use File::Basename;
use LWP::UserAgent;
use FindBin;
use lib "$FindBin::Bin";
use Utils;
use Data::Dumper;

#print %INC;
use constant false => 0;
use constant true  => 1;
use constant SEMP_USERNAME              => "SEMP_USERNAME";
use constant SEMP_PASSWORD              => "SEMP_PASSWORD";
use constant SEMP_VERSION               => "6_2";

use constant STATE_OK 			=> "Up";
use constant RPC_ANSWER_OK 		=> "ok";
use constant RPC_ANSWER_FAIL		 => "fail";

my $Plug_VERSION = "0.0.1";
my $Plug_BLURB = "Nagios Plugin - Solace 3200 series Messaging Appliance";
my $Plug_URL = "http://www.solacesystems.com"; 
my $Plug_PLUGINNAME = basename $0;
my $code;
my $result;

# Set up new Nagios Plugin Object
my $np = Nagios::Plugin->new(
    usage => "Usage: %s --routerName <router name> --routerMgmt <router mgmt ip> --credentialsDir <pathToDir> --vpn <message-vpn> --cacheInstance <instance name> --cacheCluster <cluster-name> --distributedCache <dist-cache name>",
    version => $Plug_VERSION,
    blurb   => $Plug_BLURB,
    url     => $Plug_URL,
    plugin  => $Plug_PLUGINNAME,
    timeout => 15
);

# Add command line args
$np->add_arg(
        spec     => 'routerName=s',
        help     => 'Router\'s Name as defined in the host config file',
        required => 1
);

$np->add_arg(
    spec => 'routerMgmt=s',
    help => 'Router\'s Management IP address and port with : separator',
    required => 1
);

$np->add_arg(
        spec     => 'credentialsDir=s',
        help     => 'Path to Directory containing config files with credentials for hosts to be monitored',
        required => 1
);

$np->add_arg(
    spec => 'vpn=s',
    help => 'Name of the message VPN to get queue infomation from',
    required => 1
);

$np->add_arg(
    spec => 'cacheInstance=s',
    help => 'Name of the cache-instance to be monitored',
    required => 1
);

$np->add_arg(
    spec => 'cacheCluster=s',
    help => 'Name of cache-cluster',
    required => 1
);

$np->add_arg(
    spec => 'distributedCache=s',
    help => 'Name of distributed-cache',
    required => 1
);


# get the command line options
$np->getopts;


my $hostIP              = $np->opts->routerMgmt;
my $hostName            = $np->opts->routerName;
my $credentialsDir      = $np->opts->credentialsDir;
my $cacheInstance	= $np->opts->cacheInstance;
my $cacheCluster	= $np->opts->cacheCluster;
my $distributedCache	= $np->opts->distributedCache;
my $vpn			= $np->opts->vpn;

#print Dumper ($np->opts);

unless (-d $credentialsDir) {
        $code = UNKNOWN;
        $result =  "Credentials directory $credentialsDir does not exist!";
        $np->nagios_exit($code, $result);
}

#get credentials from files
my $routerConfigs = getRouterConfigs($credentialsDir,$hostName);

if ($routerConfigs == 0){
        $code   = UNKNOWN;
        $result =  "Unable to read credentials for router:$hostName from directory:$credentialsDir";
        $np->nagios_exit($code,$result);
}

my $solUserName   = $routerConfigs->{SEMP_USERNAME};
my $solPasswd     = $routerConfigs->{SEMP_PASSWORD};
my $login         = $solUserName . ":". $solPasswd;


my $parser = XML::LibXML->new;
# first check the cache admin status
my $rpccallShow = "<rpc semp-version=\"soltr/".SEMP_VERSION."\"><show><cache-instance><name>$cacheInstance</name><cluster-name>$cacheCluster</cluster-name><cache-name>$distributedCache</cache-name></cache-instance></show></rpc>";

my $ua; 
my $req; 
my $res;

$ua = new LWP::UserAgent;
$req = new HTTP::Request 'POST',"http://".$login."@".$hostIP."/SEMP";
$req->content_type('application/x-www-form-urlencoded');
$req->content($rpccallShow);

# send the request
$res = $ua->request($req);

if ($res->is_error) {
	# HTTP error
	$code = UNKNOWN;
	my $message = "HTTP Error:SEMP query failed to complete";
	$np->nagios_exit($code,$message);
}

my $dom = $parser->parse_string($res->content);

if($dom->findvalue('/rpc-reply/execute-result/@code') ne RPC_ANSWER_OK){
        $code = UNKNOWN;
        my $message ="SEMP query succeded but result was FAIL";
        $np->nagios_exit($code,$message);
}

my @messages;

my $adminStatus = $dom->findvalue("/rpc-reply/rpc/show/cache-instance/cache-instances/cache-instance/admin-status");
my $operStatus  = $dom->findvalue("/rpc-reply/rpc/show/cache-instance/cache-instances/cache-instance/oper-status");

push (@messages,"Admin status:$adminStatus");
push (@messages,"Oper status:$operStatus");

if ($adminStatus ne STATE_OK){
	$code =  CRITICAL;
        $np->nagios_exit($code,join(",",@messages) );
} elsif ($operStatus ne STATE_OK){
	
        $code =  CRITICAL;
        $np->nagios_exit($code,join(",",@messages));
}else{
	# get Detailed stats

	$rpccallShow = "<rpc semp-version=\"soltr/".SEMP_VERSION."\"><show><cache-instance><name>$cacheInstance</name><cluster-name>$cacheCluster</cluster-name><cache-name>$distributedCache</cache-name><remote/><status/></cache-instance></show></rpc>";

	# send the request
	$req->content($rpccallShow);
	$res = $ua->request($req);

	if ($res->is_error) {
		# HTTP error
        	$code = UNKNOWN;
        	my $errorMsg = "HTTP Error:SEMP query failed to complete";
        	$np->nagios_exit($code,$errorMsg);
	}

	$dom = $parser->parse_string($res->content);

	if($dom->findvalue('/rpc-reply/execute-result/@code') ne RPC_ANSWER_OK){
		
        	$code = UNKNOWN;
        	my $errorMsg = "SEMP query succeded but result was FAIL";
        	$np->nagios_exit($code,$errorMsg);
	}

	my $currentMsgsCached    	= $dom->findvalue("/rpc-reply/rpc/show/cache-instance/cache-instances/cache-instance/remote/status/stats/stat/name[text()='Messages Cached']/../current-value");
	my $currentMsgBytesCached   	= $dom->findvalue("/rpc-reply/rpc/show/cache-instance/cache-instances/cache-instance/remote/status/stats/stat/name[text()='Message Bytes Cached']/../current-value");
	my $currentTopicsCached  	= $dom->findvalue("/rpc-reply/rpc/show/cache-instance/cache-instances/cache-instance/remote/status/stats/stat/name[text()='Cached Topics']/../current-value"); 
	
	#Populate the performance data
	
	$np->add_perfdata(label => "Messages Cached", value => $currentMsgsCached);
	$np->add_perfdata(label => "Message Bytes Cached", value => $currentMsgBytesCached);
	$np->add_perfdata(label => "Cached Topics", value => $currentTopicsCached);
	
        $code = OK;

        $np->nagios_exit($code,join(",",@messages)) ;
        

}
