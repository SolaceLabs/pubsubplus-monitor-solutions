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
use constant ADBLINKSTATE_OK => "Ok";
use constant PMSTATE_OK => "Ok";
use constant FSTATE_OK =>"Ready";
#use constant FCSTATE_OK =>/^Link Up/;
use constant LUNSTATE_OK=>FSTATE_OK;

my $Plug_VERSION = "0.0.1";
my $Plug_BLURB = "Nagios Plugin - Solace 3200 series Messaging Appliance";
my $Plug_URL = "http://www.solacesystems.com"; 
my $Plug_PLUGINNAME = basename $0;


# Set up new Nagios Plugin Object
my $np = Nagios::Plugin->new(
    usage => "Usage: %s  -r|--router <router mgmt ip> -u|--user <username> -p|--pass <password> [-w|--warning <warning val (power supply)> -c|--critical <Critical val (power supply)>] [-a|--checkADB] [-b|--checkHBA]",
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
    spec => 'warning|w=s',
    help => 'Power supply WARNING',
    required => 0
);

$np->add_arg(
    spec => 'critical|c=s',
    help => 'Power supply CRITICAL',
    required => 0
);

$np->add_arg(
    spec => 'checkADB|a',
    help => 'Check ADB (State,Links,Power Module)',
    required => 0
);
$np->add_arg(
    spec => 'checkHBA|b',
    help => 'Check HBA (Links State,LUN State)',
    required => 0
);
# get the command line options
$np->getopts;

my $login = $np->opts->user . ":" . $np->opts->pass;
my $host = $np->opts->router;
my $pluginState=OK;
my $code=OK;
my $opState=OK;
my $matelinkState=OK;
my $matelinkCount=0;
my $pmState=OK;
my $fState=OK;
my $fcCount=0;
my $ADBmessage="";

my $psState=OK;
my $PSmessage="";

my $diskState=OK;
my $lunState=OK;
my $Diskmessage="";

my @states;
my @messages;

my $parser = XML::LibXML->new;

my $rpccallShow = "<rpc semp-version=\"soltr/5_3\"><show><hardware><details/></hardware></show></rpc>";

my $ua = new LWP::UserAgent;
my $req = new HTTP::Request 'POST',"http://".$login."@".$host."/SEMP";
$req->content_type('application/x-www-form-urlencoded');
$req->content($rpccallShow);

# send the request
my $res = $ua->request($req);

if ($res->is_error) {
# HTTP error
$np->die("error in http");
}

my $dom = $parser->parse_string($res->content);
#Check ADB
if($np->opts->checkADB){
#Check Operationnal state
my $op=$dom->findvalue("/rpc-reply/rpc/show/hardware/fabric/slot/card-type[text()='Assured Delivery Blade']/../operational-state-up");
if($op ne OPSTATE_OK){
	$opState=CRITICAL;
}
push @states,$opState;

#Check mate-link-1
my $ml1=$dom->findvalue("/rpc-reply/rpc/show/hardware/fabric/slot/card-type[text()='Assured Delivery Blade']/../mate-link-1-state");
if($ml1 eq ADBLINKSTATE_OK){
	$matelinkCount++;
}
#Check mate-link-2
my $ml2=$dom->findvalue("/rpc-reply/rpc/show/hardware/fabric/slot/card-type[text()='Assured Delivery Blade']/../mate-link-2-state");
if($ml2 eq ADBLINKSTATE_OK){
	$matelinkCount++;
}
$matelinkState=CRITICAL-$matelinkCount;
push @states,$matelinkState;

#"power-module-state"
my $pm=$dom->findvalue("/rpc-reply/rpc/show/hardware/fabric/slot/card-type[text()='Assured Delivery Blade']/../power-module-state");
if($pm ne PMSTATE_OK){
	$pmState=CRITICAL;
}
push @states,$pmState;
#"flash-state"
my $fs=$dom->findvalue("/rpc-reply/rpc/show/hardware/fabric/slot/card-type[text()='Assured Delivery Blade']/../flash/state");
if($fs ne FSTATE_OK){
	$fState=CRITICAL;
}
push @states,$fState;
$ADBmessage=sprintf("ADB [Operational State: %s, Flash Card State: %s, Power Module State: %s, Mate Link Port 1: %s, Mate Link Port 2: %s]",$op,$fs,$pm,$ml1,$ml2);
push @messages,$ADBmessage;
}
#Check power supplies
if(defined($np->opts->critical) && defined($np->opts->warning)){
	my $ps=$dom->findvalue("/rpc-reply/rpc/show/hardware/power-redundancy/operational-power-supplies");
	$psState = $np->check_threshold(
    check => $ps,
    warning => $np->opts->warning,
    critical => $np->opts->critical                
);
push @states,$psState;
$PSmessage=sprintf("Power [Operational Supplies: %s]",$ps);
push @messages,$PSmessage;
}
#Check HBA
if(defined($np->opts->checkHBA)){
#fc-port1	
my $fc1=$dom->findvalue("/rpc-reply/rpc/show/hardware/fabric/slot/fibre-channel/number[text()='1']/../state");
#if($fc1 =~ FCSTATE_OK){
if($fc1 =~ /^Link Up/){
	$fcCount++;
}
#fc-port2	
my $fc2=$dom->findvalue("/rpc-reply/rpc/show/hardware/fabric/slot/fibre-channel/number[text()='2']/../state");
#if($fc2 =~ FCSTATE_OK){
if($fc2 =~ /^Link Up/){
	$fcCount++;
}

$diskState=CRITICAL-$fcCount;
push @states,$diskState;
#external-disk	
my $lun=$dom->findvalue("/rpc-reply/rpc/show/hardware/fabric/slot/external-disk-lun/state");
if($lun ne LUNSTATE_OK){
	$lunState=CRITICAL;
}
push @states,$lunState;
$Diskmessage=sprintf("HBA [FC 1: %s, FC 2: %s, LUN: %s]",$fc1,$fc2,$lun);
push @messages,$Diskmessage;
}
#sort to find max in @states
@states = reverse sort { $a <=> $b } @states; 
$np->nagios_exit($states[0],join(" ",@messages));