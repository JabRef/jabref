#!/bin/bash
# no need for databases for the integrationTest -> save memory overflow
# currently does not work: "stop: Unknown instance:" - sudo service mysql stop
sudo service postgresql stop
# following services identified by "sudo service --status-all" do not need to run, too
# excluded: rsyslog (feels wrong), udev (feels wrong), friendly-recovery ("Unknown instance" error)
sudo service acpid stop
sudo service atd stop
sudo service cron stop
sudo service memcached stop
sudo service ntp stop
sudo service rabbitmq-server stop
sudo service resolvconf stop
sudo service sshguard stop
sudo service ssh stop
# Integration tests run in a timeout. Just start them and kill them after 60s.
timeout 60 ./gradlew guiTest -Dscan --info || true
