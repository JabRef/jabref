require 'bundler'
begin
  Bundler.setup
rescue Bundler::BundlerError => e
  $stderr.puts e.message
  $stderr.puts "Run `bundle install' to install missing gems"
  exit e.status_code
end

puts "Running tests from #{Dir.pwd}"

require 'rspec/core'
require 'rspec/core/rake_task'
RSpec::Core::RakeTask.new(:spec) do |spec|
  spec.rspec_opts = %w{ --require spec_helper.rb --format Fuubar --color --format json --out spec/sheldon/ci.json }
end

task :default => [:spec]
