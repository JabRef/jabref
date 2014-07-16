#!/usr/bin/perl

#validate-htdocs-syntax.pl
#(c) 2012 Oliver Kopp

#This scripts validates the syntax of all files in the htdocs directory
#using tidy

#This script requires "tidy".
#It was tested using cygwin's perl and cygwin's tidy.

#Start it from the root directory of your git repository.
#  Windows: perl validate-htdocs-syntax.pl

#There are NO command line parameters

#configuration: should there be a prompt after each error?
use constant WAITAFTEREACHERROR => 1;


#configuration: directory to check

#online web site
use constant STARTDIR => "../../htdocs";

#single help
#use constant STARTDIR => "../../htdocs/help/ja";

#JabRef help
#  never validates as no HTML head is used and no DOCTYPE is declared.
#use constant STARTDIR => "../src/main/resources/help/";


use File::Find;
use strict;

sub wait_for_keypress {
    return unless WAITAFTEREACHERROR;
    print "Press 'Return' to continue. (Enter \"exit\" to exit the whole process)\n";
    my $input = <STDIN>;
	exit 0 if $input =~ /exit/;
}

sub verifyFile {
	return unless -f;
	#my $fullfilename = $File::Find::name;
	my $filename = $_;
	return unless ($filename =~ /(\.php)|(\.html)$/);

	#Debug output
	#print "Checking $File::Find::name\n";

	system("tidy", "-eq", "-utf8", "$filename");

	if ($? == -1) {
		print ("Failed to execute tidy.");
	} elsif ($? & 127) {
		printf "child died with signal %d, %s coredump\n",
		($? & 127), ($? & 128) ? 'with' : 'without';
	} elsif ($? != 0) {
		#some error occured

		# html/php line offset is 11. I.e., if tidy outputs "276", the line in the .html is "265"
		print "Above file was $File::Find::name and has errors\n\n";
		wait_for_keypress;
	}
}

#Debug call
#find(\&verifyFile, ("htdocs/contact.php"));

find(\&verifyFile, (STARTDIR));
