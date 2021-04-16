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

use constant false => 0;
use constant true  => 1;

use constant OPSTATE_OK => "true";
use constant ADBLINKSTATE_OK => "Up";
use constant MSG_VPN_STATUS_OK => ADBLINKSTATE_OK;
use constant MSG_VPN_STATUS_STANDBY => "Standby";
use constant SEMP_VERSION => "6_2";
use constant SEMP_USERNAME              => "SEMP_USERNAME";
use constant SEMP_PASSWORD              => "SEMP_PASSWORD";
use constant RPC_ANSWER_OK              => "ok";
use constant RPC_ANSWER_FAIL            => "fail";

my $Plug_VERSION = "0.0.1";
my $Plug_BLURB = "Nagios Plugin - Solace 3200 series Messaging Appliance";
my $Plug_URL = "http://www.solacesystems.com";
my $Plug_PLUGINNAME = basename $0;
my $code;
my $result;

# Set up new Nagios Plugin Object
my $np = Nagios::Plugin->new(
    usage => "Usage: %s --routerName <router name> --router <router mgmt ip> --vpn <message-vpn>  --warnInRate <warning val (msgs)> --critInRate <Critical val (msgs)> --warnOutRate <warning val (msgs)> --critOutRate <Critical val (msgs)> --warnConn <warning val (connection)> --critConn <Critical val (connections)> [--checkAvail] --credentialsDir <path to dir>",
    version => $Plug_VERSION,
    blurb   => $Plug_BLURB,
    url     => $Plug_URL,
    plugin  => $Plug_PLUGINNAME,
    timeout => 15
);

# Add command line args
$np->add_arg(
    spec => 'routerMgmt=s',
    help => 'Router\'s Management IP address and port with : separator',
    required => 1
);

$np->add_arg(
        spec     => 'routerName=s',
        help     => 'Router\'s Name as defined in the host config file',
        required => 1
);

$np->add_arg(
    spec => 'vpn=s',
    help => 'Name of the message VPN to get queue information from',
    required => 1
);

$np->add_arg(
    spec => 'warnInRate=s',
    help => 'VPN Rate  WARNING (messages)',
    required => 1
);

$np->add_arg(
    spec => 'critInRate=s',
    help => 'VPN Rate CRITICAL (messages)',
    required => 1
);

$np->add_arg(
    spec => 'warnOutRate=s',
    help => 'VPN Rate  WARNING (messages)',
    required => 1
);

$np->add_arg(
    spec => 'critOutRate=s',
    help => 'VPN Rate CRITICAL (messages)',
    required => 1
);

$np->add_arg(
    spec => 'warnConn=s',
    help => 'VPN Connection Count WARNING (connections)',
    required => 1
);

$np->add_arg(
    spec => 'critConn=s',
    help => 'VPN Connection Count CRITICAL (connections)',
    required => 1
);

$np->add_arg(
    spec => 'checkAvail',
    help => 'Check availability',
    required => 0
);

$np->add_arg(
        spec     => 'credentialsDir=s',
        help     => 'Path to Directory containing config files with credentials for hosts to be monitored',
        required => 1
);


# get the command line options
$np->getopts;

my $hostIP              = $np->opts->routerMgmt;
my $hostName            = $np->opts->routerName;
my $credentialsDir      = $np->opts->credentialsDir;
my $vpn			= $np->opts->vpn;

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

my $rpccallShow = "<rpc semp-version=\"soltr/".SEMP_VERSION."\"><show><message-vpn><vpn-name>$vpn</vpn-name><stats/></message-vpn></show></rpc>";
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

if ( $dom->findvalue('/rpc-reply/execute-result/@code') ne
        RPC_ANSWER_OK )
{
        #Log error
        $code = UNKNOWN;
        my $message = "SEMP query succeded but result was FAIL";
        $np->nagios_exit($code,$message);
}

my $vpnName = $dom->findvalue("/rpc-reply/rpc/show/message-vpn/vpn/name");
if ($vpnName eq "") {
	$code = CRITICAL;
	$result = "VPN $vpn does not exist";
	$np->nagios_exit($code,$result);
 }
else {
	my $connections = $dom->findvalue("/rpc-reply/rpc/show/message-vpn/vpn/connections");
	my $ingressRate = $dom->findvalue("/rpc-reply/rpc/show/message-vpn/vpn/stats/current-ingress-rate-per-second");
	my $egressRate = $dom->findvalue("/rpc-reply/rpc/show/message-vpn/vpn/stats/current-egress-rate-per-second");
	my $spoolDiscards = $dom->findvalue("/rpc-reply/rpc/show/message-vpn/vpn/stats/ingress-discards/msg-spool-ingress-discards");
	my $spoolEgressDiscards = $dom->findvalue("/rpc-reply/rpc/show/message-vpn/vpn/stats/egress-discards/msg-spool-egress-discards");

	my $totalClientMessagesReceived = $dom->findvalue("/rpc-reply/rpc/show/message-vpn/vpn/stats/total-client-messages-received");
	my $totalClientMessagesSent = $dom->findvalue("/rpc-reply/rpc/show/message-vpn/vpn/stats/total-client-messages-sent");
	my $clientDataMessagesReceived = $dom->findvalue("/rpc-reply/rpc/show/message-vpn/vpn/stats/client-data-messages-received");
	my $clientDataMessagesSent = $dom->findvalue("/rpc-reply/rpc/show/message-vpn/vpn/stats/client-data-messages-sent");
	my $clientPersistentMessagesReceived = $dom->findvalue("/rpc-reply/rpc/show/message-vpn/vpn/stats/client-persistent-messages-received");
	my $clientPersistentMessagesSent = $dom->findvalue("/rpc-reply/rpc/show/message-vpn/vpn/stats/client-persistent-messages-sent");
	my $clientNonPersistentMessagesReceived = $dom->findvalue("/rpc-reply/rpc/show/message-vpn/vpn/stats/client-non-persistent-messages-received");
	my $clientNonPersistentMessagesSent = $dom->findvalue("/rpc-reply/rpc/show/message-vpn/vpn/stats/client-non-persistent-messages-sent");
	my $clientDirectMessagesReceived = $dom->findvalue("/rpc-reply/rpc/show/message-vpn/vpn/stats/client-direct-messages-received");
	my $clientDirectMessagesSent = $dom->findvalue("/rpc-reply/rpc/show/message-vpn/vpn/stats/client-direct-messages-sent");
	my $dtoMessagesReceived = $dom->findvalue("/rpc-reply/rpc/show/message-vpn/vpn/stats/dto-messages-received");
	my $totalClientBytesReceived = $dom->findvalue("/rpc-reply/rpc/show/message-vpn/vpn/stats/total-client-bytes-received");
	my $totalClientBytesSent = $dom->findvalue("/rpc-reply/rpc/show/message-vpn/vpn/stats/total-client-bytes-sent");
	my $clientDataBytesReceived = $dom->findvalue("/rpc-reply/rpc/show/message-vpn/vpn/stats/client-data-bytes-received");
	my $clientDataBytesSent = $dom->findvalue("/rpc-reply/rpc/show/message-vpn/vpn/stats/client-data-bytes-sent");
	my $clientPersistentBytesReceived = $dom->findvalue("/rpc-reply/rpc/show/message-vpn/vpn/stats/client-persistent-bytes-received");
	my $clientPersistentBytesSent = $dom->findvalue("/rpc-reply/rpc/show/message-vpn/vpn/stats/client-persistent-bytes-sent");
	my $clientNonPersistentBytesReceived = $dom->findvalue("/rpc-reply/rpc/show/message-vpn/vpn/stats/client-non-persistent-bytes-received");
	my $clientNonPersistentBytesSent = $dom->findvalue("/rpc-reply/rpc/show/message-vpn/vpn/stats/client-non-persistent-bytes-sent");
	my $clientDirectBytesReceived = $dom->findvalue("/rpc-reply/rpc/show/message-vpn/vpn/stats/client-direct-bytes-received");
	my $clientDirectBytesSent = $dom->findvalue("/rpc-reply/rpc/show/message-vpn/vpn/stats/client-direct-bytes-sent");
	my $averageIngressRatePerMinute = $dom->findvalue("/rpc-reply/rpc/show/message-vpn/vpn/stats/average-ingress-rate-per-minute");
	my $averageEgressRatePerMinute = $dom->findvalue("/rpc-reply/rpc/show/message-vpn/vpn/stats/average-egress-rate-per-minute");

	my $totalIngressDiscards = $dom->findvalue("/rpc-reply/rpc/show/message-vpn/vpn/stats/ingress-discards/total-ingress-discards");
	my $noSubscriptionMatch = $dom->findvalue("/rpc-reply/rpc/show/message-vpn/vpn/stats/ingress-discards/no-subscription-match");

	my $totalEgressDiscards = $dom->findvalue("/rpc-reply/rpc/show/message-vpn/vpn/stats/ingress-discards/total-egress-discards");
	my $transmitCongestion = $dom->findvalue("/rpc-reply/rpc/show/message-vpn/vpn/stats/ingress-discards/transmit-congestion");

	# populate the performance data

	$np->add_perfdata(
	    label => "Connection Count",
	    value => $connections,
	    warning => $np->opts->warnConn,
	    critical => $np->opts->critConn
	);

	$np->add_perfdata(
	    label => "Ingress Rate",
	    value => $ingressRate,
	    warning => $np->opts->warnInRate,
	    critical => $np->opts->critInRate
	);


	$np->add_perfdata(
	    label => "Egress Rate",
	    value => $egressRate,
	    warning => $np->opts->warnOutRate,
	    critical => $np->opts->critOutRate
	);


	$np->add_perfdata(
	    label => "Spool Discards (Egress)",
	    value => $spoolEgressDiscards,
	);


	$np->add_perfdata(
	    label => "Spool Discards (Ingress)",
	    value => $spoolDiscards
	);


	$np->add_perfdata(label => "totalClientMessagesReceived", value => $totalClientMessagesReceived);
	$np->add_perfdata(label => "totalClientMessagesSent", value => $totalClientMessagesSent);
	$np->add_perfdata(label => "clientDataMessagesReceived", value => $clientDataMessagesReceived);
	$np->add_perfdata(label => "clientDataMessagesSent", value => $clientDataMessagesSent);
	$np->add_perfdata(label => "clientPersistentMessagesReceived", value => $clientPersistentMessagesReceived);
	$np->add_perfdata(label => "clientPersistentMessagesSent", value => $clientPersistentMessagesSent);
	$np->add_perfdata(label => "clientNonPersistentMessagesReceived", value => $clientNonPersistentMessagesReceived);
	$np->add_perfdata(label => "clientNonPersistentMessagesSent", value => $clientNonPersistentMessagesSent);
	$np->add_perfdata(label => "clientDirectMessagesReceived", value => $clientDirectMessagesReceived);
	$np->add_perfdata(label => "clientDirectMessagesSent", value => $clientDirectMessagesSent);
	$np->add_perfdata(label => "dtoMessagesReceived", value => $dtoMessagesReceived);
	$np->add_perfdata(label => "totalClientBytesReceived", value => $totalClientBytesReceived);
	$np->add_perfdata(label => "totalClientBytesSent", value => $totalClientBytesSent);
	$np->add_perfdata(label => "clientDataBytesReceived", value => $clientDataBytesReceived);
	$np->add_perfdata(label => "clientDataBytesSent", value => $clientDataBytesSent);
	$np->add_perfdata(label => "clientPersistentBytesReceived", value => $clientPersistentBytesReceived);
	$np->add_perfdata(label => "clientPersistentBytesSent", value => $clientPersistentBytesSent);
	$np->add_perfdata(label => "clientNonPersistentBytesReceived", value => $clientNonPersistentBytesReceived);
	$np->add_perfdata(label => "clientNonPersistentBytesSent", value => $clientNonPersistentBytesSent);
	$np->add_perfdata(label => "clientDirectBytesReceived", value => $clientDirectBytesReceived);
	$np->add_perfdata(label => "clientDirectBytesSent", value => $clientDirectBytesSent);
	$np->add_perfdata(label => "averageIngressRatePerMinute", value => $averageIngressRatePerMinute);
	$np->add_perfdata(label => "averageEgressRatePerMinute", value => $averageEgressRatePerMinute);

	$np->add_perfdata(label => "totalIngressDiscards", value => $totalIngressDiscards);
	$np->add_perfdata(label => "noSubscriptionMatch", value => $noSubscriptionMatch);

	$np->add_perfdata(label => "totalEgressDiscards", value => $totalEgressDiscards);
	$np->add_perfdata(label => "transmitCongestion", value => $transmitCongestion);

	#print "\n\n\n $currMsg    $warn    $crit\n\n\n";
	#

	my $connState ="";
	my $ingressState ="";
	my $egressState ="";
	my $pluginState=0;
	$code=0;

	$code = $np->check_threshold(
	    check => $connections,
	    warning => $np->opts->warnConn,
	    critical => $np->opts->critConn                
	);

	if ($code > $pluginState) {
	    $pluginState = $code;
	}

	if ($code == 0) {
	    $connState="CONNECTION_OK";
	}

	if ($code == 1) {
	    $connState="CONNECTION_WARN";
	}

	if ($code == 2) {
	    $connState="CONNECTION_CRIT";
	}

	$code = $np->check_threshold(
	    check => $egressRate,
	    warning => $np->opts->warnOutRate,
	    critical => $np->opts->critOutRate                
	);

	if ($code > $pluginState) {
	    $pluginState = $code;
	}

	if ($code == 0) {
	    $egressState="EGRESS_OK";
	}

	if ($code == 1) {
	    $egressState="EGRESS_WARN";
	}

	if ($code == 2) {
	    $egressState="EGRESS_CRIT";
	}


	$code = $np->check_threshold(
	    check => $ingressRate,
	    warning => $np->opts->warnInRate,
	    critical => $np->opts->critInRate                
	);

	if ($code > $pluginState) {
	    $pluginState = $code;
	}

	if ($code == OK) {
	    $ingressState="INGRESS_OK";
	}

	if ($code == WARNING) {
	    $ingressState="INGRESS_WARN";
	}

	if ($code == CRITICAL) {
	    $ingressState="INGRESS_CRIT";
	}

	my $availState="";

	if(defined($np->opts->checkAvail)){
		$code=OK;
		if (
		    ($dom->findvalue("/rpc-reply/rpc/show/message-vpn/vpn/local-status") ne MSG_VPN_STATUS_OK) && 
		    ($dom->findvalue("/rpc-reply/rpc/show/message-vpn/vpn/local-status") ne MSG_VPN_STATUS_STANDBY)
		){
			$code=CRITICAL;
		}
		if ($code > $pluginState) {
	    $pluginState = $code;
	}


	if ($code == OK) {
	    $availState="AVAIL_OK";
	}

	if ($code == WARNING) {
	    $availState="AVAIL_WARN";
	}

	if ($code == CRITICAL) {
	    $availState="AVAIL_CRIT";
	}
	}

	$np->nagios_exit($pluginState,"$connState.$ingressState.$egressState.$availState");
	}
