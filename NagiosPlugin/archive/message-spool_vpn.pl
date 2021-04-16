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
use constant REDUNDANY_MODE_OK => "Enabled";
use constant REDUNDANY_ADBLINK_OK => OPSTATE_OK;
use constant REDUNDANY_ADBHELLO_OK => OPSTATE_OK;
use constant IFACE_ENABLED_OK => "yes";
use constant IFACE_LINK_OK => IFACE_ENABLED_OK;


my $Plug_VERSION = "0.0.1";
my $Plug_BLURB = "Nagios Plugin - Solace 3200 series Messaging Appliance";
my $Plug_URL = "http://www.solacesystems.com"; 
my $Plug_PLUGINNAME = basename $0;


# Set up new Nagios Plugin Object
my $np = Nagios::Plugin->new(
    usage => "Usage: %s  -r|--router <router mgmt ip> -u|--user <username> -p|--pass <password> --vpn <vpn name> -w|--warning <usage %> -c|--critical <usage %>",
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
    help => 'Vpn to get stat for',
    required => 1
);

$np->add_arg(
    spec => 'warning|w=s',
    help => 'warning threshold for spool usage (%)',
    required => 1
);

$np->add_arg(
    spec => 'critical|c=s',
    help => 'critical threshold for spool usage (%)',
    required => 1
);

# get the command line options
$np->getopts;

my $login = $np->opts->user . ":" . $np->opts->pass;
my $host = $np->opts->router;

my $parser = XML::LibXML->new;

my $rpccallShow = "<rpc semp-version=\"soltr/5_3\"><show><message-spool><vpn-name>".$np->opts->vpn."</vpn-name></message-spool></show></rpc>";
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

my $percent=($dom->findvalue("/rpc-reply/rpc/show/message-spool/message-vpn/vpn/current-spool-usage-mb")/$dom->findvalue("/rpc-reply/rpc/show/message-spool/message-vpn/vpn/maximum-spool-usage-mb"))*100;
$np->add_perfdata(
    	label => "Spool usage",
    	value => sprintf("%.4f", $percent),
    	uom => "%",
    	warning => $np->opts->warning,
    	critical => $np->opts->critical  
		);

$np->add_perfdata(
    	label => "Spool usage",
    	value => $dom->findvalue("/rpc-reply/rpc/show/message-spool/message-vpn/vpn/current-messages-spooled"),
    	uom => "Msgs"
		);
		
$np->add_perfdata(
    	label => "Spool usage",
    	value => sprintf("%.1f",$dom->findvalue("/rpc-reply/rpc/show/message-spool/message-vpn/vpn/current-spool-usage-mb")),
    	uom => "MB"
		);
$np->add_perfdata(
    	label => "Transacted Sessions",
    	value => $dom->findvalue("/rpc-reply/rpc/show/message-spool/message-vpn/vpn/current-transacted-sessions")
		);	
	$np->add_perfdata(
    	label => "Transacted Msgs",
    	value => $dom->findvalue("/rpc-reply/rpc/show/message-spool/message-vpn/vpn/current-number-of-transacted-messages"),
    	uom => "Msgs"
		);		
		$np->add_perfdata(
    	label => "Ingress flows",
    	value => $dom->findvalue("/rpc-reply/rpc/show/message-spool/message-vpn/vpn/current-ingress-flows")
		);
		$np->add_perfdata(
    	label => "Egress flows",
    	value => $dom->findvalue("/rpc-reply/rpc/show/message-spool/message-vpn/vpn/current-egress-flows")
		);
		
$np->nagios_exit($np->check_threshold(
    	check => $percent,
    	warning => $np->opts->warning,
    	critical => $np->opts->critical                
		),"[VPN: ".$np->opts->vpn."]");
