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
use constant LUNSTATE_OK		=>FSTATE_OK;
use constant REDUNDANY_CONFIG_OK 	=> "Enabled";
use constant REDUNDANCY_MODE_AA 	=> "Active/Active";
use constant REDUNDANCY_MODE_AS		=> "Active/Standby";
use constant REDUNDANCY_MODE_NA		=> "N/A";
use constant REDUNDANY_ADBLINK_OK 	=> OPSTATE_OK;
use constant REDUNDANY_ADBHELLO_OK	=> OPSTATE_OK;
use constant RPC_ANSWER_OK		=> "ok";
use constant LOCAL_ACTIVE		=> "Local Active";
use constant MATE_ACTIVE		=> "Mate Active";
use constant HA_ACTIVE			=> "HA-Active";
use constant HA_STANDBY			=> "HA-Standby";
use constant HA_DOWN			=> "HA-Down";
use constant HA_UNKNOWN			=> "Unknown";
use constant SHUTDOWN			=> "Shutdown";
use constant ENABLED			=> "enabled";
use constant REDUNDANCY_STATE_OK	=> "Up";
use constant AD_ACTIVE			=> "AD-Active";
use constant AD_STANDBY			=> "AD-Standby";
use constant AD_DISABLED		=> "AD-Disabled";

my $Plug_VERSION = "0.0.1";
my $Plug_BLURB = "Nagios Plugin - Solace 3200 series Messaging Appliance";
my $Plug_URL = "http://www.solacesystems.com"; 
my $Plug_PLUGINNAME = basename $0;
my $code;
my $result;

# Set up new Nagios Plugin Object
my $np = Nagios::Plugin->new(
    usage => "Usage: %s  --routerName <router name> --routerMgmt <router mgmt ip> --credentialsDir <pathToDir>",
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
        $code = UNKNOWN;
	my $output = "Unable to read credentials for router:$hostName from directory:$credentialsDir";
        $np->nagios_exit($code,$result);
}

my $solUserName   = $routerConfigs->{SEMP_USERNAME};
my $solPasswd     = $routerConfigs->{SEMP_PASSWORD};
my $login         = $solUserName . ":". $solPasswd;

my $parser = XML::LibXML->new;

my $rpccallShow = "<rpc semp-version=\"soltr/".SEMP_VERSION."\"><show><redundancy><detail></detail></redundancy></show></rpc>";
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
        my $message  = "HTTP Error:SEMP query for redundancy failed to complete";
        $np->nagios_exit($code,$message);
}

my $domRed = $parser->parse_string($res->content);

if ( $domRed->findvalue('/rpc-reply/execute-result/@code') ne
        RPC_ANSWER_OK )
{
        $code = UNKNOWN;
        my $message = "SEMP query for redundancy succeded but result was FAIL";
        $np->nagios_exit($code,$message);

}


my @states;
my @messages;

#"config-status"
my $configStatusCode=OK;
my $configStatus=$domRed->findvalue("/rpc-reply/rpc/show/redundancy/config-status");
if($configStatus ne REDUNDANY_CONFIG_OK){
	$configStatusCode=CRITICAL;
}
push @states,$configStatusCode;
push @messages,"Config Status:".$configStatus;


#redundancy status
my $redStatusCode=OK;
my $redStatus=$domRed->findvalue("/rpc-reply/rpc/show/redundancy/redundancy-status");
if ($redStatus ne REDUNDANCY_STATE_OK){
	$redStatusCode=CRITICAL;
}
push @states,$redStatusCode;
push @messages,"Redundancy Status:".$redStatus;

#"mode"
my $redModeState;
my $redMode=$domRed->findvalue("/rpc-reply/rpc/show/redundancy/redundancy-mode");

push @messages,"Mode:".$redMode;

#Activity
my $primaryRouterActivity;
my $primaryRouterMsgSpoolStatus;

my $backupRouterActivity="";
my $backupRouterMsgSpoolStatus="";

my $activityState;
my $redState;

$primaryRouterActivity = $domRed->findvalue("/rpc-reply/rpc/show/redundancy/virtual-routers/primary/status/activity");
$backupRouterActivity  = $domRed->findvalue("/rpc-reply/rpc/show/redundancy/virtual-routers/backup/status/activity"); 

$primaryRouterMsgSpoolStatus=$domRed->findvalue("/rpc-reply/rpc/show/redundancy/virtual-routers/primary/status/detail/message-spool-status/internal/redundancy");
if ($redMode eq REDUNDANCY_MODE_AA){

	$backupRouterMsgSpoolStatus = $domRed->findvalue("/rpc-reply/rpc/show/redundancy/virtual-routers/backup/status/detail/message-spool-status/internal/redundancy");
}



if ($redMode eq REDUNDANCY_MODE_AA){
	if ($primaryRouterActivity eq LOCAL_ACTIVE && $backupRouterActivity eq MATE_ACTIVE){
		if ($primaryRouterMsgSpoolStatus eq AD_ACTIVE && $backupRouterMsgSpoolStatus eq AD_DISABLED){
			#router is HA-active
			$activityState=OK;
			$redState=HA_ACTIVE;
		}
		elsif ($primaryRouterMsgSpoolStatus eq AD_DISABLED && $backupRouterMsgSpoolStatus eq AD_STANDBY){
			#router is HA-standby
			$activityState=OK;
			$redState=HA_STANDBY;
		}
		elsif ($primaryRouterMsgSpoolStatus eq AD_DISABLED && $backupRouterMsgSpoolStatus eq AD_DISABLED){
			$activityState=OK;
			$redState=HA_UNKNOWN;
		}
		else{
			$activityState=WARNING;
			$redState=HA_UNKNOWN;
		}
 	}
 	elsif ($primaryRouterActivity eq MATE_ACTIVE && $backupRouterActivity eq MATE_ACTIVE ||
		$primaryRouterActivity eq LOCAL_ACTIVE && $backupRouterActivity eq LOCAL_ACTIVE){
		#HA DOWN
		$activityState=CRITICAL;
		$redState=HA_DOWN;
 	}

}elsif ($redMode eq REDUNDANCY_MODE_AS){

	if ($primaryRouterActivity eq LOCAL_ACTIVE){
		#router is HA-active
		$activityState=OK;
                $redState=HA_ACTIVE;
	}elsif ($backupRouterActivity eq MATE_ACTIVE){
		#router is HA-standby
		$activityState=OK;
                $redState=HA_STANDBY;
	}
		elsif ($primaryRouterActivity eq MATE_ACTIVE || $backupRouterActivity eq LOCAL_ACTIVE){
		#HA DOWN
		$activityState=CRITICAL;
                $redState=HA_DOWN;
	}

}

else{
	$activityState = CRITICAL;
	$redState      = HA_UNKNOWN;
}

push @states,$activityState;
push ( @messages,"Activity State:".$redState);
push ( @messages,"Primary Router:".$primaryRouterActivity);
#push ( @messages,"Spool:".$primaryRouterMsgSpoolStatus);
push ( @messages,"Backup Router:".$backupRouterActivity);
#push ( @messages,"Spool:".$backupRouterMsgSpoolStatus);

#"Mate"
push @messages,"Mate:".$domRed->findvalue("/rpc-reply/rpc/show/redundancy/mate-router-name");

#"ADB Link"
my $alState=OK;
my $al=$domRed->findvalue("/rpc-reply/rpc/show/redundancy/oper-status/adb-link-up");
if($al ne REDUNDANY_ADBLINK_OK){
	$alState=CRITICAL;
}
push @states,$alState;
push @messages,"ADB Link:".$al;

#"ADB Hello"
my $ahState=OK;
my $ah=$domRed->findvalue("/rpc-reply/rpc/show/redundancy/oper-status/adb-hello-up");
if($ah ne REDUNDANY_ADBHELLO_OK){
	$ahState=CRITICAL;
}
push @states,$ahState;
push @messages,"ADB Hello:".$ah;

#sort to find max in @states
@states = reverse sort { $a <=> $b } @states; 
$np->nagios_exit($states[0],"[".join(",",@messages)."]");
