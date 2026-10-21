#!/bin/bash
# EKM Local Build Guard
echo "====================================================================="
echo " [!] WARNING: Local container build blocked!"
echo " Android Gradle builds in this container will trigger OOM-Kills (Error 137)."
echo " Push to GitHub to trigger the automated CI/CD pipeline:"
echo " .github/workflows/build_apk.yml"
echo "====================================================================="
exit 1
