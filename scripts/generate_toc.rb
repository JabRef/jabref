#!/usr/bin/env ruby

# Source: https://gist.github.com/albertodebortoli/9310424
# Via: http://stackoverflow.com/a/22131019/873282

File.open("Code-Howtos.md", 'r') do |f|
  f.each_line do |line|
    forbidden_words = ['Table of contents', 'define', 'pragma']
    next if !line.start_with?("#") || forbidden_words.any? { |w| line =~ /#{w}/ }

    title = line.gsub("#", "").strip
    href = title.gsub(" ", "-").gsub('"',"").downcase
    puts "  " * (line.count("#")-1) + "* [#{title}](\##{href})"
  end
end
