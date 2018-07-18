#!/bin/bash

clear

echo -----------------------------------
echo -en '\033[47;31m'"\033[1mChecking brew installation\033[0m" 
tput sgr0
echo
brewFile="/usr/local/bin/brew"
if [ ! -f $brewFile ]; then
	echo Homebrew not installed... installing
	/usr/bin/ruby -e "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/master/install)"
else
	echo Homebrew already installed
fi

echo -----------------------------------
echo -en '\033[47;31m'"\033[1mChecking python installation\033[0m" 
tput sgr0
echo
pythonFile="/usr/local/bin/python2.7"
if [ ! -f $pythonFile ]; then
	echo Python not installed... installing
	brew install python
else
	echo Python already installed
fi

echo -----------------------------------
echo -en '\033[47;31m'"\033[1mChecking numpy installation\033[0m" 
tput sgr0
echo
numpyName=`brew list | grep numpy`
if [ -z $numpyName ]; then
	echo Numpy not installed... installing
	brew tap homebrew/science
	brew install numpy
else
	echo Numpy already installed
fi



echo -----------------------------------
echo -en '\033[47;31m'"\033[1mChecking scipy installation\033[0m" 
tput sgr0
echo
scipyName=`brew list | grep scipy`
if [ -z $scipyName ]; then
	echo Scipy not installed... installing
	brew install scipy
else
	echo Scipy already installed
fi

echo -----------------------------------
echo -en '\033[47;31m'"\033[1mChecking wxPython installation\033[0m" 
tput sgr0
echo
wxName=`brew list | grep wxpython`
if [ -z $wxName ]; then
	echo wxPython not installed... installing
	brew install wxpython
else
	echo wxPython already installed
fi

echo -----------------------------------
echo -en '\033[47;31m'"\033[1mChecking matplotlib installation\033[0m" 
tput sgr0
echo
matplotlibName=`brew list | grep matplotlib`
if [ -z $matplotlibName ]; then
	echo Matplotlib not installed... installing
	brew install matplotlib
else
	echo Matplotlib already installed
fi

echo -----------------------------------
echo -en '\033[47;31m'"\033[1mChecking pandas installation\033[0m" 
tput sgr0
echo
pandasName=`/usr/local/bin/pip2.7 freeze | grep pandas`
if [ -z $pandasName ]; then
	echo Pandas not installed... installing
	/usr/local/bin/pip2.7 install pandas
else
	echo Pandas already installed
fi

echo -----------------------------------
echo -en '\033[47;31m'"\033[1mDownloading and installing GASATaD\033[0m" 
tput sgr0
echo
curl -sL https://github.com/milegroup/gasatad/raw/gh-pages/packages/GASATaD_0_9.app.tgz -o $TMPDIR/GASATaD_0_9.app.tgz
tar xfz $TMPDIR/GASATaD_0_9.app.tgz -C $TMPDIR
rm -rf /Applications/GASATaD.app
mv $TMPDIR/GASATaD.app /Applications
echo GASATaD has been added to the Applications folder
echo Installation is now complete
echo -----------------------------------