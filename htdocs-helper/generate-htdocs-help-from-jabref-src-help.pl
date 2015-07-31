#!/usr/bin/perl
#requires: perl >= 5.10

#generate-htdocs-help-form-jabref-src-help.pl
#(c) 2012 Kolja Brix and Oliver Kopp

#This scripts converts the help files
#from the source directory of JabRef (HELPDIR_JABREF)
#to help files for the web page (HELPDIR_WEB)

#Start it from the root directory of your git repository.
#  Windows: perl generate-htdocs-help-form-jabref-src-help.pl
#It will overwrite all help files in HELPDIR_WEB
#It will NOT delete files in HELPDIR_WEB which were removed in HELPDIR_JABREF

#There are NO command line parameters

#If you have newline issues at the generated files,
#adapt FORCE_WINDOWS_NEWLINES


#Error:
#Use of uninitialized value in concatenation (.) or string at generate-htdocs-help-from-jabref-src-help.pl line 174, <$infileH> line 138.
#Reason:
#A new language has been added to HELPDIR_JABREF, where no translation is contained in
#%translation_back_to_contents. Please add the language to there.

use constant HELPDIR_JABREF => "../src/main/resources/help";
use constant HELPDIR_WEB    => "../../htdocs/help";

#0 for normal operationrequired
#1 for cygwin's perl
use constant FORCE_WINDOWS_NEWLINES => 0;

#translations for "Back to contents"
our %translation_back_to_contents = (
  "da" => "Back to contents",
  "de" => "Zur&uuml;ck zum Inhaltsverzeichnis",
  "en" => "Back to contents",
  "fr" => "Retour au contenu",
  "in" => "Kembali ke Daftar Isi",
  "ja" => "目次に戻る"
);


#build.xml for getting string replacements ${version} and ${year}
use constant BUILDXML       => "../build.xml";

use warnings;
use strict;

#enable "given/when"
use feature ":5.10";

sub handleDir;
sub handleFile;
sub loadPreferences;

our $jabref_version;
our $jabref_year;
our $jabref_placeholder_version;
our $jabref_placeholder_year;

loadPreferences();

#Debug call for a single file
#handleFile("../src/main/resources/help/About.html", "../../htdocs/help/About.php", "en");
#exit;


# handle English
handleDir(HELPDIR_JABREF, HELPDIR_WEB, "en");

#handle other languages (contained in sub directories)

my $helpdirJabRef;

opendir($helpdirJabRef, HELPDIR_JABREF) or die $!;

my $sourcedir;
my $targetdir;
my $lang;

while (my $subdir = readdir($helpdirJabRef)) {
	$sourcedir = HELPDIR_JABREF . "/$subdir";
	next unless (-d $sourcedir);
	next if ($subdir =~ /\.\.?/);

	$targetdir = HELPDIR_WEB . "/$subdir";
	$lang = $subdir;

	handleDir($sourcedir, $targetdir, $lang);
}
close($helpdirJabRef);

exit 0;



# Parameters:
#    sourcedir
#    targetdir
#    language
sub handleDir {
	my $sourcedir = shift;
	my $targetdir = shift;
	my $lang = shift;

	print("Handling $sourcedir...\n");

	if (!-d $targetdir) {
		mkdir($targetdir);
	}

	my $dh;
	opendir($dh, $sourcedir) or die $!;
	while (my $infilename = readdir($dh)) {
		next unless ($infilename =~ /\.html$/);
		my $outfilename =  $infilename;
		$outfilename =~ s/\.html/\.php/g;
		my $sourcefilename = $sourcedir . "/" . $infilename;
		my $targetfilename = $targetdir . "/" . $outfilename;
		handleFile($sourcefilename, $targetfilename, $lang);
	}
	close($dh);
}

#
# Parameters:
#    infilename: source file (html)
#    outfile:    target file (php)
#    lang:       language (ISO-format)
#
sub handleFile {
  my $infilename  =  shift;
  my $outfilename = shift;
  my $lang = shift;

  my $replace_placeholders = ($infilename =~ /About.html$/);

  #Debug output
  #print("handleFile:\n$infilename\n$outfilename\n$lang\n$replace_placeholders\n\n");

  open(my $infileH, "<", $infilename) or die "cannot open < $infilename: $!";
  my @infile = <$infileH>;

  my @outfile=();

  # Determine title out of first h1 heading
  my $title="";
  my $line;
  foreach $line(@infile) {
    if ($line =~ /\<h1\>(.*)\<\/h1\>/) {
      $title=$1;
	  if ($replace_placeholders) {
	    $title =~ s/$jabref_placeholder_version/$jabref_version/;
	    $title =~ s/$jabref_placeholder_year/$jabref_year/;
	  }
	  # title is found, go to the normal handling
	  last;
    }
  }

  #remove html tags from title
  #even if <em> is not allowed in h1 elements, JabRef doc uses that
  $title =~ s#<(.|\n)*?>##g;

#Following prefix does not work at sourceforge.
#<?xml version=\"1.0\" encoding=\"UTF-8\"?>
#We use php's header statement instead

  #add to the relative path to navigation|footer if help is non-english
  my $pathaddition;
  if ($lang eq 'en') {
    $pathaddition = "";
  } else {
    $pathaddition = "../";
  }

  my $navigationlink = $pathaddition . "../navigation.php";
  my $footerlink = $pathaddition . "../footer.php";

  my $header=<<HTML;
<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\"
   \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">
<html xmlns=\"http://www.w3.org/1999/xhtml\" lang=\"$lang\" xml:lang=\"$lang\">
<?php
  header('Content-type: application/xhtml+xml; charset=utf-8');

  // DO NOT EDIT BY HAND
  // This file is generated from jabref/src/help.
  // Run generate-htdocs-help-from-jabref-src-help.pl in the root directory
  // of the JabRef repository to regenerate the htdocs out of JabRef's help.
?>
<head>
  <meta http-equiv=\"content-type\" content=\"application/xhtml+xml; charset=UTF-8\" />
  <title>$title</title>
  <link href=\"/css/style.css\" rel=\"stylesheet\" type=\"text/css\" />
</head>

<body>
  <div id=\"container\">
    <?php include(\"$navigationlink\"); ?>
    <a href=\"Contents.php\">$translation_back_to_contents{$lang}</a>

HTML

  my $footer=<<HTML;
  <?php include(\"$footerlink\"); ?>
  </div>\n\n</body>\n</html>
HTML

  push(@outfile, $header);

  my $status=0;
  # 0 out of html
  # 1 in html
  # 2 within basefont

  foreach $line(@infile) {
    #Debug states
	#print "$status / $line";

    if ($status==0 && $line =~ /\<body/) {
      $status=1;
    } elsif ($status==1 && $line =~ /\<\/body\>/) {
      $status=0;
    } elsif ($status==1) {
      #we may not transfer a "basefont"
	  if ($line =~ /\<basefont/) {
	    if ($line !~ /\/\>/) {
		  $status = 2;
		}
	  } else {
	    if ($replace_placeholders) {
	      $line =~ s/$jabref_placeholder_version/$jabref_version/;
	      $line =~ s/$jabref_placeholder_year/$jabref_year/;
	    }
	    if (!($line =~ /href=\"http:\/\//)) {
		  #line does NOT contain a href to some http address
		  #we assume that line is NOT a reference to an external site
		  #replace "html" extension with "php" extension
		  #still allow links as "...html#part".
		  $line =~  s/href=\"([^\"]*)\.html/href=\"$1\.php/g;
	    }
        push(@outfile, $line);
	  }
	} elsif (($status==2) && ($line =~ /\/\>/)) {
		#basefont ended, reset to "inhtml"
		$status = 1;
	}
  }

  push(@outfile, $footer);

  open(OUTFILE,">$outfilename");

  if (FORCE_WINDOWS_NEWLINES) {
	foreach my $line (@outfile) {
		$line =~ s/\r?\n|\r/\r\n/g;
	}
  }

  print OUTFILE @outfile;

  close(OUTFILE);

  close($infileH);
}

#extracts info out of build.xml
#	<property name="jabref.version" value="2.8b" />
#	<property name="jabref.year" value="2012" />
#	<property name="jabref.placeholder.version" value="${version}" />
#	<property name="jabref.placeholder.year" value="${year}" />
sub loadPreferences {
  open(my $buildXML, "<", BUILDXML) or die "cannot open < " . BUILDXML . ": $!";
  my @buildxml = <$buildXML>;
  close($buildXML);
  foreach my $line (@buildxml) {
    #check for one-line property declaration name / value
    if ($line =~ /property name="([^"]*)" value="([^"]*)"/) {
	  #copy value from value to local variable
	  #a non-hardcoded version using "eval" would also be possible
	  #the SLOC count would be equal to the count of the following (easier) given/when construct.
	  given($1) {
		when ("jabref.version") {
		  $jabref_version = $2;
		}
	    when ("jabref.year") {
		  $jabref_year = $2;
		}
		when ("jabref.placeholder.version") {
		  $jabref_placeholder_version = $2;
		}
		when ("jabref.placeholder.year") {
		  $jabref_placeholder_year = $2;
		}
      }
    }
  }
}