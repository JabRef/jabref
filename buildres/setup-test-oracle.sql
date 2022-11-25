create pluggable database jabref admin user jabref identified by jabref
  file_name_convert=('/opt/oracle/oradata/XE/pdbseed','/opt/oracle/oradata/XE/JABREF');
alter pluggable database jabref open read write;
alter pluggable database all save state;
ALTER SESSION SET CONTAINER = jabref;
grant all privileges to jabref container=current;
