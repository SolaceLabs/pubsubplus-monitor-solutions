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

#print %INC;
use constant false => 0;
use constant true  => 1;
use constant SEMP_USERNAME              => "SEMP_USERNAME";
use constant SEMP_PASSWORD              => "SEMP_PASSWORD";
use constant SEMP_VERSION               => "6_2";

use constant ADBLINKSTATE_OK => "Up";
use constant QUEUE_EGRESS_OK => ADBLINKSTATE_OK;
use constant QUEUE_INGRESS_OK => ADBLINKSTATE_OK;
use constant RPC_ANSWER_OK => "ok";
use constant RPC_ANSWER_FAIL => "fail";

my $Plug_VERSION = "0.0.1";
my $Plug_BLURB = "Nagios Plugin - Solace 3200 series Messaging Appliance";
my $Plug_URL = "http://www.solacesystems.com"; 
my $Plug_PLUGINNAME = basename $0;
my $code;
my $result;

# Set up new Nagios Plugin Object
my $np = Nagios::Plugin->new(
    usage => "Usage: %s --routerName <router name> --routerMgmt <router mgmt ip> --credentialsDir <pathToDir> --vpn <message-vpn> --queue <queue name> --warningMsgs <warning val (msgs)> --criticalMsgs <Critical val (msgs)> --warningUsage <warning percent> --criticalUsage <critical percent> [--checkAvail] [-q|--quiet]",
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
    spec => 'queue=s',
    help => 'Name of the queue to get infomation from',
    required => 1
);

$np->add_arg(
    spec => 'warningMsgs=s',
    help => 'Queue depth WARNING (messages)',
    required => 1
);

$np->add_arg(
    spec => 'criticalMsgs=s',
    help => 'Queue depth CRITICAL (messages)',
    required => 1
);

$np->add_arg(
    spec => 'checkAvail',
    help => 'Check availability',
    required => 0
);

$np->add_arg(
    spec => 'quiet|q',
    help => 'Allow to return Ok if router is not AD-Active',
    required => 0
);

$np->add_arg(
    spec => 'warningUsage=s',
    help => 'Queue spool usage WARNING (percent)',
    required => 1
);

$np->add_arg(
    spec => 'criticalUsage=s',
    help => 'Queue spool usage CRITICAL (percent)',
    required => 1
);

# get the command line options
$np->getopts;


my $hostIP              = $np->opts->routerMgmt;
my $hostName            = $np->opts->routerName;
my $credentialsDir      = $np->opts->credentialsDir;
my $queue		= $np->opts->queue;
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

my $warningMsgs = $np->opts->warningMsgs;
my $criticalMsgs = $np->opts->criticalMsgs;

my $warningUsage = $np->opts->warningUsage;
my $criticalUsage = $np->opts->criticalUsage;

my $parser = XML::LibXML->new;

my $rpccallShow = "<rpc semp-version=\"soltr/".SEMP_VERSION."\"><show><queue><name>$queue</name><vpn-name>$vpn</vpn-name></queue></show></rpc>";
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
	if ($np->opts->quiet){
	$np->nagios_exit(OK,"$hostName probably AD-Standby");
	}else{
	$np->nagios_exit(CRITICAL,"$hostName probably AD-Standby with no quiet option");	
	}
}

my $queueName = $dom->findvalue("/rpc-reply/rpc/show/queue/queues/queue/name");
if ($queueName eq "") {
	$code 	= CRITICAL;
	$result = "Unable to detect queue: $queue in VPN:$vpn";
	$np->nagios_exit($code,$result);
}else {	
	my $currMsg = $dom->findvalue("/rpc-reply/rpc/show/queue/queues/queue/info/num-messages-spooled");
	my $currMByte = $dom->findvalue("/rpc-reply/rpc/show/queue/queues/queue/info/current-spool-usage-in-mb");
	my $hwm = $dom->findvalue("/rpc-reply/rpc/show/queue/queues/queue/info/high-water-mark-in-mb");
	my $totDelUnacked = $dom->findvalue("/rpc-reply/rpc/show/queue/queues/queue/info/total-delivered-unacked-msgs");
	my $bind = $dom->findvalue("/rpc-reply/rpc/show/queue/queues/queue/info/bind-count");
	my $topicCount = $dom->findvalue("/rpc-reply/rpc/show/queue/queues/queue/info/topic-subscription-count");
	my $quota = $dom->findvalue("/rpc-reply/rpc/show/queue/queues/queue/info/quota");
	my $percent;
	if ($quota > 0){
		$percent =sprintf("%.2f", $currMByte/$quota)*100;
	}
	my @states;
	my @messages;

	# populate the performance data

	$np->add_perfdata(
	    label => "Message Count",
	    value => $currMsg,
	    uom => "Msgs",
	    warning => $warningMsgs,
	    critical => $criticalMsgs
	);

	$np->add_perfdata(
	    label => "Spool Size",
	    value => $currMByte,
	    uom => "MB",
	);
	$np->add_perfdata(
	    label => "Spool Usage",
	    value => $percent,
	    uom => "%",
            warning => $warningUsage,
	    critical => $criticalUsage
	);

	$np->add_perfdata(
	    label => "High Water Mark",
	    value => $hwm,
	    uom => "MB"
	);


	$np->add_perfdata(
	    label => "Delivered Unacked Messages",
	    value => $totDelUnacked,
	    uom => "Msgs"
	);


	$np->add_perfdata(
	    label => "Bind Count",
	    value => $bind
	);

	$np->add_perfdata(
	    label => "Topic Count",
	    value => $topicCount
	);
	# States messages
	push @messages, "VPN:".$vpn;
	push @messages, "Queue:".$queue;

	#ingress
	my $QueueState=CRITICAL;
	if($dom->findvalue("/rpc-reply/rpc/show/queue/queues/queue/info/ingress-config-status") eq QUEUE_INGRESS_OK){
		$QueueState--;
	}
	push @messages, "Ingress:".$dom->findvalue("/rpc-reply/rpc/show/queue/queues/queue/info/ingress-config-status");
	#egress
	if($dom->findvalue("/rpc-reply/rpc/show/queue/queues/queue/info/egress-config-status") eq QUEUE_EGRESS_OK){
		$QueueState--;
	}
	push @messages, "Egress:".$dom->findvalue("/rpc-reply/rpc/show/queue/queues/queue/info/egress-config-status");
	push @states,$QueueState;

	push @states,$np->check_threshold(
	    check => $currMsg,
	    warning => $warningMsgs,
            critical => $criticalMsgs
	);

	push @states,$np->check_threshold(
            check => $percent,
            warning => $warningUsage,
            critical => $criticalUsage
        );


	#sort to find max in @states
	@states = reverse sort { $a <=> $b } @states; 
	$np->nagios_exit($states[0],"[".join(",",@messages)."]");
}
