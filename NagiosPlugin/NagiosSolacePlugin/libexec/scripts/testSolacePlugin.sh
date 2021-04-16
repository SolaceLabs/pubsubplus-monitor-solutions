#!/bin/sh

#Enter the paths to directories to be checked, separated by space
DIRS_TO_CHECK="/usr/local/nagios/libexec/solace /usr/local/nagios/libexec/scripts /usr/local/nagios/libexec/scripts/createNagiosObjectConfigs"

echo

for DIR  in $DIRS_TO_CHECK ; do 
	echo "Checking $DIR.."
	echo 

	FLAG=1

	for i in "$DIR"/*.pl ; do

  		if [[ -n $(perl -c $i |& grep "syntax OK" ) ]]; then

			echo -e " $i  ... OK"
    		else
    			echo -e " $i  ... has compilation errors!!!" 
			$FLAG=0
  		fi
	done

	if [[ $FLAG -eq 0 ]];then
		echo 
		echo "One or more Perl scripts in $DIR do not compile."
	else
		echo
		echo "All Perl scripts in $DIR compile successfully"
	fi
	echo "=============="
done


