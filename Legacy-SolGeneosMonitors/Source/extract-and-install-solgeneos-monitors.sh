#!/bin/bash
##########################################################################
# Run this script to update your custom solgeneos monitors.
# Just place your solgeneos-monitors_*.tar.gz in one of the directories
# listed under: source_dirs and run the script.
# The script will:
#  - extract your installation archive in its place
#  - run the installation script
# 
##########################################################################
# append or change to your custom install directory
declare -a source_dirs=("/usr/sw/jail/geneos-tmp" "/usr/sw/jail/tmp")
# change to true, if your script needs to extract the tar.gz file first
extract_before_install="true"
##########################################################################
# Functions

function check_rc {
  rc=$?
  if [[ $rc != 0 ]]; then 
    echo "$1 returned non zero rc! Rc was: $rc"
    read -p "Do you want to continue? " -n 1 -r
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
      exit $rc
    fi
  fi
}

function extract {
  install_done="false"
  files_copied="false"
  for base_dir in ${source_dirs[@]}; do
    if [ "$install_done" = "false" ]; then
      echo "Checking ${base_dir} exists..."
      if [ -d "${base_dir}" ]; then
        echo "Updating solgeneos from ${base_dir}..."
        cd ${base_dir}
        if [ "$extract_before_install" = "true" ]; then
          if ls ${base_dir}/solgeneos-monitors_*.tar.gz 1>/dev/null 2>&1; then
            echo "Removing any old install files..."
            rm -rf ${base_dir}/solgeneos-monitors_*/
            echo "Extracting install to ${base_dir}..."
            tar -xzf solgeneos-monitors_*.tar.gz
            check_rc "tar -xzf solgeneos-monitors_*.tar.gz"
          else
            echo "No file matching solgeneos-monitors_*.tar found in ${base_dir}..."
          fi
       fi
       if ls ${base_dir}/solgeneos-monitors_v*/install-solgeneos-monitors.sh 1>/dev/null 2>&1; then
         cd ${base_dir}/solgeneos-monitors_v*
         chmod +x install-solgeneos-monitors.sh
         ./install-solgeneos-monitors.sh
         rc=$?
         if [[ $rc = 0 ]]; then
           install_done="true"
         fi
       else
           echo "No install files found in ${base_dir}/solgeneos-monitors_v*/..."
       fi
      else
        echo "${base_dir} does not exist, skipping..."
      fi
    fi
  done
  if [ "$install_done" = "true" ]; then
    echo "Install script successful!"
  else
    echo "No install files found!"
    echo "Install script failed!"
    exit 1
  fi
}

##########################################################################
# main
extract
exit 0

