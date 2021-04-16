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
use constant SEMP_USERNAME              => "SEMP_USERNAME";
use constant SEMP_PASSWORD              => "SEMP_PASSWORD";
use constant SEMP_VERSION               => "6_2";

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
use constant REPL_ASTATE_OK => "enabled";
use constant REPL_CSTATE_ACTIVE => "active";
use constant REPL_CSTATE_STANDBY => "standby";
use constant REPL_REMOTE_BRIDGE_OK => "up";
use constant REPL_LOCAL_BRIDGE_OK => REPL_REMOTE_BRIDGE_OK;
use constant REPL_QUEUE_BRIDGE_OK => "bound";
use constant RPC_ANSWER_OK => 'ok';
use constant RPC_ANSWER_FAIL => "fail";

my $Plug_VERSION = "0.0.1";
my $Plug_BLURB = "Nagios Plugin - Solace 3200 series Messaging Appliance";
my $Plug_URL = "http://www.solacesystems.com"; 
my $Plug_PLUGINNAME = basename $0;
my $code;
my $result;

# Set up new Nagios Plugin Object
my $np = Nagios::Plugin->new(
    usage => "Usage: %s  --routerName <router name> --routerMgmt <router mgmt ip> --credentialsDir <pathToDir> --vpn <vpn name> [-q|--quiet]",
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
    help => 'Vpn name',
    required => 1
);

$np->add_arg(
    spec => 'quiet|q',
    help => 'Allow to return Ok if router is not AD-Active',
    required => 0
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

my $rpccallShow = "<rpc semp-version=\"soltr/".SEMP_VERSION."\"><show><message-vpn><vpn-name>".$np->opts->vpn."</vpn-name><replication/></message-vpn></show></rpc>";
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

#Check that result code is OK
$result=$dom->findvalue('/rpc-reply/execute-result/@code');
if ($result ne RPC_ANSWER_OK){
        $code = UNKNOWN;
        my $message = "SEMP query succeded but result was FAIL";
        $np->nagios_exit($code,$message);

}
#Check that data exists
if(!($dom->findvalue("/rpc-reply/rpc/show/message-vpn/replication/message-vpns/message-vpn/admin-state"))){
	$np->nagios_exit(CRITICAL,"Unable to get replication state:Check if replication is configured and VPN:$vpn exists \n");
}

my @states;
my @messages;

#admin state
my $enState=OK;
my $en=$dom->findvalue("/rpc-reply/rpc/show/message-vpn/replication/message-vpns/message-vpn/admin-state");
if($en ne REPL_ASTATE_OK){
	$enState=CRITICAL;

	push @states,$enState;
	push @messages,"Admin State:".$en;
	$np->nagios_exit($enState,@messages);
}
else{
	#check that queue/bind-state doesn't exist (not primary replication and primary HA) AND 
	#local-bridge doesn't exist (not backup replication and primary HA)--> check if we are standby HA (either primary/stanby replication)
	if( (!$dom->findvalue("/rpc-reply/rpc/show/message-vpn/replication/message-vpns/message-vpn/queue/bind-state")) &&
	     !$dom->findvalue("/rpc-reply/rpc/show/message-vpn/replication/message-vpns/message-vpn/local-bridge/bridge-state")){
		if($np->opts->quiet){
			push @messages,$hostName ." is probably Standby Router";	
		}else{
			push @states,CRITICAL;
			push @messages,$hostName ." is probably Standby Router, peharps you want to use --quiet option?";
		}

	}else{
		#active
		if($dom->findvalue("/rpc-reply/rpc/show/message-vpn/replication/message-vpns/message-vpn/config-state") eq REPL_CSTATE_ACTIVE){
			#remote-bridge
			my $rbState=OK;
			my $rb=$dom->findvalue("/rpc-reply/rpc/show/message-vpn/replication/message-vpns/message-vpn/remote-bridge/connection-state");
			if($rb ne REPL_REMOTE_BRIDGE_OK){
				$rbState=CRITICAL;
			}
			push @states,$rbState;
			push @messages,"Remote Bridge State:".$rb;

			#queue
			my $qbState=OK;
			my $qb=$dom->findvalue("/rpc-reply/rpc/show/message-vpn/replication/message-vpns/message-vpn/queue/bind-state");
			if($qb ne REPL_QUEUE_BRIDGE_OK){
				$qbState=CRITICAL;
			}
			push @states,$qbState;
			push @messages,"Queue State:".$qb;
		}

		#standby
		if($dom->findvalue("/rpc-reply/rpc/show/message-vpn/replication/message-vpns/message-vpn/config-state") eq REPL_CSTATE_STANDBY){
			#local-bridge
			my $lbState=OK;
			my $lb=$dom->findvalue("/rpc-reply/rpc/show/message-vpn/replication/message-vpns/message-vpn/local-bridge/bridge-state");
			if($lb ne REPL_LOCAL_BRIDGE_OK){
				$lbState=CRITICAL;
			}
			push @states,$lbState;
			push @messages,"Local Bridge State:".$lb;
			}		
	}
	#sort to find max in @states
	@states = reverse sort { $a <=> $b } @states; 
	$np->nagios_exit($states[0],"[".join(",",@messages)."]");
}
