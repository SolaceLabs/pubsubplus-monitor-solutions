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
use Data::Dumper;

use constant false => 0;
use constant true  => 1;
use constant SEMP_USERNAME              => "SEMP_USERNAME";
use constant SEMP_PASSWORD              => "SEMP_PASSWORD";
use constant SEMP_VERSION               => "6_2";
use constant OPSTATE_OK 		=> "true";
use constant ADBLINKSTATE_OK 		=> "Up";
use constant PMSTATE_OK 		=> "Ok";
use constant FSTATE_OK 			=>"Ready";
use constant FCSTATE_OK 		=>"Online";
use constant LUNSTATE_OK		=> FSTATE_OK;
use constant REDUNDANY_CONFIG_OK 	=> "Enabled";
use constant REDUNDANY_MODE_OK 		=> "Enabled";
use constant REDUNDANY_ADBLINK_OK 	=> OPSTATE_OK;
use constant REDUNDANY_ADBHELLO_OK 	=> OPSTATE_OK;
use constant IFACE_ENABLED_OK 		=> "yes";
use constant IFACE_LINK_OK 		=> IFACE_ENABLED_OK;
use constant MSGSPOOLOPSTATE_ADACTIVE 	=> "AD-Active";
use constant MSGSPOOLOPSTATE_ADSTANDBY 	=> "AD-Standby";
use constant MSGSPOOLENABLED 		=> "Enabled";
use constant RPC_ANSWER_OK		=> "ok";

my $Plug_VERSION = "0.0.1";
my $Plug_BLURB = "Nagios Plugin - Solace 3200 series Messaging Appliance";
my $Plug_URL = "http://www.solacesystems.com"; 
my $Plug_PLUGINNAME = basename $0;
my $code;
my $result;

# Net up new Nagios Plugin Object
my $np = Nagios::Plugin->new(
    usage => "Usage: %s  --routerName <router name> --routerMgmt <router mgmt ip> --warnUsage <usage percent> --critUsage <usage percent>--warnTransSess <usage percent> --ct|--critTransSess <usage percent>] --credentialsDir <path to dir>
     [--waf|--warnAdbFs <usage (MB)> --caf|--critAdbFs <usage (MB)>] [--wam|--warnAdbMsg <usage (Msg)> --cam|--critAdbMsg <usage (Msg)>]
     [--wdf|--warnDiskFs <usage (MB)> --cdf|--critDiskFs <usage (MB)>] [--wdm|--warnDiskMsg <usage (Msg)> --cdm|--critDiskMsg <usage (Msg)>]
     [--wtf|--warnTotalFs <usage (MB)> --ctf|--critTotalFs <usage (MB)>] [--wtm|--warnTotalMsg <usage (Msg)> --ctm|--critTotalMsg <usage (Msg)>]
     [-q|--quiet]",
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
    spec => 'warnUsage=s',
    help => 'warning threshold for Message spool usage (%)',
    required => 1
);

$np->add_arg(
    spec => 'critUsage=s',
    help => 'critical threshold for Message spool usage (%)',
    required => 1
);

$np->add_arg(
    spec => 'warnTransSess|wt=s',
    help => 'warning threshold for transacted sessions (%)',
    required => 0
);

$np->add_arg(
    spec => 'critTransSess|ct=s',
    help => 'critical threshold for transacted sessions (%)',
    required => 0
);

$np->add_arg(
    spec => 'warnAdbFs|waf=s',
    help => 'warning threshold for Adb fs (MB)',
    required => 0
);

$np->add_arg(
    spec => 'critAdbFs|caf=s',
    help => 'critical threshold for Adb fs (MB)',
    required => 0
);

$np->add_arg(
    spec => 'warnAdbMsg|wam=s',
    help => 'warning threshold for Adb spooled Msgs (Msgs)',
    required => 0
);

$np->add_arg(
    spec => 'critAdbMsg|cam=s',
    help => 'critical threshold for Adb spooled Msgs (Msgs)',
    required => 0
);

$np->add_arg(
    spec => 'warnDiskFs|wdf=s',
    help => 'warning threshold for Disk fs (MB)',
    required => 0
);

$np->add_arg(
    spec => 'critDiskFs|cdf=s',
    help => 'critical threshold for Disk fs (MB)',
    required => 0
);

$np->add_arg(
    spec => 'warnDiskMsg|wdm=s',
    help => 'warning threshold for Disk spooled Msgs (Msgs)',
    required => 0
);

$np->add_arg(
    spec => 'critDiskMsg|cdm=s',
    help => 'critical threshold for Disk spooled Msgs (Msgs)',
    required => 0
);

$np->add_arg(
    spec => 'warnTotalFs|wtf=s',
    help => 'warning threshold for Total fs (MB)',
    required => 0
);

$np->add_arg(
    spec => 'critTotalFs|ctf=s',
    help => 'critical threshold for Total fs (MB)',
    required => 0
);

$np->add_arg(
    spec => 'warnTotalMsg|wtm=s',
    help => 'warning threshold for Total spooled Msgs (Msgs)',
    required => 0
);

$np->add_arg(
    spec => 'critTotalMsg|ctm=s',
    help => 'critical threshold for Total spooled Msgs (Msgs)',
    required => 0
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

my $warnUsage     = $np->opts->warnUsage;
my $critUsage	  = $np->opts->critUsage;

my $parser = XML::LibXML->new;

my $rpccallShow = "<rpc semp-version=\"soltr/".SEMP_VERSION."\"><show><message-spool><detail/></message-spool></show></rpc>";
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

#print $dom->toString();
#print "\n";

# check config status
# use a regex var as we canot directly refer to a constant
my $regex = qr/${ \(MSGSPOOLENABLED)} /;
if (! $dom->findvalue("/rpc-reply/rpc/show/message-spool/message-spool-info/config-status") =~ $regex){
	$code = CRITICAL;
	$result = "Message spool is shutdown!";
	$np->nagios_exit($code,$result);
}
my $maxDiskUsage = $dom->findvalue("/rpc-reply/rpc/show/message-spool/message-spool-info/max-disk-usage");

if ( $maxDiskUsage eq 0 ) {
	$code = CRITICAL;
        $result = "Message spool disk-usage not configured!";
        $np->nagios_exit($code,$result);
}

if ($dom->findvalue("/rpc-reply/rpc/show/message-spool/message-spool-info/operational-status") eq MSGSPOOLOPSTATE_ADSTANDBY){
	if ($np->opts->quiet){
			$np->nagios_exit(OK,"$hostName is ".MSGSPOOLOPSTATE_ADSTANDBY);
	}else{
			$np->nagios_exit(CRITICAL,"$hostName is ".MSGSPOOLOPSTATE_ADSTANDBY." use --quiet option to get rid of the error");
	}
}
my @states;
my @messages;

#"Transacted Sessions"
if(defined($np->opts->warnTransSess) && defined($np->opts->critTransSess)){
		push @states,$np->check_threshold(
    	check => $dom->findvalue("/rpc-reply/rpc/show/message-spool/message-spool-info/transacted-session-count-utilization-percentage"),
    	warning => $np->opts->warnTransSess,
    	critical => $np->opts->critTransSess                
		);
		$np->add_perfdata(
    	label => "Transacted Session Usage",
    	value => $dom->findvalue("/rpc-reply/rpc/show/message-spool/message-spool-info/transacted-session-count-utilization-percentage"),
    	uom => "%",
    	warning => $np->opts->warnTransSess,
    	critical => $np->opts->critTransSess  
		);
		$np->add_perfdata(
    	label => "Transacted Session",
    	value => $dom->findvalue("/rpc-reply/rpc/show/message-spool/message-spool-info/transacted-sessions-used")
		);
}
#"ADB MB"
if(defined($np->opts->warnAdbFs) && defined($np->opts->critAdbFs)){
		push @states,$np->check_threshold(
    	check => $dom->findvalue("/rpc-reply/rpc/show/message-spool/message-spool-info/current-rfad-usage"),
    	warning => $np->opts->warnAdbFs,
    	critical => $np->opts->critAdbFs                
		);
		$np->add_perfdata(
    	label => "ADB Usage",
    	value => $dom->findvalue("/rpc-reply/rpc/show/message-spool/message-spool-info/current-rfad-usage"),
    	uom => "MB",
    	warning => $np->opts->warnAdbFs,
    	critical => $np->opts->critAdbFs  
		);
}
#"Disk MB"
if(defined($np->opts->warnDiskFs) && defined($np->opts->critDiskFs)){
		push @states,$np->check_threshold(
    	check => $dom->findvalue("/rpc-reply/rpc/show/message-spool/message-spool-info/current-disk-usage"),
    	warning => $np->opts->warnDiskFs,
    	critical => $np->opts->critDiskFs                
		);
		$np->add_perfdata(
    	label => "Disk Usage",
    	value => $dom->findvalue("/rpc-reply/rpc/show/message-spool/message-spool-info/current-disk-usage"),
    	uom => "MB",
    	warning => $np->opts->warnDiskFs,
    	critical => $np->opts->critDiskFs  
		);
}
#"Total MB"
if(defined($np->opts->warnTotalFs) && defined($np->opts->critTotalFs)){
		push @states,$np->check_threshold(
    	check => $dom->findvalue("/rpc-reply/rpc/show/message-spool/message-spool-info/current-persist-usage"),
    	warning => $np->opts->warnTotalFs,
    	critical => $np->opts->critTotalFs                
		);
		$np->add_perfdata(
    	label => "Total Usage",
    	value => $dom->findvalue("/rpc-reply/rpc/show/message-spool/message-spool-info/current-persist-usage"),
    	uom => "MB",
    	warning => $np->opts->warnTotalFs,
    	critical => $np->opts->critTotalFs  
		);
}

 my $currDiskUsage = $dom->findvalue("/rpc-reply/rpc/show/message-spool/message-spool-info/current-disk-usage");
 my $percentUsage    = sprintf("%.2f", $currDiskUsage/$maxDiskUsage)*100;

 $np->add_perfdata(
        label => "Total Disk Usage(%)",
        value => $percentUsage,
        uom => "%",
        warning => $np->opts->warnUsage,
        critical => $np->opts->critUsage
                );


#"ADB Msgs"
if(defined($np->opts->warnAdbMsg) && defined($np->opts->critAdbMsg)){
		push @states,$np->check_threshold(
    	check => $dom->findvalue("/rpc-reply/rpc/show/message-spool/message-spool-info/rfad-messages-currently-spooled"),
    	warning => $np->opts->warnAdbMsg,
    	critical => $np->opts->critAdbMsg                
		);
		$np->add_perfdata(
    	label => "ADB Usage",
    	value => $dom->findvalue("/rpc-reply/rpc/show/message-spool/message-spool-info/rfad-messages-currently-spooled"),
    	uom => "Msgs",
    	warning => $np->opts->warnAdbMsg,
    	critical => $np->opts->critAdbMsg  
		);
}
#"Disk Msgs"
if(defined($np->opts->warnDiskMsg) && defined($np->opts->critDiskMsg)){
		push @states,$np->check_threshold(
    	check => $dom->findvalue("/rpc-reply/rpc/show/message-spool/message-spool-info/disk-messages-currently-spooled"),
    	warning => $np->opts->warnDiskMsg,
    	critical => $np->opts->critDiskMsg                
		);
		$np->add_perfdata(
    	label => "Disk Usage",
    	value => $dom->findvalue("/rpc-reply/rpc/show/message-spool/message-spool-info/disk-messages-currently-spooled"),
    	uom => "Msgs",
    	warning => $np->opts->warnDiskMsg,
    	critical => $np->opts->critDiskMsg  
		);
}
#"Total Msgs"
if(defined($np->opts->warnTotalMsg) && defined($np->opts->critTotalMsg)){
		push @states,$np->check_threshold(
    	check => $dom->findvalue("/rpc-reply/rpc/show/message-spool/message-spool-info/total-messages-currently-spooled"),
    	warning => $np->opts->warnTotalMsg,
    	critical => $np->opts->critTotalMsg                
		);
		$np->add_perfdata(
    	label => "Total Usage",
    	value => $dom->findvalue("/rpc-reply/rpc/show/message-spool/message-spool-info/total-messages-currently-spooled"),
    	uom => "Msgs",
    	warning => $np->opts->warnTotalMsg,
    	critical => $np->opts->critTotalMsg  
		);
}
#sort to find max in @states
@states = reverse sort { $a <=> $b } @states; 

if (scalar @states == 0){
	$code = OK;
	$np->nagios_exit($code,"");
}
else{
	$np->nagios_exit($states[0],"");
}
