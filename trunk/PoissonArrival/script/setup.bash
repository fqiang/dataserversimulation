sudo mkdir /home/cambridge_fq208/setup
sudo wget -P /home/cambridge_fq208/setup/ http://www.cl.cam.ac.uk/~fq208/project/java.tar.gz
sudo tar zxvf /home/cambridge_fq208/setup/java.tar.gz -C /
sudo yum -y install vim
sudo yum -y install subversion
sudo mkdir /usr/local/ant
sudo wget -P /usr/local/ant/ http://www.cl.cam.ac.uk/~fq208/project/apache-ant-1.8.0-bin.tar.gz
sudo tar zxvf /usr/local/ant/apache-ant-1.8.0-bin.tar.gz -C /usr/local/ant/
sudo ln -s /usr/local/ant/apache-ant-1.8.0 /usr/local/ant/default
sudo wget -P /home/cambridge_fq208/setup/ http://www.cl.cam.ac.uk/~fq208/project/bash_profile
cp -f /home/cambridge_fq208/setup/bash_profile /home/cambridge_fq208/.bash_profile
source /home/cambridge_fq208/.bash_profile
sudo mv /home/cambridge_fq208/setup.bash /home/cambridge_fq208/setup.bash.old
sudo rm -rf /home/cambridge_fq208/setup.old
sudo mv /home/cambridge_fq208/setup /home/cambridge_fq208/setup.old
mkdir /home/cambridge_fq208/acs_project
mkdir /home/cambridge_fq208/.ssh
