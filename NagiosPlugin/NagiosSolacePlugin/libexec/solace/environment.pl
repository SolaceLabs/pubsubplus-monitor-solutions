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
use constant RPC_ANSWER_OK		=> "ok";

my $Plug_VERSION = "0.0.1";
my $Plug_BLURB = "Nagios Plugin - Solace 3200 series Messaging Appliance";
my $Plug_URL = "http://www.solacesystems.com"; 
my $Plug_PLUGINNAME = basename $0;
my $code;
my $result;

# Set up new Nagios Plugin Object
my $np = Nagios::Plugin->new(
    usage => "Usage: %s  --routerName <router Name> --routerMgmt <router mgmt ip> --wf|--warnFan <rpm> --cf|--critFan <rpm> --wc|--warnChassis <C> --cc|--critChassis <C> --wn|--warnNpu <C> --cn|--critNpu <C> --credentialsDir <pathToDir>",
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
    spec => 'warnFan|wf=s',
    help => 'warning FAN speed (rpm)',
    required => 1
);

$np->add_arg(
    spec => 'critFan|cf=s',
    help => 'critical FAN speed(rpm)',
    required => 1
);
$np->add_arg(
    spec => 'warnChassis|wc=s',
    help => 'warning Chassis Temp (C)',
    required => 1
);

$np->add_arg(
    spec => 'critChassis|cc=s',
    help => 'critical Chassis Temp (C)',
    required => 1
);
$np->add_arg(
    spec => 'warnNpu|wn=s',
    help => 'warning Npu Temp (C)',
    required => 1
);

$np->add_arg(
    spec => 'critNpu|cn=s',
    help => 'critical Npu Temp (C)',
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
	$code   = UNKNOWN;
        $result =  "Unable to read credentials for router:$hostName from directory:$credentialsDir";
        $np->nagios_exit($code,$result);
}

my $solUserName   = $routerConfigs->{SEMP_USERNAME};
my $solPasswd     = $routerConfigs->{SEMP_PASSWORD};
my $login         = $solUserName . ":". $solPasswd;

my $parser = XML::LibXML->new;

my $rpccallShow = "<rpc semp-version=\"soltr/".SEMP_VERSION."\"><show><environment></environment></show></rpc>";
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

my @states;
my $fan;

#Fan speeds
#Chassis temperature
#NPU temperature
my @fans=$dom->findnodes("/rpc-reply/rpc/show/environment/mainboard/sensors/sensor/type[text()='Fan speed']/..");
foreach $fan (@fans) {
push @states,$np->check_threshold(
    check => $fan->findvalue("value"),
    warning => $np->opts->warnFan,
    critical => $np->opts->critFan                
);

$np->add_perfdata(
    label => $fan->findvalue("name"),
    value => $fan->findvalue("value"),
    uom => $fan->findvalue("unit"),
    warning => $np->opts->warnFan,
    critical => $np->opts->critFan
);
 };
 
 push @states,$np->check_threshold(
    check => $dom->findvalue("/rpc-reply/rpc/show/environment/mainboard/sensors/sensor/name[text()='Chassis Temp.']/../value"),
    warning => $np->opts->warnChassis,
    critical => $np->opts->critChassis                
);

$np->add_perfdata(
    label => 'Chassis Temp.',
    value => $dom->findvalue("/rpc-reply/rpc/show/environment/mainboard/sensors/sensor/name[text()='Chassis Temp.']/../value"),
    uom => $dom->findvalue("/rpc-reply/rpc/show/environment/mainboard/sensors/sensor/name[text()='Chassis Temp.']/../unit"),
    warning => $np->opts->warnChassis,
    critical => $np->opts->critChassis
);

 push @states,$np->check_threshold(
    check => $dom->findvalue("/rpc-reply/rpc/show/environment/slots/slot/card-type[text()='Network Acceleration Blade']/../sensors/sensor/name[text()='NPU Core Temp']/../value"),
    warning => $np->opts->warnNpu,
    critical => $np->opts->critNpu                
);

$np->add_perfdata(
    label => 'NPU Core Temp',
    value => $dom->findvalue("/rpc-reply/rpc/show/environment/slots/slot/card-type[text()='Network Acceleration Blade']/../sensors/sensor/name[text()='NPU Core Temp']/../value"),
    uom => $dom->findvalue("/rpc-reply/rpc/show/environment/slots/slot/card-type[text()='Network Acceleration Blade']/../sensors/sensor/name[text()='NPU Core Temp']/../unit"),
    warning => $np->opts->warnNpu,
    critical => $np->opts->critNpu
);
#sort to find max in @states
@states = reverse sort { $a <=> $b } @states; 
$np->nagios_exit($states[0],"");
