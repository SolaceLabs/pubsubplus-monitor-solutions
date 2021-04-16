#!/usr/bin/perl -w
# nagios: -epn
use strict;
use Nagios::Plugin;
use Nagios::Plugin::DieNicely;
use XML::LibXML;
use Data::Dumper;
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
use constant DISKASTATE_OK =>OPSTATE_OK;
use constant DISKSTATE_OK => "up";
use constant RAIDSTATE_OK => "in fully redundant state";
use constant RAIDRELOADSTATE_OK => "false";

my $Plug_VERSION = "0.0.1";
my $Plug_BLURB = "Nagios Plugin - Solace 3200 series Messaging Appliance";
my $Plug_URL = "http://www.solacesystems.com"; 
my $Plug_PLUGINNAME = basename $0;


# Set up new Nagios Plugin Object
my $np = Nagios::Plugin->new(
    usage => "Usage: %s  -r|--router <router mgmt ip> -u|--user <username> -p|--pass <password> -w|--warning <%% used> -c|--critical <%% used> -f|--filesystem <filesystem>",
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

my $login = $np->opts->user . ":" . $np->opts->pass;
my $host = $np->opts->router;

my $parser = XML::LibXML->new;

my $rpccallShow = "<rpc semp-version=\"soltr/5_3\"><show><disk><detail/></disk></show></rpc>";
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
#Check Disk

#disk1	
my $percent=$dom->findvalue("/rpc-reply/rpc/show/disk/disk-infos/disk-info/file-system[text()='".$np->opts->filesystem."']/../use");
$percent =~ s/%//g;
my $blocksAvailable=$dom->findvalue("/rpc-reply/rpc/show/disk/disk-infos/disk-info/file-system[text()='".$np->opts->filesystem."']/../available");
my $blocksUsed=$dom->findvalue("/rpc-reply/rpc/show/disk/disk-infos/disk-info/file-system[text()='".$np->opts->filesystem."']/../used");
my $code = $np->check_threshold(
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
