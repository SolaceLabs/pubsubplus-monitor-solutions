#!/usr/bin/perl

package Configs;
use strict;
use warnings;

use Exporter;
use FindBin;
use Cwd qw(realpath);

our @ISA = 'Exporter';
our @EXPORT = qw($templates
                 $dirs
		 $tokens
                );

my $baseDir=realpath("$FindBin::Bin");

our $dirs	=       {
			  TEMPLATES		=> join ('/',$baseDir,"templates"),
                	  OUTPUTS    		=> join ('/',$baseDir,"outputs")
                	};

our $templates	=	{
			  SOLACE_ROUTER			=> join ('/', $dirs->{TEMPLATES}, "solaceRouter.cfg"),
			  SOLACE_VPN			=> join ('/', $dirs->{TEMPLATES}, "solaceVpn.cfg"),
			  SOLACE_ADAPTER		=> join ('/', $dirs->{TEMPLATES}, "solaceAdapter.cfg"),
			  SOLACE_ROUTER_PAIR		=> join ('/', $dirs->{TEMPLATES}, "solaceRouterPair.cfg"),
			  SOLACE_ADAPTER_CLUSTER	=> join ('/', $dirs->{TEMPLATES}, "solaceAdapterCluster.cfg") 
			};	

our $tokens	=	{
			  HOST_NAME			=> "<HOST_NAME_TOKEN>",
			  HOST_ALIAS			=> "<HOST_ALIAS_TOKEN>",
			  MGMT_ADDRESS			=> "<MGMT_ADDRESS_TOKEN>",
			  ADDRESS_PORT			=> "<ADDRESS_PORT_TOKEN>",
			  PORT_NUMBER			=> "<PORT_NUMBER_TOKEN>",
			  INTERFACE_MGMT		=> "<INTERFACE_MGMT_TOKEN>",
			  INTERFACE_MSGB		=> "<INTERFACE_MSGB_TOKEN>",
			  PARTITION_FS			=> "<PARTITION_FS_TOKEN>",
			  HOSTGROUP_NAME		=> "<HOSTGROUP_NAME_TOKEN>",
			  HOSTGROUP_ALIAS		=> "<HOSTGROUP_ALIAS_TOKEN>",
			  HOSTGROUP_MEMBERS		=> "<HOSTGROUP_MEMBERS_TOKEN>",
			  HOSTDEP_NAME			=> "<HOSTDEP_NAME_TOKEN>",

		          PARENT			=> "<PARENT_TOKEN>",
			  ROUTER_NAME			=> "<ROUTER_NAME_TOKEN>",
			  MSGB_ADDRESS			=> "<MSGB_ADDRESS_TOKEN>",
			  SERVICE_XML			=> "<SERVICE_XML_TOKEN>",
			  CONFIG_FILE			=> "<CONFIG_FILE_TOKEN>",
			  SERVICE_FILTER		=> "<SERVICE_FILTER_TOKEN>",
			  QUEUE				=> "<QUEUE_NAME_TOKEN>",
			  BRIDGE			=> "<BRIDGE_NAME_TOKEN>",
			
			  VPN_NAME			=> "<VPN_NAME_TOKEN>",
			  CLIENT_STATS_TOPIC		=> "<CLIENT_STATS_TOPIC_TOKEN>",
			  SERVICE_DESCRIPTION   	=> "<SERVICE_DESCRIPTION_TOKEN>",
			  CLUSTER_WARNING_THRESHOLD	=> "<CLUSTER_WARNING_THRESHOLD_TOKEN>",
			  CLUSTER_CRITICAL_THRESHOLD	=> "<CLUSTER_CRITICAL_THRESHOLD_TOKEN>",
			  CLUSTER_MEMBERS		=> "<CLUSTER_MEMBERS_TOKEN>",
			  ADAPTER_TYPE			=> "<ADAPTER_TYPE_TOKEN>",
			  ADAPTER_CHECK_COMMAND		=> "<ADAPTER_CHECK_COMMAND_TOKEN>"
			};
