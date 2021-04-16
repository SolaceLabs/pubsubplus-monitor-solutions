#!/usr/bin/perl -w
# nagios: -epn
use strict;
use Nagios::Plugin;
use Nagios::Plugin::DieNicely;
use XML::LibXML;
use Data::Dumper;
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
use constant DISKASTATE_OK =>OPSTATE_OK;
use constant DISKSTATE_OK => "up";
use constant RAIDSTATE_OK => "in fully redundant state";
use constant RAIDRELOADSTATE_OK => "false";
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
    usage => "Usage: %s  --routerName <router name> --routerMgmt <router mgmt ip> --credentialsDir <pathToDir> -w|--warning <%% used> -c|--critical <%% used> -f|--filesystem <filesystem>",
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
    spec => 'filesystem|f=s',
    help => 'filesystem',
    required => 1
);

$np->add_arg(
    spec => 'warning|w=s',
    help => 'warning (%)',
    required => 1
);

$np->add_arg(
    spec => 'critical|c=s',
    help => 'critical (%)',
    required => 1
);
# get the command line options
$np->getopts;

my $hostIP              = $np->opts->routerMgmt;
my $hostName            = $np->opts->routerName;
my $credentialsDir      = $np->opts->credentialsDir;
my $filesystem		= $np->opts->filesystem;

unless (-d $credentialsDir) {
        $code = UNKNOWN;
        $result =  "Credentials directory $credentialsDir does not exist!";
        $np->nagios_exit($code, $result);
}
#get credentials from file

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

my $rpccallShow = "<rpc semp-version=\"soltr/".SEMP_VERSION."\"><show><disk><detail/></disk></show></rpc>";
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
#Check Disk

#disk1	
my $percent=$dom->findvalue("/rpc-reply/rpc/show/disk/disk-infos/disk-info/file-system[text()='".$filesystem."']/../use");
$percent =~ s/%//g;
if ($percent eq ""){
	$code = CRITICAL;
	$result = "unable to detect file system specified:$filesystem";
	$np->nagios_exit($code,$result);
}
else{
	my $blocksAvailable=$dom->findvalue("/rpc-reply/rpc/show/disk/disk-infos/disk-info/file-system[text()='".$np->opts->filesystem."']/../available");
	my $blocksUsed=$dom->findvalue("/rpc-reply/rpc/show/disk/disk-infos/disk-info/file-system[text()='".$np->opts->filesystem."']/../used");
	$code = $np->check_threshold(
	    check => $percent,
	    warning => $np->opts->warning,
	    critical => $np->opts->critical                
	);

	$np->add_perfdata(
	    label => "Used",
	    value => $percent,
	    uom => "%",
	    warning => $np->opts->warning,
	    critical => $np->opts->critical
	);
	$np->add_perfdata(
	    label => "Free",
	    value => int($blocksAvailable/1000),
	    uom => "MB",
	    warning => "",
	    critical => ""
	);

	$np->nagios_exit($code,"");
}
