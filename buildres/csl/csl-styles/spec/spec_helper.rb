require 'csl'
require 'json'
require 'yaml'

PROJECT_ROOT = File.expand_path('../..', __FILE__)
PULL_REQUEST = File.join(PROJECT_ROOT, 'pull-request')
STYLE_ROOT = File.directory?(PULL_REQUEST) ? PULL_REQUEST : PROJECT_ROOT

ISSN = Hash.new { |h,k| h[k] = [] }
TITLES = Hash.new { |h,k| h[k] = [] }
FILTER = YAML.load_file(File.join(File.dirname(__FILE__), 'filters.yaml'))

EXTRA_FILES = Dir[File.join(STYLE_ROOT, '**', '*')].reject do |file|
  basedir = file.sub(STYLE_ROOT + "/","").partition("/")[0]
  name = File.basename(file)
  File.extname(file) == '.csl' || FILTER['EXTRA_FILES'].any? { |f| f === name } || FILTER['EXTRA_FILES_DIRECTORY'].any? { |d| d === basedir}
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
          ISSN[issn.to_s] << basename unless FILTER['ISSN'].include?(issn.to_s)
        end
      end

      if style.info.has_eissn?
        [style.info.eissn].flatten(1).each do |issn|
          ISSN[issn.to_s] << basename unless FILTER['ISSN'].include?(issn.to_s)
        end
      end

      if style.has_title?
        title = style.title.to_s.downcase
        TITLES[title] << basename unless FILTER['TITLES'].include?(title)
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
  dependence = type == '' ? 'independent' : type
  glob = File.join(STYLE_ROOT, type, '*.csl')
  print "\nLoading #{dependence} styles matching #{STYLE_FILTER.source} in #{glob}"

  Dir[glob].select do |filename|
    filename =~ STYLE_FILTER
  end
end

Dependents = Hash[collect_styles('dependent').each_with_index.map { |path, i|
  print '.' if i % 120 == 0
  load_style(path)
}]

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
