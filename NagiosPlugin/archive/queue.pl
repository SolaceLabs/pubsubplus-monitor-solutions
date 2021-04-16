#!/usr/bin/perl -w
# nagios: -epn
use strict;
use Nagios::Plugin;
use Nagios::Plugin::DieNicely;
use XML::LibXML;
use File::Basename;
use LWP::UserAgent;

use constant false => 0;
use constant true  => 1;

use constant ADBLINKSTATE_OK => "Up";
use constant QUEUE_EGRESS_OK => ADBLINKSTATE_OK;
use constant QUEUE_INGRESS_OK => ADBLINKSTATE_OK;
use constant RPC_ANSWER_OK => "ok";
use constant RPC_ANSWER_FAIL => "fail";

my $Plug_VERSION = "0.0.1";
my $Plug_BLURB = "Nagios Plugin - Solace 3200 series Messaging Appliance";
my $Plug_URL = "http://www.solacesystems.com"; 
my $Plug_PLUGINNAME = basename $0;


# Set up new Nagios Plugin Object
my $np = Nagios::Plugin->new(
    usage => "Usage: %s  -r|--router <router mgmt ip> -u|--user <username> -p|--pass <password> --vpn <message-vpn> --queue <queue name> --warning <warning val (msgs)> --critical <Critical val (msgs)> [--checkAvail] [-q|--quiet]",
    version => $Plug_VERSION,
    blurb   => $Plug_BLURB,
    url     => $Plug_URL,
    plugin  => $Plug_PLUGINNAME,
    timeout => 15
);

# Add command line args
$np->add_arg(
    spec => 'router|r=s',
    help => 'Router\'s Management IP address and port with : separator',
    required => 1
);

$np->add_arg(
    spec => 'user|u=s',
    help => 'Username for management access',
    required => 1
);

$np->add_arg(
    spec => 'pass|p=s',
    help => 'Password for management access',
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
    spec => 'warning=s',
    help => 'Queue depth WARNING (messages)',
    required => 1
);

$np->add_arg(
    spec => 'critical=s',
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

# get the command line options
$np->getopts;

my $login = $np->opts->user . ":" . $np->opts->pass;
my $host = $np->opts->router;
my $vpn = $np->opts->vpn;
my $queue = $np->opts->queue;
my $warn = $np->opts->warning;
my $crit = $np->opts->critical;


my $parser = XML::LibXML->new;

my $rpccallShow = "<rpc semp-version=\"soltr/5_3\"><show><queue><name>$queue</name><vpn-name>$vpn</vpn-name></queue></show></rpc>";
my $ua;
my $req;
my $res;

$ua = new LWP::UserAgent;
$req = new HTTP::Request 'POST',"http://".$login."@".$host."/SEMP";
$req->content_type('application/x-www-form-urlencoded');
$req->content($rpccallShow);

# send the request
$res = $ua->request($req);

if ($res->is_error) {
# HTTP error
$np->die("error in http");
}

my $dom = $parser->parse_string($res->content);
if($dom->findvalue('/rpc-reply/execute-result/@code') ne RPC_ANSWER_OK){
	if ($np->opts->quiet){
	$np->nagios_exit(OK,"$host probably AD-Standby");
	}else{
	$np->nagios_exit(CRITICAL,"$host probably AD-Standby with no quiet option");	
	}
}

my $currMsg = $dom->findvalue("/rpc-reply/rpc/show/queue/queues/queue/info/num-messages-spooled");
my $currMByte = $dom->findvalue("/rpc-reply/rpc/show/queue/queues/queue/info/current-spool-usage-in-mb");
my $hwm = $dom->findvalue("/rpc-reply/rpc/show/queue/queues/queue/info/high-water-mark-in-mb");
my $totDelUnacked = $dom->findvalue("/rpc-reply/rpc/show/queue/queues/queue/info/total-delivered-unacked-msgs");
my $bind = $dom->findvalue("/rpc-reply/rpc/show/queue/queues/queue/info/bind-count");
my $topicCount = $dom->findvalue("/rpc-reply/rpc/show/queue/queues/queue/info/topic-subscription-count");
my $percent =sprintf("%.2f", $currMByte/$dom->findvalue("/rpc-reply/rpc/show/queue/queues/queue/info/quota")*100);
my @states;
my @messages;

# populate the performance data

$np->add_perfdata(
    label => "Message Count",
    value => $currMsg,
    uom => "Msgs",
    warning => $warn,
    critical => $crit
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
    check => $currMsg
);

#sort to find max in @states
@states = reverse sort { $a <=> $b } @states; 
$np->nagios_exit($states[0],"[".join(",",@messages)."]");

