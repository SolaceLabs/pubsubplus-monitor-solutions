#!/usr/bin/perl

####################################################
# This script starts an admin listener process	   #
# for each VPN config file found in the VPN config #
# directory.					   #
# Params:					   #
# 	-Path to admin listnener binary		   #
#	-Path to directory containing VPN configs  #
#	-Path to credentials directory		   #
####################################################

use warnings;

use Data::Dumper;
use File::Slurp;
use Config::Std;

use constant ROUTERNAME		=> "router_name";
use constant ROUTERIP		=> "msgBackBoneIP";
use constant HOST_NAME		=> "host_name";
use constant VPN		=> "VPN";
use constant CLIENT_USERNAME	=> "CLIENT_USERNAME";
use constant CLIENT_PASSWORD	=> "CLIENT_PASSWORD";

 #check usage
 if ( scalar @ARGV < 3) {
	print_usage();
	exit(0);
 }

 my $ADMIN_LISTENER = shift or die ("unable to get path to admin listener!");
 my $vpnCfgFile	   = shift or die ("unable to get path to VPN config file!");
 my $credentialsDir = shift or die ("unable to get path to directory with router credentials!");

 # for each cfg file in the directory
 # read the directory to get the ip, hostname, vpn name 
 # read the credentials file to get the password

      
 my @vpnFile = read_file($vpnCfgFile);
 my $routerName;
 my $vpn;
 my $routerIP;
 #scan the vpn file, get the message backbone IP
 foreach my $line(@vpnFile){
		my $regex;
		# put the regex in a variable as we cannot refer to 
		# a constant in the regex
		# rewgex: get all the characters matching the constant
		# and before the ; character
		$regex = qr/${\(ROUTERIP)}(.*?);/;
		if ($line =~ $regex ) {
                       	$routerIP = trim($1);
               	}
		$regex = qr/${\(HOST_NAME)}(.*?);/;
		if ($line =~ $regex ) {
                       	$vpn = trim($1);
		}
		$regex = qr/${\(ROUTERNAME)}(.*?);/;
		if ($line =~ $regex ) {
                       	$routerName = trim($1);
               	}
}
# now that we have the Router and VPN name, get the username and password
# for the message backbone from the config dir
my $vpnConfigs	= getVPNConfigs($credentialsDir, $routerName, $vpn);
my $solUser 	= trim($vpnConfigs->{CLIENT_USERNAME});
my $solPass	= trim($vpnConfigs->{CLIENT_PASSWORD});

my $cu = $solUser."@".$vpn;

#finally call the admin listener binary
my $res = "$ADMIN_LISTENER --cip=$routerIP --cu=$cu --cp=$solPass";
print ("--Starting admin listener for VPN: $vpn --\n");
#print "$res\n";
system("nohup $res &");
 

###TRIM###
sub  trim { my $s = shift; $s =~ s/^\s+|\s+$//g; return $s };

 ###USAGE###
 sub print_usage{

	print "\nUsage: /runAdminListener.pl <path to admin listener binary> <path to VPN config file> <path to directory with credentials>\n\n";
        return 0;
}

####################################
# Reads the  config directory
# If it finds a config file for 
# the router and vpn name supplied
# It returns all the elements
###################################

sub getVPNConfigs{
         my $cfgDir = shift or die "Unable to get Cfg Dir";
         my $routerName = shift or die "Unable to get router name";
         my $vpnName = shift or die "Unable to get VPN name";
         foreach my $fp (glob("$cfgDir/*")) {
                my $filename = $fp;
                my %cfg;
                read_config($filename => %cfg);
		# first get the router name
		# need to iterate through the has as it can be anywhere
		my $thisRouterName;
                my $thisVPNName;

                foreach my $key  (keys%cfg) {
                        my $type = $cfg{$key}{TYPE};
                        if ($type eq ROUTER ){
                                $thisRouterName = $key;
                        }
                }
		# if we do not have a router name the config file is malformed
		if(!$thisRouterName){
                        return 0;
                }
		# now check the VPN names
		foreach my $key  (keys%cfg) {
                        my $type = $cfg{$key}{TYPE};
                        if ($type eq VPN ){
                                $thisVPNName = $key;
                                if ( $thisRouterName eq $routerName && $thisVPNName eq $vpnName){
                                        return ($cfg{$key});
                                }
                        }
                }
          }
        return 0;
}
