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
use constant SEMP_USERNAME              => "SEMP_USERNAME";
use constant SEMP_PASSWORD              => "SEMP_PASSWORD";
use constant SEMP_VERSION               => "6_2";
use constant RPC_ANSWER_OK		=> "ok";

my $Plug_VERSION = "0.0.1";
my $Plug_BLURB = "Nagios Plugin - Solace 3200 series Messaging Appliance";
my $Plug_URL = "http://www.solacesystems.com"; 
my $Plug_PLUGINNAME = basename $0;
my $code;
my $result;


# Set up new Nagios Plugin Object
my $np = Nagios::Plugin->new(
    usage => "Usage: %s  --routerName <router name> --routerMgmt <router mgmt ip> -i|--iface <interface> [--wa|--warnAvail <number of members> --ca|--critAvail <number of members>] [--wo|--warnOper <number of members> --co|--critOper <number of members>] --credentialsDir <path to dir>",
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
    spec => 'iface|i=s',
    help => 'Interface (Fabric/slot/instance)',
    required => 1
);

$np->add_arg(
    spec => 'warnAvail|wa=s',
    help => 'warning threshold for available members',
    required => 0
);

$np->add_arg(
    spec => 'critAvail|ca=s',
    help => 'critical threshold for available members',
    required => 0
);

$np->add_arg(
    spec => 'warnOper|wo=s',
    help => 'warning threshold for operationnal members',
    required => 0
);

$np->add_arg(
    spec => 'critOper|co=s',
    help => 'critical threshold for operationnal members',
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
my $ifaceName		= $np->opts->iface;

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

my $rpccallShow = "<rpc semp-version=\"soltr/".SEMP_VERSION."\"><show><interface><phy-interface>".$np->opts->iface."</phy-interface></interface></show></rpc>";
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
        $code = UNKNOWN;
        my $message = "SEMP query succeded but result was FAIL";
        $np->nagios_exit($code,$message);

}

my $interface = $dom->findvalue("/rpc-reply/rpc/show/interface/interfaces/interface/phy-interface");
if ($interface eq ""){
	$code = CRITICAL;
	$result = "Interface $ifaceName does not exist";
	$np->nagios_exit($code,$result);
 }
 else{
	my @states;
	my @messages;

	push @messages,"Interface:".$np->opts->iface;

	#"enabled"
	my $enState=OK;
	my $en=$dom->findvalue("/rpc-reply/rpc/show/interface/interfaces/interface/enabled");
	if($en ne IFACE_ENABLED_OK){
		$enState=CRITICAL;
	}
	push @states,$enState;
	push @messages,"Enabled:".$en;

	#"stats"
	my $stat;
	my @stats=$dom->findnodes("/rpc-reply/rpc/show/interface/interfaces/interface/stats/*");
	foreach $stat (@stats) {
		$np->add_perfdata(
	    label => $stat->nodeName,
	    value => $stat->textContent
	);
	}

	#physical
	#We don't use exists to keep compatibility with earlier version of LibXML
	if($dom->findnodes("/rpc-reply/rpc/show/interface/interfaces/interface/eth")){
		#link
		my $liState=OK;
		my $li=$dom->findvalue("/rpc-reply/rpc/show/interface/interfaces/interface/eth/link-detected");
		if($li ne IFACE_LINK_OK){
			$liState=CRITICAL;
		}
		push @states,$liState;
		push @messages,"Link:".$li;
	}

	#lag
	if($dom->findnodes("/rpc-reply/rpc/show/interface/interfaces/interface/lag")){
		#available
		if(defined($np->opts->critAvail) && defined($np->opts->warnAvail)){
			my @avail=$dom->findnodes("/rpc-reply/rpc/show/interface/interfaces/interface/lag/available-members/member");
			
			my $warning = $np->opts->warnAvail;
			my $critical = $np->opts->critAvail;

			my $availMembers = $np->check_threshold(
                				check => scalar(grep {defined $_} @avail),
                				warning => $warning,
                				critical => $critical
                        			); 

			push @states,$availMembers;
		
			push @messages,"Available members:".scalar(grep {defined $_} @avail);
		}
		#operationnal
		if(defined($np->opts->critOper) && defined($np->opts->warnOper)){
			my @oper=$dom->findnodes("/rpc-reply/rpc/show/interface/interfaces/interface/lag/operational-members/member");
			
			my $warning = $np->opts->warnOper;
                        my $critical = $np->opts->critOper;
			
			my $critMembers = $np->check_threshold(
		                			check => scalar(grep {defined $_} @oper),
               						warning => $warning,
                					critical => $critical
							);
			push @states,$critMembers;

			push @messages,"Operational members:".scalar(grep {defined $_} @oper);
		}
	}

	#sort to find max in @states
	@states = reverse sort { $a <=> $b } @states; 
	$np->nagios_exit($states[0],"[".join(",",@messages)."]");
	}
