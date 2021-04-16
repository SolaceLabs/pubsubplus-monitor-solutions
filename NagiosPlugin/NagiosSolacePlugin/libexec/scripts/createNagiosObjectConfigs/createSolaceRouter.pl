#!/usr/bin/perl -w

use strict;
use FindBin;
use lib "$FindBin::Bin/common";
use Utilities;
use Configs;
use Data::Dumper qw(Dumper);
use File::Copy;
use Cwd;
use POSIX qw(strftime);

use constant IFACE_MGMT		=> "eth1";
use constant IFACE_MSGB		=> "1/1/lag1";
use constant PARTITION_FS	=> "/dev/md2";

main();

sub main {
 #this will contain all the input values we capture
 my $solaceRouter;
 
 #grab inputs
 print ("\n");

 $solaceRouter->{ $tokens->{HOST_NAME} }		= askQuestion("Enter the router name:","",'AN',"","",128);
 $solaceRouter->{ $tokens->{HOST_ALIAS} }	 	= "Solace Router : ".$solaceRouter->{ $tokens->{HOST_NAME} }; 
 $solaceRouter->{ $tokens->{MGMT_ADDRESS} }		= askQuestion("Enter the management IP address in dotted decimal format (xx.xx.xx.xx):",
									"",'AN',"","",16);

 my $port						= askQuestion("Enter the port number for the management IP:","80",'N',1,55555,"");
 $solaceRouter->{ $tokens->{ADDRESS_PORT} }		= join(':',$solaceRouter->{ $tokens->{MGMT_ADDRESS} },$port);
 
 $solaceRouter->{ $tokens->{INTERFACE_MGMT} }		= askQuestion("Enter the name of the management interface to be monitored:",
									IFACE_MGMT,'AN',"","",128);
 
 $solaceRouter->{ $tokens->{INTERFACE_MSGB} }		= askQuestion("Enter the name of the message-backbone interface to be monitored:",
									IFACE_MSGB,'AN',"","",128);
 
 $solaceRouter->{ $tokens->{PARTITION_FS} }		= askQuestion("Enter the name of the file system partition to be monitored:",
									PARTITION_FS,'AN',"","",128);
 
 $solaceRouter->{ $tokens->{HOSTGROUP_NAME} }		= askQuestion("Enter the name of the hostgroup for this router:",
								$solaceRouter->{ $tokens->{HOST_NAME} },'AN',"","",128);

 $solaceRouter->{ $tokens->{HOSTGROUP_ALIAS} }		= $solaceRouter->{ $tokens->{HOSTGROUP_NAME} };
 
 $solaceRouter->{ $tokens->{HOSTGROUP_MEMBERS} }	= askQuestion("Enter the Nagios hostnames(display names) of VPNs to be monitored in this router(comma separated):",
								"",'AN',"","",1024);

 $solaceRouter->{ $tokens->{HOSTDEP_NAME} }		= $solaceRouter->{ $tokens->{HOSTGROUP_NAME} };
 
 #print Dumper($solaceRouter);

 my $outfileName 		= join('/',$dirs->{OUTPUTS},$solaceRouter->{ $tokens->{HOST_NAME} }.".cfg");

 my $templateFile		= $templates->{SOLACE_ROUTER};
 
 copyTemplate($templateFile,$outfileName);

 my $key;
 for $key ( keys %{$solaceRouter}) {
 	substitute($key,$solaceRouter->{$key},$outfileName);
 }

 
 print ("\nThe Object configuration file for the Solace router has been created at: $outfileName\n\n");

}
