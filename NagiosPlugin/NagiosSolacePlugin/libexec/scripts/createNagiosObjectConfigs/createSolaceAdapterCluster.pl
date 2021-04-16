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

main();

sub main {
 #this will contain all the input values we capture
 my $solaceAdapterCluster;
 
 print "\n---------------------------------------------------------------------------------------------------------------------------\n";
 print "NOTE:Ensure that all members of this cluster have been created prior to running this script. If not,exit and create them!\n";
 print "-----------------------------------------------------------------------------------------------------------------------------\n";

 if (lc (askQuestion("Do you want to continue? (Y/N):",'N','AN',"","",1)) eq 'n' ){
        print "\n";
        exit 0;
 }
 else{
	 #grab inputs
	 print ("\n");

	 $solaceAdapterCluster->{ $tokens->{SERVICE_DESCRIPTION} }      = askQuestion("Enter the name of the cluster:","",'AN',"","",128);
	 
	 $solaceAdapterCluster->{ $tokens->{HOST_NAME} }		= askQuestion("Enter the hostname of the Solace HA pair hosting this cluster:","",'AN',"","",128);


	 my $members                                                    = askQuestion("Enter the number of members in this cluster:","",'N',0,128,"");
	  
	 my $warn       						= askQuestion("Enter the number of members that should be in a non-OK state for the cluster to enter a WARNING state:","",'N',0,$members,"");

	 $solaceAdapterCluster->{ $tokens->{CLUSTER_WARNING_THRESHOLD} } = join("","@",$warn,":",$warn);

	 my $crit     							= askQuestion("Enter the number of members that should be in a non-OK state for the cluster to enter a CRITICAL state:","",'N',0,$members,"");

	 $solaceAdapterCluster->{ $tokens->{CLUSTER_CRITICAL_THRESHOLD} } = join("","@",$crit,":",$crit);
	 
	 my $serviceStateID = "\$SERVICESTATEID";

	 my @clusterMembers;
	 
	 my $host;
	 my $service;

	 for (my $i=1;$i<=$members;$i++){
		print "\n--Cluster member $i configurations--\n";
		
		$host 		= askQuestion("Enter the adapter's hostname (display name in Nagios):","",'AN',"","",128);

		$service	= askQuestion("Enter the adapter's service name (display name in Nagios):","",'AN',"","",128);

		my $memberState = join("",$serviceStateID,":",$host,":",$service,"\$");

		push(@clusterMembers,$memberState);

	 }

	 $solaceAdapterCluster->{ $tokens->{CLUSTER_MEMBERS} } = join(",",@clusterMembers);
		
		
	 #print Dumper($solaceAdapterCluster);

	 my $outfileName 		= join('/',$dirs->{OUTPUTS},$solaceAdapterCluster->{ $tokens->{SERVICE_DESCRIPTION} }.".cfg");

	 my $templateFile		=  $templates->{SOLACE_ADAPTER_CLUSTER} ;
	 
	 copyTemplate($templateFile,$outfileName);

	 my $key;
	 for $key ( keys %{$solaceAdapterCluster}) {
		substitute($key,$solaceAdapterCluster->{$key},$outfileName);
	 }

	 
	 print ("\nThe Object configuration file for the Solace router pair has been created at: $outfileName\n");

	print "\n----------------------------------------------------------------------------------------------------\n";
        print "NOTE:Ensure that the hostnames and service descriptions of the cluster members match with the \n";
	print "corresponding object configurations as this is required for the cluster checks to work properly \n";
        print "------------------------------------------------------------------------------------------------------\n\n";

	 }
}
