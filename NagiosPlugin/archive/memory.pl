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

my $Plug_VERSION = "0.0.1";
my $Plug_BLURB = "Nagios Plugin - Solace 3200 series Messaging Appliance";
my $Plug_URL = "http://www.solacesystems.com"; 
my $Plug_PLUGINNAME = basename $0;


# Set up new Nagios Plugin Object
my $np = Nagios::Plugin->new(
    usage => "Usage: %s  -r|--router <router mgmt ip> -u|--user <username> -p|--pass <password> -w|--warning <warning usage threshold (%)> -c|--critical <critical usage threshold (%)>",
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
    help => 'Warning usage threshold %',
    required => 1
);

$np->add_arg(
    spec => 'critical|c=s',
    help => 'Critical usage threshold %',
    required => 1
);

# get the command line options
$np->getopts;

my $login = $np->opts->user . ":" . $np->opts->pass;
my $host = $np->opts->router;

my $parser = XML::LibXML->new;

my $rpccallShow = "<rpc semp-version=\"soltr/5_3\"><show><memory></memory></show></rpc>";

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
my @states;

#physical
push @states,$np->check_threshold(
    check => $dom->findvalue("/rpc-reply/rpc/show/memory/physical-memory-usage-percent"),
    warning => $np->opts->warning,
    critical => $np->opts->critical                
);

$np->add_perfdata(
    label => "physical-mem",
    value => $dom->findvalue("/rpc-reply/rpc/show/memory/physical-memory-usage-percent"),
    uom => "%",
    warning => $np->opts->warning,
    critical => $np->opts->critical
);

#sub
push @states,$np->check_threshold(
    check => $dom->findvalue("/rpc-reply/rpc/show/memory/subscription-memory-usage-percent"),
    warning => $np->opts->warning,
    critical => $np->opts->critical                
);

$np->add_perfdata(
    label => "subscriptions-mem",
    value => $dom->findvalue("/rpc-reply/rpc/show/memory/subscription-memory-usage-percent"),
    uom => "%",
    warning => $np->opts->warning,
    critical => $np->opts->critical
);

#NAB
push @states,$np->check_threshold(
    check => $dom->findvalue("/rpc-reply/rpc/show/memory/slot-infos/slot-info/nab-buffer-load-factor"),
    warning => $np->opts->warning,
    critical => $np->opts->critical                
);

$np->add_perfdata(
    label => "NAB-load-factor",
    value => $dom->findvalue("/rpc-reply/rpc/show/memory/slot-infos/slot-info/nab-buffer-load-factor"),
    uom => "%",
    warning => $np->opts->warning,
    critical => $np->opts->critical
);

#sort to find max in @states
@states = reverse sort { $a <=> $b } @states; 
$np->nagios_exit($states[0],"");
