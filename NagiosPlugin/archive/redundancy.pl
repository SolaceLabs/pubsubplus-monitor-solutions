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
use constant PMSTATE_OK => "Ok";
use constant FSTATE_OK =>"Ready";
use constant FCSTATE_OK =>"Online";
use constant LUNSTATE_OK=>FSTATE_OK;
use constant REDUNDANY_CONFIG_OK => "Enabled";
use constant REDUNDANY_MODE_OK => "Active/Active";
use constant REDUNDANY_ADBLINK_OK => OPSTATE_OK;
use constant REDUNDANY_ADBHELLO_OK => OPSTATE_OK;

my $Plug_VERSION = "0.0.1";
my $Plug_BLURB = "Nagios Plugin - Solace 3200 series Messaging Appliance";
my $Plug_URL = "http://www.solacesystems.com"; 
my $Plug_PLUGINNAME = basename $0;


# Set up new Nagios Plugin Object
my $np = Nagios::Plugin->new(
    usage => "Usage: %s  -r|--router <router mgmt ip> -u|--user <username> -p|--pass <password>",
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

# get the command line options
$np->getopts;

my $login = $np->opts->user . ":" . $np->opts->pass;
my $host = $np->opts->router;

my $parser = XML::LibXML->new;

my $rpccallShow = "<rpc semp-version=\"soltr/5_3\"><show><redundancy></redundancy></show></rpc>";
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
my @states;
my @messages;

#"config-status"
my $rsState=OK;
my $rs=$dom->findvalue("/rpc-reply/rpc/show/redundancy/config-status");
if($rs ne REDUNDANY_CONFIG_OK){
	$rsState=CRITICAL;
}
push @states,$rsState;
push @messages,"Config:".$rs;

#"mode"
my $rmState=OK;
my $rm=$dom->findvalue("/rpc-reply/rpc/show/redundancy/redundancy-mode");
if($rm ne REDUNDANY_MODE_OK){
	$rmState=CRITICAL;
}
push @states,$rmState;
push @messages,"Mode:".$rm;

#"Mate"
push @messages,"Mate:".$dom->findvalue("/rpc-reply/rpc/show/redundancy/mate-router-name");

#"ADB Link"
my $alState=OK;
my $al=$dom->findvalue("/rpc-reply/rpc/show/redundancy/oper-status/adb-link-up");
if($al ne REDUNDANY_ADBLINK_OK){
	$alState=CRITICAL;
}
push @states,$alState;
push @messages,"ADB Link:".$al;

#"ADB Hello"
my $ahState=OK;
my $ah=$dom->findvalue("/rpc-reply/rpc/show/redundancy/oper-status/adb-hello-up");
if($ah ne REDUNDANY_ADBHELLO_OK){
	$ahState=CRITICAL;
}
push @states,$ahState;
push @messages,"ADB Hello:".$ah;

#sort to find max in @states
@states = reverse sort { $a <=> $b } @states; 
$np->nagios_exit($states[0],"[".join(",",@messages)."]");
