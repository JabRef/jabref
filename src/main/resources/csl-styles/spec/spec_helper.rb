require 'csl'
require 'json'

STYLE_ROOT = File.expand_path('../..', __FILE__)

ISSN = Hash.new { |h,k| h[k] = [] }
TITLES = Hash.new { |h,k| h[k] = [] }

# These ISSNs are ignored when checking for duplicate ISSNs
ISSN_FILTER = %w{
  1662-453X 1663-9812 1664-042X 1664-0640 1664-1078 1664-2295
  1664-2392 1664-302X 1664-3224 1664-462X 1664-8021 2234-943X
  0036-8075 1095-9203 1359-4184 1476-5578 1097-6256 1047-7594
  1546-1726 2108-6419 0035-2969 1958-5691 0943-8610 2194-508X
  0223-5099 0322-8916 1805-6555 1899-0665 0305-1048 1362-4962
  0042-7306 1783-1830 1438-5627 0353-6483 1855-8399
}

# These titles are ignored when checking for duplicate titles
TITLES_FILTER = [
  # 'example title 1',
  # 'example title 2'
]

# These styles are ignored when checking for valid citation-formats
CITATION_FORMAT_FILTER = %w{
  bibtex blank national-archives-of-australia
}

# These styles are ignored when checking for unused macros
UNUSED_MACROS_FILTER = %w{
  chicago-annotated-bibliography chicago-author-date chicago-author-date-16th-edition
  chicago-library-list chicago-note-bibliography-16th-edition
  chicago-note-bibliography-with-ibid
  chicago-note-bibliography taylor-and-francis-chicago-author-date
}

# These files and directories are ignored when checking for extra files
EXTRA_FILES_FILTER = [
  'CONTRIBUTING.md', 'Gemfile', 'Gemfile.lock', 'README.md',
  'dependent', 'Rakefile', 'renamed-styles.json'
]

# These directories and their contents are ignored when checking for extra files
EXTRA_FILES_DIRECTORY_FILTER = [
  'spec', 'vendor'
]

EXTRA_FILES = Dir[File.join(STYLE_ROOT, '**', '*')].reject do |file|
  basedir = file.sub(STYLE_ROOT + "/","").partition("/")[0]
  name = File.basename(file)
  File.extname(file) == '.csl' || EXTRA_FILES_FILTER.any? { |f| f === name } || EXTRA_FILES_DIRECTORY_FILTER.any? { |d| d === basedir}
end

# License URL and text
CSL_LICENSE_URL = 'http://creativecommons.org/licenses/by-sa/3.0/'
CSL_LICENSE_TEXT =
  'This work is licensed under a Creative Commons Attribution-ShareAlike 3.0 License'

def load_style(path)
  filename = File.basename(path)
  basename = filename[0..-5]

  begin
    style = CSL::Style.load(path)
  rescue => error
    return [basename, [filename, path, nil, error]]
  end

  unless style.nil?
    begin
      if style.info.has_issn?
        [style.info.issn].flatten(1).each do |issn|
          ISSN[issn.to_s] << basename unless ISSN_FILTER.include?(issn.to_s)
        end
      end

      if style.info.has_eissn?
        [style.info.eissn].flatten(1).each do |issn|
          ISSN[issn.to_s] << basename unless ISSN_FILTER.include?(issn.to_s)
        end
      end

      if style.has_title?
        title = style.title.to_s.downcase
        TITLES[title] << basename unless TITLES_FILTER.include?(title)
      end
    rescue
      warn "Failed to extract ISSN of style #{basename}"
    end
  end

  [basename, [filename, path, style]]
end


# Collect styles to include in this test run.
#
# By default, all *.csl files will be included. If the environment
# variable CSL_TEST is set, only the style files matching the content
# of the variable will be tested; this can be a space separated list
# of files including regular expressions; if CSL_TEST is set to the
# special value `git' only those styles which have been modified since
# the last commit will be tested.
#
# Note that this requires the `git` executable to be available. Also
# note that this will not catch new files which have not yet been
# committed, but only modified files.
#
# Examples:
#
# $ CSL_TEST=git bundle exec rspec spec --format doc
# -> only includes changed styles and using documentation format
#
# $ CSL_TEST="apa.csl vancouver.csl" bundle exec rspec spec
# -> only run test for apa.csl and vancouver.csl
#
# $ CSL_TEST="chicago.*" bundle exec rspec spec
# -> run tests for all styles starting with 'chicago'

STYLE_FILTER = case ENV['CSL_TEST']
  when nil
    /./
  when 'git'
    Regexp.new("/(#{`git diff --name-only`.split(/\s+/).join('|')})$")
  else
    Regexp.new("/(#{ENV['CSL_TEST'].split(/\s+/).join('|')})$")
  end

def collect_styles(type = '')
  Dir[File.join(STYLE_ROOT, type, '*.csl')].select do |filename|
    filename =~ STYLE_FILTER
  end
end

print "\nLoading dependent styles"

Dependents = Hash[collect_styles('dependent').each_with_index.map { |path, i|
  print '.' if i % 120 == 0
  load_style(path)
}]

print "\nLoading independent styles"

Independents = Hash[collect_styles.each_with_index.map { |path, i|
  print '.'  if i % 120 == 0
  load_style(path)
}]

# Make sure we always have the basenames of all independent styles stored
if ENV['CSL_TEST'] != nil
  INDEPENDENTS_BASENAMES = Dir[File.join(STYLE_ROOT, '*.csl')].map { |path|
    File.basename(path, '.csl')
  }
else
  INDEPENDENTS_BASENAMES = Independents.keys
end

# Store basenames of dependent styles
DEPENDENTS_BASENAMES = Dir[File.join(STYLE_ROOT, 'dependent', '*.csl')].map { |path|
  File.basename(path, '.csl')
}

# Make sure the parents of selected dependents are loaded
# (necessary for citation-format comparison)
if ENV['CSL_TEST'] != nil
  parent_basenames = []

  Dependents.each_pair do |basename, (filename, path, style)|
    if style.has_independent_parent_link?
      parent_basename = style.independent_parent_link[/[^\/]+$/]
      if !parent_basenames.include?(parent_basename)
        parent_basenames << parent_basename
      end
    end
  end

  # eliminate parents that already have been loaded
  parent_basenames.reject! do |basename|
    Independents.has_key?(basename)
  end

  # load extra parents
  extra_independents = Hash[parent_basenames.each_with_index.map { |basename, i|
    print '.'  if i % 120 == 0

    # convert basename to path
    path = File.join(STYLE_ROOT, basename + '.csl')
    load_style(path)
  }]

  # combine hashes
  Independents.merge!(extra_independents)
end

puts
