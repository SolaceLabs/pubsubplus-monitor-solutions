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

use constant OPSTATE_OK => "true";
use constant ADBLINKSTATE_OK => "Up";
use constant MSG_VPN_STATUS_OK => ADBLINKSTATE_OK;
use constant MSG_VPN_STATUS_STANDBY => "Standby";

my $Plug_VERSION = "0.0.1";
my $Plug_BLURB = "Nagios Plugin - Solace 3200 series Messaging Appliance";
my $Plug_URL = "http://www.solacesystems.com";
my $Plug_PLUGINNAME = basename $0;


# Set up new Nagios Plugin Object
my $np = Nagios::Plugin->new(
    usage => "Usage: %s -r|--router <router mgmt ip> -u|--user <username> -p|--pass <password> --vpn <message-vpn>  --warnInRate <warning val (msgs)> --critInRate <Critical val (msgs)> --warnOutRate <warning val (msgs)> --critOutRate <Critical val (msgs)> --warnConn <warning val (connection)> --critConn <Critical val (connections)> [--checkAvail]",
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

# get the command line options
$np->getopts;

my $login = $np->opts->user . ":" . $np->opts->pass;
my $host = $np->opts->router;
my $vpn = $np->opts->vpn;


my $parser = XML::LibXML->new;

my $rpccallShow = "<rpc semp-version=\"soltr/5_3\"><show><message-vpn><vpn-name>$vpn</vpn-name><stats/></message-vpn></show></rpc>";
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

my $connections = $dom->findvalue("/rpc-reply/rpc/show/message-vpn/vpn/connections");
my $ingressRate = $dom->findvalue("/rpc-reply/rpc/show/message-vpn/vpn/stats/current-ingress-rate-per-second");
my $egressRate = $dom->findvalue("/rpc-reply/rpc/show/message-vpn/vpn/stats/current-egress-rate-per-second");
my $spoolDiscards = $dom->findvalue("/rpc-reply/rpc/show/message-vpn/vpn/stats/ingress-discards/msg-spool-ingress-discards");
my $spoolEgressDiscards = $dom->findvalue("/rpc-reply/rpc/show/message-vpn/vpn/stats/egress-discards/msg-spool-egress-discards");

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


#print "\n\n\n $currMsg    $warn    $crit\n\n\n";
#

my $connState ="";
my $ingressState ="";
my $egressState ="";
my $pluginState=0;
my $code=0;

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
