#!/usr/bin/perl -w
package Utils;

use strict;
use warnings;
use Exporter;
use Config::Std;
use Data::Dumper;

##
#these should be in the same format as in the config file 
#being read
#
use constant ROUTER 		=> "ROUTER";
use constant VPN    		=> "VPN";
use constant NAGIOS_SERVER 	=> "NAGIOS_SERVER";
use constant TYPE		=> "TYPE";

our @ISA= qw( Exporter );

# these CAN be exported.
#our @EXPORT_OK = qw( export_me export_me_too );

# these are exported by default.
our @EXPORT = qw( getSudoConfigs
		  getRouterConfigs
		  getVPNConfigs
                  unique
		);

#################################
# Reads the config directory
# If it finds a config file for a nagios host
# It returns all the elements
# ################################
sub getSudoConfigs {
	my $cfgDir = shift or die "Unable to get Cfg Dir";
	foreach my $fp (glob("$cfgDir/*")) {
		#print "Reading file:$fp\n\n";
		my $filename = $fp;
		my %cfg;
		read_config($filename => %cfg);
        	foreach my $key  (keys%cfg) {
              		my $type = $cfg{$key}{TYPE};
                        if ($type eq NAGIOS_SERVER ){
                		return ($cfg{$key});
			}
		}
         }
         return 0;
}
     		
################################
# Reads the config directory
# If it finds a config file with
# the router name supplied
# It returns all the elements
# ################################
sub getRouterConfigs{
	 my $cfgDir = shift or die "Unable to get Cfg Dir";
	 my $routerName = shift or die "Unable to get router name";
         foreach my $fp (glob("$cfgDir/*")) {
               	my $filename = $fp;
                my %cfg;
                read_config($filename => %cfg);
                foreach my $key  (keys%cfg) { 
			my $type = $cfg{$key}{TYPE};     
         		if ($type eq ROUTER ){
                        	my $name = $key;
				if ($name eq $routerName){
					return ($cfg{$key});
				}
			}
		}
	}
	return 0;
}

###############################
# Reads the config directory
# If it finds a config file for 
# the router and vpn name supplied
# It returns all the elements
# ################################
sub getVPNConfigs{
         my $cfgDir = shift or die "Unable to get Cfg Dir";
         my $routerName = shift or die "Unable to get router name";
	 my $vpnName = shift or die "Unable to get VPN name";
         foreach my $fp (glob("$cfgDir/*")) {
                my $filename = $fp;
                my %cfg;
                read_config($filename => %cfg);
		#first get the router name
		#we need to interate through the hash as it can be anywhere
		my $thisRouterName;
		my $thisVPNName;
	
                foreach my $key  (keys%cfg) {
                        my $type = $cfg{$key}{TYPE};
                        if ($type eq ROUTER ){
                                $thisRouterName = $key;
			}
		}
		#if we have not got a router then the config file is malformed
		if(!$thisRouterName){
			return 0;
		}
		#now check the VPN names
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

########################
##		       #
## returns unique vals #
## in an array	       #
##		       #
#########################

sub unique {
    my %seen = ();
    my @uniqVals = ();
    foreach my $item (@_) {
        unless ($seen{$item}) {
            push @uniqVals, $item;
            $seen{$item} = 1;
        }
    }
    return @uniqVals;
}



1
