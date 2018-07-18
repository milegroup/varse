echo
echo "Uninstalling brew..."
echo "--------------------"
ruby -e "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/master/uninstall)"

echo
echo "Removing GASATaD from the system"
echo "--------------------------------"
rm -rf /Applications/GASATaD.app