#!/usr/bin/perl -w
# nagios: -epn
use strict;
use Nagios::Plugin;
use Nagios::Plugin::DieNicely;
use XML::LibXML;
#use Data::Dumper;
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
use constant REDUNDANY_MODE_OK => "Enabled";
use constant REDUNDANY_ADBLINK_OK => OPSTATE_OK;
use constant REDUNDANY_ADBHELLO_OK => OPSTATE_OK;
use constant IFACE_ENABLED_OK => "yes";
use constant IFACE_LINK_OK => IFACE_ENABLED_OK;
use constant BRIDGE_ASTATE_OK => REDUNDANY_CONFIG_OK;
use constant BRIDGE_ISTATE_OK => "Ready-InSync";
use constant BRIDGE_OSTATE_OK => BRIDGE_ISTATE_OK;
use constant BRIDGE_ISTATE_SHUTDOWN => "Shutdown";
use constant BRIDGE_OSTATE_SHUTDOWN => BRIDGE_ISTATE_SHUTDOWN;


my $Plug_VERSION = "0.0.1";
my $Plug_BLURB = "Nagios Plugin - Solace 3200 series Messaging Appliance";
my $Plug_URL = "http://www.solacesystems.com"; 
my $Plug_PLUGINNAME = basename $0;


# Set up new Nagios Plugin Object
my $np = Nagios::Plugin->new(
    usage => "Usage: %s  -r|--router <router mgmt ip> -u|--user <username> -p|--pass <password> -b|--bridge <bridge name> --vpn <vpn name> [-o|--checkOutbound] [-q|--quiet]",
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
    spec => 'bridge|b=s',
    help => 'Bridge name',
    required => 1
);

$np->add_arg(
    spec => 'vpn=s',
    help => 'Name of the message VPN to get bridge information from',
    required => 1
);

$np->add_arg(
    spec => 'checkOutbound|o',
    help => 'Check OutBound for bi-directionnal bridges',
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

my $parser = XML::LibXML->new;

my $rpccallShow = "<rpc semp-version=\"soltr/5_3\"><show><bridge><bridge-name-pattern>".$np->opts->bridge."</bridge-name-pattern><vpn-name-pattern>".$np->opts->vpn."</vpn-name-pattern></bridge></show></rpc>";
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

if(($dom->findvalue("/rpc-reply/rpc/show/bridge/bridges/bridge/inbound-operational-state") eq BRIDGE_ISTATE_SHUTDOWN) && ($dom->findvalue("/rpc-reply/rpc/show/bridge/bridges/bridge/admin-state") eq BRIDGE_ASTATE_OK)){
	if($np->opts->quiet){
		$np->nagios_exit(OK,"Router is probably AD-Standby");
	}else{
		$np->nagios_exit(CRITICAL,"Router is probably AD-Standby, you might want to use quiet option ?");
	}
}

push @messages,"Local VPN:".$np->opts->vpn;
push @messages,"Remote VPN:".$dom->findvalue("/rpc-reply/rpc/show/bridge/bridges/bridge/connected-remote-vpn-name");
push @messages,"Remote router:".$dom->findvalue("/rpc-reply/rpc/show/bridge/bridges/bridge/connected-remote-router-name");

#"admin state"
my $adminState=OK;
my $admin=$dom->findvalue("/rpc-reply/rpc/show/bridge/bridges/bridge/admin-state");
if($admin ne BRIDGE_ASTATE_OK){
	$adminState=CRITICAL;
}
push @states,$adminState;
push @messages,"Admin State:".$admin;

#"in state"
my $inState=OK;
my $in=$dom->findvalue("/rpc-reply/rpc/show/bridge/bridges/bridge/inbound-operational-state");
if($in ne BRIDGE_ISTATE_OK){
	$inState=CRITICAL;
}
push @states,$inState;
push @messages,"Inbound State:".$in;
if (defined($np->opts->checkOutbound)){
	#"out state"
	my $outState=OK;
	my $out=$dom->findvalue("/rpc-reply/rpc/show/bridge/bridges/bridge/outbound-operational-state");
	if($out ne BRIDGE_OSTATE_OK){
		$outState=CRITICAL;
	}
	push @states,$outState;
	push @messages,"Outbound State:".$out;
}


#sort to find max in @states
@states = reverse sort { $a <=> $b } @states; 
$np->nagios_exit($states[0],"[".join(",",@messages)."]");
